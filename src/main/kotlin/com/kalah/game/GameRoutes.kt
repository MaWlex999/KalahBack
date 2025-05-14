package com.kalah.game

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.kalah.lobby.LobbyManager
import com.kalah.dto.UserDTO

fun Route.gameRoutes() {
    route("/games") {
        post("/{lobbyId}/move") {
            val lobbyId = call.parameters["lobbyId"]?.toIntOrNull() ?: return@post call.respondText("No lobbyId", status = io.ktor.http.HttpStatusCode.BadRequest)
            val nickname = call.request.queryParameters["nickname"] ?: return@post call.respondText("No nickname", status = io.ktor.http.HttpStatusCode.BadRequest)
            val holeIndex = call.request.queryParameters["holeIndex"]?.toIntOrNull() ?: return@post call.respondText("No holeIndex", status = io.ktor.http.HttpStatusCode.BadRequest)
            val lobby = LobbyManager.getById(lobbyId) ?: return@post call.respondText("No lobby", status = io.ktor.http.HttpStatusCode.NotFound)
            val playerInd = when (nickname) {
                lobby.creator.nickname -> 1
                lobby.guest?.nickname -> 2
                else -> return@post call.respondText("Unknown player", status = io.ktor.http.HttpStatusCode.BadRequest)
            }
            val state = GameManager.makeMove(lobbyId, playerInd, holeIndex)
            if (state != null) call.respondText("OK") else call.respondText("Invalid move", status = io.ktor.http.HttpStatusCode.BadRequest)
        }
    }
} 