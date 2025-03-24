package no.nav.aap.motor.api

import com.papsign.ktor.openapigen.APITag
import com.papsign.ktor.openapigen.route.TagModule
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.mdc.JobbLogInfoProviderHolder
import no.nav.aap.motor.retry.DriftJobbRepositoryExposed
import java.time.LocalDateTime
import javax.sql.DataSource

private enum class Tags(override val description: String) : APITag {
    MotorAPI(
        "Disse endepunktene er drift-endepunkter for motoren."
    ),
}

public fun NormalOpenAPIRoute.motorApi(dataSource: DataSource) {
    val modules = TagModule(listOf(Tags.MotorAPI))
    route("/drift/api/jobb") {
        route("/feilende") {
            get<Unit, List<JobbInfoDto>>(modules) { _ ->
                val saker: List<JobbInfoDto> = dataSource.transaction(readOnly = true) { connection ->
                    DriftJobbRepositoryExposed(connection).hentAlleFeilende()
                        .map { (jobbInput, jobbStatus) ->
                            jobbInfoDto(jobbInput, jobbStatus, connection)
                        }

                }
                respond(saker)
            }
        }
        route("/planlagte-jobber") {
            get<Unit, List<JobbInfoDto>>(modules) { _ ->
                val saker: List<JobbInfoDto> = dataSource.transaction(readOnly = true) { connection ->
                    DriftJobbRepositoryExposed(connection).hentInfoOmGjentagendeJobber().map { info ->
                        JobbInfoDto(
                            id = info.jobbId(),
                            type = info.type(),
                            status = info.status(),
                            planlagtKjøretidspunkt = info.nesteKjøring(),
                            metadata = mapOf(),
                            antallFeilendeForsøk = 0,
                            beskrivelse = info.beskrivelse(),
                            navn = info.navn()
                        )
                    }
                }
                respond(saker)
            }
        }
        route("/{jobbId}/kjor") {
            post<JobbIdDTO, String, Unit>(modules) { jobbId, _ ->
                val nå = LocalDateTime.now()
                val oppdatert = dataSource.transaction { connection ->
                    DriftJobbRepositoryExposed(connection).settNesteKjøring(jobbId.jobbId, nå)
                }
                if (oppdatert == 0) {
                    respond("Kunne ikke oppdatere tidspunkt for neste kjøring for jobb med ID $jobbId")
                } 
                respond("Setter neste kjøring for jobb med ID $jobbId til $nå.")
            }
        }
        route("/rekjor/{jobbId}") {
            get<JobbIdDTO, String>(modules) { jobbId ->
                val antallSchedulert = dataSource.transaction { connection ->
                    DriftJobbRepositoryExposed(connection).markerFeilendeForKlar(jobbId.jobbId)
                }
                respond("Rekjøring av feilende jobb med ID $jobbId startet, startet $antallSchedulert jobber.")
            }
        }
        route("/avbryt/{jobbId}") {
            get<JobbIdDTO, String>(modules) { jobbId ->
                val antallSchedulert = dataSource.transaction { connection ->
                    DriftJobbRepositoryExposed(connection).markerSomAvbrutt(jobbId.jobbId)
                }
                respond("Avbryter videre kjøring av feilende jobb med ID $jobbId startet, antall jobber avbrutt $antallSchedulert.")
            }
        }
        route("/rekjorAlleFeilede") {
            get<Unit, String>(modules) {
                val antallSchedulert = dataSource.transaction { connection ->
                    DriftJobbRepositoryExposed(connection).markerAlleFeiledeForKlare()
                }
                respond("Rekjøring av feilede startet, startet $antallSchedulert jobber.")
            }
        }
        route("/sisteKjørte") {
            get<Unit, List<JobbInfoDto>>(modules) { _ ->
                val saker: List<JobbInfoDto> = dataSource.transaction(readOnly = true) { connection ->
                    DriftJobbRepositoryExposed(connection).hentSisteJobber(150)
                        .map { (jobbInput, feilmelding) ->
                            jobbInfoDto(jobbInput, feilmelding, connection)
                        }

                }
                respond(saker)
            }
        }
    }
}

private fun jobbInfoDto(
    jobbInput: JobbInput,
    feilmelding: String?,
    connection: DBConnection
): JobbInfoDto {
    return JobbInfoDto(
        id = jobbInput.jobbId(),
        type = jobbInput.type(),
        navn = jobbInput.navn(),
        beskrivelse = jobbInput.beskrivelse(),
        status = jobbInput.status(),
        antallFeilendeForsøk = jobbInput.antallRetriesForsøkt(),
        feilmelding = feilmelding,
        planlagtKjøretidspunkt = jobbInput.nesteKjøring(),
        metadata = JobbLogInfoProviderHolder.get()
            .hentInformasjon(connection, jobbInput)?.felterMedVerdi
            ?: mapOf()
    )
}

