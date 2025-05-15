package no.nav.aap.motor.cron

import org.intellij.lang.annotations.Language
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.max

public class CronExpression private constructor(private val expr: String, withSeconds: Boolean = true) {
    internal enum class CronFieldType(val from: Int, val to: Int, val names: List<String>?) {
        SECOND(0, 59, null) {
            override fun getValue(dateTime: ZonedDateTime): Int {
                return dateTime.second
            }

            override fun setValue(dateTime: ZonedDateTime, value: Int): ZonedDateTime {
                return dateTime.withSecond(value).withNano(0)
            }

            override fun overflow(dateTime: ZonedDateTime): ZonedDateTime {
                return dateTime.plusMinutes(1).withSecond(0).withNano(0)
            }
        },
        MINUTE(0, 59, null) {
            override fun getValue(dateTime: ZonedDateTime): Int {
                return dateTime.minute
            }

            override fun setValue(dateTime: ZonedDateTime, value: Int): ZonedDateTime {
                return dateTime.withMinute(value).withSecond(0).withNano(0)
            }

            override fun overflow(dateTime: ZonedDateTime): ZonedDateTime {
                return dateTime.plusHours(1).withMinute(0).withSecond(0).withNano(0)
            }
        },
        HOUR(0, 23, null) {
            override fun getValue(dateTime: ZonedDateTime): Int {
                return dateTime.hour
            }

            override fun setValue(dateTime: ZonedDateTime, value: Int): ZonedDateTime {
                return dateTime.withHour(value).withMinute(0).withSecond(0).withNano(0)
            }

            override fun overflow(dateTime: ZonedDateTime): ZonedDateTime {
                return dateTime.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
            }
        },
        DAY_OF_MONTH(1, 31, null) {
            override fun getValue(dateTime: ZonedDateTime): Int {
                return dateTime.dayOfMonth
            }

            override fun setValue(dateTime: ZonedDateTime, value: Int): ZonedDateTime {
                return dateTime.withDayOfMonth(value).withHour(0).withMinute(0).withSecond(0).withNano(0)
            }

            override fun overflow(dateTime: ZonedDateTime): ZonedDateTime {
                return dateTime.plusMonths(1).withDayOfMonth(0).withHour(0).withMinute(0).withSecond(0).withNano(0)
            }
        },
        MONTH(
            1, 12,
            mutableListOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
        ) {
            override fun getValue(dateTime: ZonedDateTime): Int {
                return dateTime.monthValue
            }

            override fun setValue(dateTime: ZonedDateTime, value: Int): ZonedDateTime {
                return dateTime.withMonth(value).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
            }

            override fun overflow(dateTime: ZonedDateTime): ZonedDateTime {
                return dateTime.plusYears(1).withMonth(1).withHour(0).withDayOfMonth(1).withMinute(0).withSecond(0)
                    .withNano(0)
            }
        },
        DAY_OF_WEEK(1, 7, mutableListOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")) {
            override fun getValue(dateTime: ZonedDateTime): Int {
                return dateTime.dayOfWeek.value
            }

            override fun setValue(dateTime: ZonedDateTime, value: Int): ZonedDateTime {
                throw UnsupportedOperationException()
            }

            override fun overflow(dateTime: ZonedDateTime): ZonedDateTime {
                throw UnsupportedOperationException()
            }
        };

        /**
         * @param dateTime [ZonedDateTime] instance
         * @return The field time or date value from `dateTime`
         */
        abstract fun getValue(dateTime: ZonedDateTime): Int

        /**
         * @param dateTime Initial [ZonedDateTime] instance to use
         * @param value    to set for this field in `dateTime`
         * @return [ZonedDateTime] with `value` set for this field and all smaller fields cleared
         */
        abstract fun setValue(dateTime: ZonedDateTime, value: Int): ZonedDateTime

        /**
         * Handle when this field overflows and the next higher field should be incremented
         *
         * @param dateTime Initial [ZonedDateTime] instance to use
         * @return [ZonedDateTime] with the next greater field incremented and all smaller fields cleared
         */
        abstract fun overflow(dateTime: ZonedDateTime): ZonedDateTime
    }

    private val secondField: SimpleField
    private val minuteField: SimpleField
    private val hourField: SimpleField
    private val dayOfWeekField: DayOfWeekField
    private val monthField: SimpleField
    private val dayOfMonthField: DayOfMonthField

