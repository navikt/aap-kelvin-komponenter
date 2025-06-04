package no.nav.aap.motor.help

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbUtfører

class EnAnnenRekkefølgeTestJobbUtfører(connection: DBConnection) : RekkefølgeTestJobbUtfører(connection) {

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return EnAnnenRekkefølgeTestJobbUtfører(connection)
        }

        override fun type(): String {
            return "EnAnnenTullRekkefolge"
        }

        override fun navn(): String {
            return "EnAnnenTullRekkefolge"
        }

        override fun beskrivelse(): String {
            return "EnAnnenTullRekkefolge"
        }
    }
}
