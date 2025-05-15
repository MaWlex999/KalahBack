package com.example.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import com.example.models.*
import com.example.game.GameManager
import io.ktor.util.reflect.TypeInfo
import io.ktor.websocket.Frame
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import java.awt.TrayIcon

private val gameManager = GameManager()

fun Application.configureRouting() {
    routing {
        // User routes
        route("/users") {
            post {
                val login = call.request.queryParameters["login"]
                val password = call.request.queryParameters["password"]
                println("Creating user with login: $login")
                val createdUser = gameManager.createUser(login, password)
                call.respond(createdUser)
            }
            
            get {
                val login = call.request.queryParameters["login"] ?: return@get
                val password = call.request.queryParameters["password"] ?: return@get
                println("Authenticating user with login: $login")
                val user = gameManager.getUserByCredentials(login, password)
                if (user != null) {
                    call.respond(user)
                } else {
                    println("User not found with login: $login")
                    call.respond(404)
                }
            }
        }

        // Lobby routes
        route("/api/lobbies") {
            get {
                println("Getting all lobbies")
                call.respond(gameManager.getAllLobbies())
            }
            
            post {
                val lobby = call.receive<LobbyDTO>()
                println("Creating lobby: ${lobby.name}")
                val createdLobby = gameManager.createLobby(lobby)
                call.respond(createdLobby)
            }
            
            post("/{lobbyId}/join") {
                val lobbyId = call.parameters["lobbyId"]?.toIntOrNull() ?: return@post
                val user = call.receive<UserDTO>()
                println("User ${user.nickname} joining lobby $lobbyId")
                val updatedLobby = gameManager.joinLobby(lobbyId, user)
                if (updatedLobby != null) {
                    call.respond(updatedLobby)
                } else {
                    println("Lobby $lobbyId not found")
                    call.respond(404)
                }
            }
            
            post("/{lobbyId}/start") {
                val lobbyId = call.parameters["lobbyId"]?.toIntOrNull() ?: return@post
                println("Starting game in lobby $lobbyId")
                val success = gameManager.startGame(lobbyId)
                call.respond(mapOf("success" to success))
            }
            
            // WebSocket for lobby updates
            webSocket("/{lobbyId}/track") {
                val lobbyId = call.parameters["lobbyId"]?.toIntOrNull() ?: return@webSocket
                println("WebSocket connection established for tracking lobby $lobbyId")
                try {
                    gameManager.trackLobby(lobbyId).collect { lobby ->
                        send(Frame.Text(Json.encodeToString(LobbyDTO.serializer(), lobby)))
                    }
                } catch (e: ClosedSendChannelException) {
                    println("WebSocket connection closed for lobby $lobbyId")
                }
            }
        }

        // Game routes
        route("/api/games") {
            post("/{gameId}/move") {
                val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@post
                val move = call.receive<MoveDTO>()
                println("Player ${move.playerNickname} making move in game $gameId at hole ${move.holeIndex}")
                val success = gameManager.makeMove(gameId, move.playerNickname, move.holeIndex)
                call.respond(mapOf("success" to success))
            }
            
            // WebSocket for game updates
            webSocket("/{gameId}/track") {
                val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@webSocket
                println("WebSocket connection established for tracking game $gameId")
                try {
                    gameManager.trackGame(gameId).collect { gameState ->
                        send(Frame.Text(Json.encodeToString(KalahGameStateDTO.serializer(), gameState)))
                    }
                } catch (e: ClosedSendChannelException) {
                    println("WebSocket connection closed for game $gameId")
                }
            }
        }
    }
} 