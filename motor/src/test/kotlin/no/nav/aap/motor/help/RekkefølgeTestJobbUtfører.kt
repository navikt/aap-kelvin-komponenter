package no.nav.aap.motor.help

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import org.slf4j.LoggerFactory

class RekkefølgeTestJobbUtfører(private val connection: DBConnection) : JobbUtfører {

    private val logger = LoggerFactory.getLogger(TullTestJobbUtfører::class.java)


    override fun utfør(input: JobbInput) {
        val threadName = Thread.currentThread().name
        connection.execute("INSERT INTO ORDER_TABLE (VALUE, TRAD_NAVN) VALUES (?, ?)") {
            setParams {
                setString(1, input.payload)
                setString(2, threadName)
            }
        };
        Thread.sleep(20)
        logger.info("Wrote ${input.payload} into table. Thread name: $threadName")
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return RekkefølgeTestJobbUtfører(connection)
        }

        override fun type(): String {
            return "TullRekkefolge"
        }

        override fun navn(): String {
            return "TullRekkefolge"
        }

        override fun beskrivelse(): String {
            return "TullRekkefolge"
        }
    }
}