    init {
        val expectedParts = if (withSeconds) 6 else 5
        val parts = expr.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray() //$NON-NLS-1$
        if (parts.size != expectedParts) {
            throw IllegalArgumentException(
                String.format(
                    "Invalid cron expression [%s], expected %s field, got %s",
                    expr,
                    expectedParts,
                    parts.size
                )
            )
        }

        var ix = if (withSeconds) 1 else 0
        this.secondField = SimpleField(CronFieldType.SECOND, if (withSeconds) parts[0] else "0")
        this.minuteField = SimpleField(CronFieldType.MINUTE, parts[ix++])
        this.hourField = SimpleField(CronFieldType.HOUR, parts[ix++])
        this.dayOfMonthField = DayOfMonthField(parts[ix++])
        this.monthField = SimpleField(CronFieldType.MONTH, parts[ix++])
        this.dayOfWeekField = DayOfWeekField(parts[ix++])
    }

    internal fun nextLocalDateTimeAfter(dateTime: LocalDateTime): LocalDateTime {
        return nextTimeAfter(ZonedDateTime.of(dateTime, ZoneId.systemDefault())).toLocalDateTime()
    }

    internal fun nextTimeAfter(afterTime: ZonedDateTime, durationInMillis: Long): ZonedDateTime {
        return nextTimeAfter(afterTime, afterTime.plus(Duration.ofMillis(durationInMillis)))
    }

    @JvmOverloads
    internal fun nextTimeAfter(
        afterTime: ZonedDateTime,
        dateTimeBarrier: ZonedDateTime = afterTime.plusYears(4)
    ): ZonedDateTime {
        val nextDateTime = arrayOf(afterTime.plusSeconds(1).withNano(0))

        while (true) {
            checkIfDateTimeBarrierIsReached(nextDateTime[0], dateTimeBarrier)
            if (!monthField.nextMatch(nextDateTime)) {
                continue
            }
            if (!findDay(nextDateTime, dateTimeBarrier)) {
                continue
            }
            if (!hourField.nextMatch(nextDateTime)) {
                continue
            }
            if (!minuteField.nextMatch(nextDateTime)) {
                continue
            }
            if (!secondField.nextMatch(nextDateTime)) {
                continue
            }

            checkIfDateTimeBarrierIsReached(nextDateTime[0], dateTimeBarrier)
            return nextDateTime[0]
        }
    }

    /**
     * Find the next match for the day field.
     *
     *
     * This is handled different than all other fields because there are two ways to describe the day and it is easier
     * to handle them together in the same method.
     *
     * @param dateTime        Initial [ZonedDateTime] instance to start from
     * @param dateTimeBarrier At which point stop searching for next execution time
     * @return `true` if a match was found for this field or `false` if the field overflowed
     * @see {@link SimpleField.nextMatch
     */
    private fun findDay(dateTime: Array<ZonedDateTime>, dateTimeBarrier: ZonedDateTime): Boolean {
        val month = dateTime[0].monthValue

        while (!(dayOfMonthField.matches(dateTime[0].toLocalDate())
                    && dayOfWeekField.matches(dateTime[0].toLocalDate()))
        ) {
            dateTime[0] = dateTime[0].plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
            if (dateTime[0].monthValue != month) {
                return false
            }
        }
        return true
    }

    override fun toString(): String {
        return javaClass.simpleName + "<" + expr + ">"
    }

    internal class FieldPart : Comparable<FieldPart?> {
        var from: Int = -1
        var to: Int = -1
        var increment: Int = -1
        var modifier: String? = null
        var incrementModifier: String? = null

        override fun compareTo(other: FieldPart?): Int {
            return from.compareTo(other?.from ?: 0)
        }
    }

    internal abstract class BasicField internal constructor(val fieldType: CronFieldType, fieldExpr: String) {
        val parts: MutableList<FieldPart> = ArrayList()

        init {
            parse(fieldExpr)
        }

        private fun parse(fieldExpr: String) { // NOSONAR
            val rangeParts = fieldExpr.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            for (rangePart: String in rangeParts) {
                val m: Matcher = CRON_FIELD_REGEXP.matcher(rangePart)
                if (!m.matches()) {
                    throw IllegalArgumentException("Invalid cron field '$rangePart' for field [$fieldType]")
                }
                val startNummer = m.group("start")
                val modifier = m.group("mod")
                val sluttNummer = m.group("end")
                val incrementModifier = m.group("incmod")
                val increment = m.group("inc")

                val part = FieldPart()
                part.increment = 999
                if (startNummer != null) {
                    part.from = mapValue(startNummer)
                    part.modifier = modifier
                    if (sluttNummer != null) {
                        part.to = mapValue(sluttNummer)
                        part.increment = 1
                    } else if (increment != null) {
                        part.to = fieldType.to
                    } else {
                        part.to = part.from
                    }
                } else if (m.group("all") != null) {
                    part.from = fieldType.from
                    part.to = fieldType.to
                    part.increment = 1
                } else if (m.group("ignore") != null) {
                    part.modifier = m.group("ignore")
                } else if (m.group("last") != null) {
                    part.modifier = m.group("last")
                } else {
                    throw IllegalArgumentException("Invalid cron part: $rangePart")
                }

                if (increment != null) {
                    part.incrementModifier = incrementModifier
                    part.increment = increment.toInt()
                }

                validateRange(part)
                validatePart(part)
                parts.add(part)
            }

            Collections.sort(parts)
        }

