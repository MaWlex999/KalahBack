package com.kalah.game

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import com.kalah.lobby.LobbyManager
import com.kalah.game.GameManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.isActive

fun Route.websocketRoutes() {
    webSocket("/lobbies/{lobbyId}/track") {
        val lobbyId = call.parameters["lobbyId"]?.toIntOrNull() ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "No lobbyId"))
        var lastLobbyJson: String? = null
        while (isActive) {
            val lobby = LobbyManager.getById(lobbyId) ?: break
            val lobbyJson = Json.encodeToString(lobby)
            if (lobbyJson != lastLobbyJson) {
                outgoing.send(Frame.Text(lobbyJson))
                lastLobbyJson = lobbyJson
            }
            kotlinx.coroutines.delay(500)
        }
    }
    webSocket("/games/{lobbyId}/track") {
        val lobbyId = call.parameters["lobbyId"]?.toIntOrNull() ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "No lobbyId"))
        var lastGameJson: String? = null
        while (isActive) {
            val state = GameManager.getGame(lobbyId) ?: break
            val gameJson = Json.encodeToString(state)
            if (gameJson != lastGameJson) {
                outgoing.send(Frame.Text(gameJson))
                lastGameJson = gameJson
            }
            kotlinx.coroutines.delay(300)
        }
    }
} 