package com.kalah.game

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import com.kalah.lobby.LobbyManager
import com.kalah.game.GameManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Route.websocketRoutes() {
    webSocket("/lobbies/{lobbyId}/track") {
        val lobbyId = call.parameters["lobbyId"]?.toIntOrNull() ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "No lobbyId"))
        while (true) {
            val lobby = LobbyManager.getById(lobbyId) ?: break
            outgoing.send(Frame.Text(Json.encodeToString(lobby)))
            // Можно добавить задержку или реакцию на события
            kotlinx.coroutines.delay(2000)
        }
    }
    webSocket("/games/{lobbyId}/track") {
        val lobbyId = call.parameters["lobbyId"]?.toIntOrNull() ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "No lobbyId"))
        while (true) {
            val state = GameManager.getGame(lobbyId) ?: break
            outgoing.send(Frame.Text(Json.encodeToString(state)))
            // Можно добавить задержку или реакцию на события
            kotlinx.coroutines.delay(1000)
        }
    }
} 