        protected open fun validatePart(part: FieldPart) {
            if (part.modifier != null) {
                throw IllegalArgumentException(String.format("Invalid modifier [%s]", part.modifier))
            } else if (part.incrementModifier != null && "/" != part.incrementModifier) {
                throw IllegalArgumentException(String.format("Invalid increment modifier [%s]", part.incrementModifier))
            }
        }

        private fun validateRange(part: FieldPart) {
            if ((part.from != -1 && part.from < fieldType.from) || (part.to != -1 && part.to > fieldType.to)) {
                throw IllegalArgumentException(
                    String.format(
                        "Invalid interval [%s-%s], must be %s<=_<=%s", part.from, part.to, fieldType.from,
                        fieldType.to
                    )
                )
            } else if (part.from != -1 && part.to != -1 && part.from > part.to) {
                throw IllegalArgumentException(
                    String.format(
                        "Invalid interval [%s-%s].  Rolling periods are not supported (ex. 5-1, only 1-5) since this won't give a deterministic result. Must be %s<=_<=%s",
                        part.from, part.to, fieldType.from, fieldType.to
                    )
                )
            }
        }

        protected open fun mapValue(value: String): Int {
            var idx = 0
            if (fieldType.names != null && (fieldType.names.indexOf(value.uppercase(Locale.getDefault())).also {
                    idx = it
                }) >= 0) {
                return idx + fieldType.from
            }
            return value.toInt()
        }

        protected open fun matches(`val`: Int, part: FieldPart): Boolean {
            return ((`val` >= part.from) && `val` <= part.to) && (`val` - part.from) % part.increment == 0
        }

        protected fun nextMatch(`val`: Int, part: FieldPart): Int {
            if (`val` > part.to) {
                return -1
            }
            var nextPotential = max(`val`.toDouble(), part.from.toDouble()).toInt()
            if (part.increment == 1 || nextPotential == part.from) {
                return nextPotential
            }

            val remainder = ((nextPotential - part.from) % part.increment)
            if (remainder != 0) {
                nextPotential += part.increment - remainder
            }

            return if (nextPotential <= part.to) nextPotential else -1
        }

        companion object {
            private val CRON_FIELD_REGEXP: Pattern = Pattern
                .compile(
                    "(?:                                             # start of group 1\n"
                            + "   (?:(?<all>\\*)|(?<ignore>\\?)|(?<last>L))  # global flag (L, ?, *)\n"
                            + " | (?<start>[0-9]{1,2}|[a-z]{3,3})              # or start number or symbol\n"
                            + "      (?:                                        # start of group 2\n"
                            + "         (?<mod>L|W)                             # modifier (L,W)\n"
                            + "       | -(?<end>[0-9]{1,2}|[a-z]{3,3})        # or end nummer or symbol (in range)\n"
                            + "      )?                                         # end of group 2\n"
                            + ")                                              # end of group 1\n"
                            + "(?:(?<incmod>/|\\#)(?<inc>[0-9]{1,7}))?        # increment and increment modifier (/ or \\#)\n",
                    Pattern.CASE_INSENSITIVE or Pattern.COMMENTS
                )
        }
    }

    internal class SimpleField(fieldType: CronFieldType, fieldExpr: String) :
        BasicField(fieldType, fieldExpr) {
        fun matches(`val`: Int): Boolean {
            if (`val` >= fieldType.from && `val` <= fieldType.to) {
                for (part: FieldPart in parts) {
                    if (matches(`val`, part)) {
                        return true
                    }
                }
            }
            return false
        }

        /**
         * Find the next match for this field. If a match cannot be found force an overflow and increase the next
         * greatest field.
         *
         * @param dateTime [ZonedDateTime] array so the reference can be modified
         * @return `true` if a match was found for this field or `false` if the field overflowed
         */
        fun nextMatch(dateTime: Array<ZonedDateTime>): Boolean {
            val value: Int = fieldType.getValue(dateTime[0])

            for (part: FieldPart in parts) {
                val nextMatch: Int = nextMatch(value, part)
                if (nextMatch > -1) {
                    if (nextMatch != value) {
                        dateTime[0] = fieldType.setValue(dateTime[0], nextMatch)
                    }
                    return true
                }
            }

            dateTime[0] = fieldType.overflow(dateTime[0])
            return false
        }
    }

