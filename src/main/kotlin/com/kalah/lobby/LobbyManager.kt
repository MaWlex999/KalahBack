package com.kalah.lobby

import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID

object LobbyManager {
    private val lobbies = ConcurrentHashMap<String, Lobby>()

    fun createLobby(hostName: String): Lobby {
        val id = UUID.randomUUID().toString()
        val lobby = Lobby(id, hostName)
        lobbies[id] = lobby
        return lobby
    }

    fun joinLobby(lobbyId: String, guestName: String): Lobby? {
        val lobby = lobbies[lobbyId]
        if (lobby != null && lobby.guestName == null) {
            lobby.guestName = guestName
            lobby.isReady = true
            return lobby
        }
        return null
    }

    fun getLobby(lobbyId: String): Lobby? = lobbies[lobbyId]
}

@Serializable
data class Lobby(
    val id: String,
    val hostName: String,
    var guestName: String? = null,
    var isReady: Boolean = false
)

@Serializable
data class CreateLobbyRequest(val hostName: String)
@Serializable
data class JoinLobbyRequest(val lobbyId: String, val guestName: String)

fun Route.lobbyRoutes() {
    route("/lobby") {
        post("/create") {
            val req = call.receive<CreateLobbyRequest>()
            val lobby = LobbyManager.createLobby(req.hostName)
            call.respond(lobby)
        }
        post("/join") {
            val req = call.receive<JoinLobbyRequest>()
            val lobby = LobbyManager.joinLobby(req.lobbyId, req.guestName)
            if (lobby != null) {
                call.respond(lobby)
            } else {
                call.respondText("Лобби не найдено или уже заполнено", status = io.ktor.http.HttpStatusCode.BadRequest)
            }
        }
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respondText("Нет id", status = io.ktor.http.HttpStatusCode.BadRequest)
            val lobby = LobbyManager.getLobby(id)
            if (lobby != null) {
                call.respond(lobby)
            } else {
                call.respondText("Лобби не найдено", status = io.ktor.http.HttpStatusCode.NotFound)
            }
        }
    }
} 