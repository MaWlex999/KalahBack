package com.kalah.lobby

import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID
import com.kalah.dto.LobbyDTO
import com.kalah.dto.UserDTO
import java.util.concurrent.atomic.AtomicInteger

object LobbyManager {
    private val lobbies = mutableListOf<LobbyDTO>()
    private val idCounter = AtomicInteger(1)

    fun getAll(): List<LobbyDTO> = lobbies.toList()

    fun create(name: String, owner: UserDTO, maxPlayers: Int = 2): LobbyDTO {
        val lobby = LobbyDTO(
            id = idCounter.getAndIncrement(),
            name = name,
            players = listOf(owner),
            maxPlayers = maxPlayers,
            ownerId = owner.id,
            status = "waiting"
        )
        lobbies.add(lobby)
        return lobby
    }

    fun join(lobbyId: Int, user: UserDTO): LobbyDTO? {
        val idx = lobbies.indexOfFirst { it.id == lobbyId }
        if (idx == -1) return null
        val lobby = lobbies[idx]
        if (lobby.players.any { it.id == user.id } || lobby.players.size >= lobby.maxPlayers) return null
        val updated = lobby.copy(players = lobby.players + user)
        lobbies[idx] = updated
        return updated
    }

    fun updateStatus(lobbyId: Int, status: String) {
        val idx = lobbies.indexOfFirst { it.id == lobbyId }
        if (idx != -1) {
            lobbies[idx] = lobbies[idx].copy(status = status)
        }
    }

    fun getById(lobbyId: Int): LobbyDTO? = lobbies.find { it.id == lobbyId }
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
            val lobby = LobbyManager.create(req.hostName, UserDTO(0, ""), 2)
            call.respond(lobby)
        }
        post("/join") {
            val req = call.receive<JoinLobbyRequest>()
            val lobby = LobbyManager.join(req.lobbyId.toInt(), UserDTO(0, req.guestName))
            if (lobby != null) {
                call.respond(lobby)
            } else {
                call.respondText("Лобби не найдено или уже заполнено", status = io.ktor.http.HttpStatusCode.BadRequest)
            }
        }
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respondText("Нет id", status = io.ktor.http.HttpStatusCode.BadRequest)
            val lobby = LobbyManager.getById(id.toInt())
            if (lobby != null) {
                call.respond(lobby)
            } else {
                call.respondText("Лобби не найдено", status = io.ktor.http.HttpStatusCode.NotFound)
            }
        }
    }
} 