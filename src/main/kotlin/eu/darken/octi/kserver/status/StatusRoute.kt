package eu.darken.octi.kserver.status

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import javax.inject.Inject

class StatusRoute @Inject constructor(

) {

    fun setup(rootRoute: Routing) {
        rootRoute.get("/v1/status") {
            call.respondText(
                "Status: ${call.application.developmentMode}",
                ContentType.Application.Json
            )
        }
    }
}