    internal class DayOfWeekField(fieldExpr: String) :
        BasicField(CronFieldType.DAY_OF_WEEK, fieldExpr) {
        fun matches(dato: LocalDate): Boolean {
            for (part: FieldPart in parts) {
                if (("L" == part.modifier)) {
                    val ym = YearMonth.of(dato.year, dato.month.value)
                    return dato.dayOfWeek == DayOfWeek.of(part.from) && dato.dayOfMonth > (ym.lengthOfMonth() - 7)
                } else if (("#" == part.incrementModifier)) {
                    if (dato.dayOfWeek == DayOfWeek.of(part.from)) {
                        val num = dato.dayOfMonth / 7
                        return part.increment == (if (dato.dayOfMonth % 7 == 0) num else num + 1)
                    }
                    return false
                } else if (matches(dato.dayOfWeek.value, part)) {
                    return true
                }
            }
            return false
        }

        override fun mapValue(value: String): Int {
            // Use 1-7 for weedays, but 0 will also represent sunday (linux practice)
            return if (("0" == value)) 7 else super.mapValue(value)
        }

        override fun matches(`val`: Int, part: FieldPart): Boolean {
            return ("?" == part.modifier) || super.matches(`val`, part)
        }

        override fun validatePart(part: FieldPart) {
            if (part.modifier != null && mutableListOf<String?>("L", "?").indexOf(part.modifier) == -1) {
                throw IllegalArgumentException(String.format("Invalid modifier [%s]", part.modifier))
            } else if (part.incrementModifier != null && mutableListOf<String?>(
                    "/",
                    "#"
                ).indexOf(part.incrementModifier) == -1
            ) {
                throw IllegalArgumentException(String.format("Invalid increment modifier [%s]", part.incrementModifier))
            }
        }
    }

    internal class DayOfMonthField(fieldExpr: String) :
        BasicField(CronFieldType.DAY_OF_MONTH, fieldExpr) {
        fun matches(dato: LocalDate): Boolean {
            for (part: FieldPart in parts) {
                if (("L" == part.modifier)) {
                    val ym = YearMonth.of(dato.year, dato.month.value)
                    return dato.dayOfMonth == (ym.lengthOfMonth() - (if (part.from == -1) 0 else part.from))
                } else if (("W" == part.modifier)) {
                    if (dato.dayOfWeek.value <= 5) {
                        if (dato.dayOfMonth == part.from) {
                            return true
                        } else if (dato.dayOfWeek.value == 5) {
                            return dato.plusDays(1).dayOfMonth == part.from
                        } else if (dato.dayOfWeek.value == 1) {
                            return dato.minusDays(1).dayOfMonth == part.from
                        }
                    }
                } else if (matches(dato.dayOfMonth, part)) {
                    return true
                }
            }
            return false
        }

        override fun validatePart(part: FieldPart) {
            if (part.modifier != null && mutableListOf<String?>("L", "W", "?").indexOf(part.modifier) == -1) {
                throw IllegalArgumentException(String.format("Invalid modifier [%s]", part.modifier))
            } else if (part.incrementModifier != null && "/" != part.incrementModifier) {
                throw IllegalArgumentException(String.format("Invalid increment modifier [%s]", part.incrementModifier))
            }
        }

        override fun matches(`val`: Int, part: FieldPart): Boolean {
            return ("?" == part.modifier) || super.matches(`val`, part)
        }
    }

    public companion object {
        public fun create(@Language("CronExp") expr: String): CronExpression {
            return CronExpression(expr, true)
        }

        public fun createWithoutSeconds(@Language("CronExp")  expr: String): CronExpression {
            return CronExpression(expr, false)
        }

        private fun checkIfDateTimeBarrierIsReached(nextTime: ZonedDateTime, dateTimeBarrier: ZonedDateTime) {
            if (nextTime.isAfter(dateTimeBarrier)) {
                throw IllegalArgumentException("No next execution time could be determined that is before the limit of $dateTimeBarrier")
            }
        }
    }
}