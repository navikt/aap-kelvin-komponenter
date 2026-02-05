import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate


class TidslinjeSerdeTest {
    @Test
    fun roundtrip() {
        val examples = listOf<Tidslinje<String>>(
            Tidslinje(),
            tidslinjeOf(
                Periode(LocalDate.parse("2020-01-01"), LocalDate.parse("2021-01-01")) to "hey"
            ),
            tidslinjeOf(
                Periode(LocalDate.parse("2020-01-01"), LocalDate.parse("2021-01-01")) to "hey",
                Periode(LocalDate.parse("2021-03-01"), LocalDate.parse("2022-01-01")) to "ho"
            )
        )

        for (example in examples) {
            val json = DefaultJsonMapper.toJson(example)
            val result = DefaultJsonMapper.fromJson<Tidslinje<String>>(json)
            assertThat(result).isEqualTo(example)
            println(json)
        }
    }

    @Test
    fun serialize() {
        val obj = tidslinjeOf(
            Periode(LocalDate.parse("2020-01-01"), LocalDate.parse("2021-01-01")) to "hey",
            Periode(LocalDate.parse("2021-03-01"), LocalDate.parse("2022-01-01")) to "ho"
        )
        val json = DefaultJsonMapper.toJson(obj)
        val jsonObj = DefaultJsonMapper.objectMapper().readTree(json)
        assertThat(jsonObj.isArray).isTrue()

        jsonObj[0].also { førsteSegment ->
            assertThat(førsteSegment.fieldNames().asSequence().toSet())
                .isEqualTo(setOf("periode", "verdi"))
            assertThat(førsteSegment["periode"].fieldNames().asSequence().toSet())
                .isEqualTo(setOf("fom", "tom"))
            assertThat(førsteSegment["periode"]["fom"].asText()).isEqualTo("2020-01-01")
            assertThat(førsteSegment["periode"]["tom"].asText()).isEqualTo("2021-01-01")
            assertThat(førsteSegment["verdi"].asText()).isEqualTo("hey")
        }

        jsonObj[1].also { andreSegment ->
            assertThat(andreSegment.fieldNames().asSequence().toSet())
                .isEqualTo(setOf("periode", "verdi"))
            assertThat(andreSegment["periode"].fieldNames().asSequence().toSet())
                .isEqualTo(setOf("fom", "tom"))
            assertThat(andreSegment["periode"]["fom"].asText()).isEqualTo("2021-03-01")
            assertThat(andreSegment["periode"]["tom"].asText()).isEqualTo("2022-01-01")
            assertThat(andreSegment["verdi"].asText()).isEqualTo("ho")
        }
    }

    @Test
    fun deserialize() {
        val json = """
            [ {
              "periode": {
                  "fom" : "2020-01-01",
                  "tom" : "2021-01-01"
              },
              "verdi" : "hey"
            }, {
              "periode": {
                  "fom" : "2021-03-01",
                  "tom" : "2022-01-01"
              },
              "verdi" : "ho"
            } ]
        """.trimIndent()
        val obj = DefaultJsonMapper.fromJson<Tidslinje<String>>(json)
        assertThat(obj).isEqualTo(
            tidslinjeOf(
                Periode(LocalDate.parse("2020-01-01"), LocalDate.parse("2021-01-01")) to "hey",
                Periode(LocalDate.parse("2021-03-01"), LocalDate.parse("2022-01-01")) to "ho"
            )
        )
    }
}