package com.kalah.lobby

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.kalah.dto.LobbyDTO
import com.kalah.dto.UserDTO

@kotlinx.serialization.Serializable
data class CreateLobbyRequest(
    val name: String,
    val initialStones: Int,
    val initialHoles: Int,
    val creator: UserDTO
)

@kotlinx.serialization.Serializable
data class JoinLobbyRequest(val lobbyId: Int, val user: UserDTO)

fun Route.lobbyRoutes() {
    route("/lobbies") {
        get {
            call.respond(LobbyManager.getAll())
        }
        post("/create") {
            val req = call.receive<CreateLobbyRequest>()
            val lobby = LobbyManager.create(req.name, req.initialStones, req.initialHoles, req.creator)
            call.respond(lobby)
        }
        post("/{lobbyId}/join") {
            val lobbyId = call.parameters["lobbyId"]?.toIntOrNull() ?: return@post call.respondText("No lobbyId", status = io.ktor.http.HttpStatusCode.BadRequest)
            val user = call.receive<UserDTO>()
            val lobby = LobbyManager.join(lobbyId, user)
            if (lobby != null) call.respond(lobby)
            else call.respondText("Cannot join", status = io.ktor.http.HttpStatusCode.BadRequest)
        }
    }
} 