package com.kalah

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import com.kalah.lobby.lobbyRoutes
import com.kalah.game.gameRoutes
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import com.kalah.auth.authRoutes
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.kalah.game.websocketRoutes


fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    install(WebSockets)
    install(Authentication) {
        jwt {
            realm = "kalah-server"
            verifier(
                JWT.require(Algorithm.HMAC256("super_secret_key"))
                    .withIssuer("kalah-server")
                    .withAudience("kalah-client")
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("login").asString().isNotEmpty()) JWTPrincipal(credential.payload) else null
            }
        }
    }
    routing {
        authRoutes()
        lobbyRoutes()
        gameRoutes()
        websocketRoutes()
    }
} 