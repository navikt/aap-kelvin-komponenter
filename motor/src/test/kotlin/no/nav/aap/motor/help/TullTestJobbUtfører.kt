package no.nav.aap.motor.help

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import org.slf4j.LoggerFactory

class TullTestJobbUtfører(private val connection: DBConnection) : JobbUtfører {

    private val logger = LoggerFactory.getLogger(TullTestJobbUtfører::class.java)


    override fun utfør(input: JobbInput) {
        connection.execute("INSERT INTO TEST_TABLE (VALUE) VALUES (?)") {
            setParams {
                setString(1, input.payload)
            }
        };
        logger.info("Wrote ${input.payload} into table.")
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return TullTestJobbUtfører(connection)
        }

        override fun type(): String {
            return "Tull"
        }

        override fun navn(): String {
            return "tull"
        }

        override fun beskrivelse(): String {
            return "tull"
        }
    }
}
