package no.nav.aap.motor

import no.nav.aap.komponenter.dbconnect.Row

public object JobbInputParser {

    public fun mapJobb(row: Row): JobbInput {
        return JobbInput(JobbType.parse(row.getString("type")))
            .medId(row.getLong("id"))
            .medStatus(row.getEnum("status"))
            .gruppering(
                row.getLongOrNull("sak_id"),
                row.getLongOrNull("behandling_id")
            )
            .medAntallFeil(row.getLong("antall_feil"))
            .medProperties(row.getPropertiesOrNull("parameters"))
            .medPayload(row.getStringOrNull("payload"))
            .medOpprettetTidspunkt(row.getLocalDateTime("OPPRETTET_TID"))
            .medNesteKjøring(row.getLocalDateTime("neste_kjoring"))
    }
}