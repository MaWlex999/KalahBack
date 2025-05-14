package com.kalah

import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import com.kalah.auth.AuthManager
import com.kalah.lobby.LobbyManager
import com.kalah.lobby.Lobby
import com.kalah.lobby.CreateLobbyRequest
import com.kalah.lobby.JoinLobbyRequest
import com.kalah.game.GameManager
import com.kalah.game.KalahGame
import com.kalah.game.CreateGameRequest
import com.kalah.game.MoveRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Application.ktorCompatibleRoutes() {
    routing {
        // USERS
        route("/users") {
            get {
                val login = call.request.queryParameters["login"]
                val password = call.request.queryParameters["password"]
                val filter = call.request.queryParameters["filter"]
                when {
                    login != null && password != null -> {
                       
                        if (AuthManager.authenticate(login, password)) {
                            call.respond(mapOf("login" to login))
                        } else {
                            call.respond(HttpStatusCode.Unauthorized, "Неверный логин или пароль")
                        }
                    }
                    filter != null -> {
                        
                        val users = AuthManager.getAllUsers().filter { it.contains(filter) }
                        call.respond(users.map { mapOf("login" to it) })
                    }
                    else -> {
                     
                        val users = AuthManager.getAllUsers()
                        call.respond(users.map { mapOf("login" to it) })
                    }
                }
            }
            post {
                val login = call.request.queryParameters["login"]
                val password = call.request.queryParameters["password"]
                if (login != null && password != null) {
                    if (AuthManager.register(login, password)) {
                        call.respond(HttpStatusCode.Created)
                    } else {
                        call.respond(HttpStatusCode.Conflict, "Пользователь уже существует")
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest, "login и password обязательны")
                }
            }
            put {
                val login = call.request.queryParameters["login"]
                val password = call.request.queryParameters["password"]
                val newUser = call.receive<Map<String, String>>()
                if (login != null && password != null && newUser["login"] != null) {
                    if (AuthManager.authenticate(login, password)) {
                        
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, "Неверный логин или пароль")
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest, "login, password и новый login обязательны")
                }
            }
        }

        // LOBBIES
        route("/lobbies") {
            get {
                // Получить все лобби (заглушка)
                val lobbies = LobbyManager.getAllLobbies()
                call.respond(lobbies)
            }
            post("/create") {
                val lobbyDTO = call.receive<CreateLobbyRequest>()
                val lobby = LobbyManager.createLobby(lobbyDTO.hostName)
                call.respond(lobby)
            }
            post("/{id}/join") {
                val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Нет id")
                val user = call.receive<JoinLobbyRequest>()
                val lobby = LobbyManager.joinLobby(id, user.guestName)
                if (lobby != null) {
                    call.respond(lobby)
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Лобби не найдено или уже заполнено")
                }
            }
        }
        webSocket("/lobbies/{id}/track") {
            val id = call.parameters["id"] ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Нет id"))
            
            LobbyManager.getLobby(id)?.let {
                outgoing.send(Frame.Text(Json.encodeToString(it)))
            }
            for (frame in incoming) {
                
            }
        }

        // GAMES
        route("/games") {
            post("/{id}/start") {
                val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Нет id")
              
                val lobby = LobbyManager.getLobby(id)
                if (lobby != null && lobby.guestName != null) {
                    val game = GameManager.createGame(lobby.hostName, lobby.guestName!!)
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Лобби не готово")
                }
            }
            post("/{id}/move") {
                val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Нет id")
                val params = call.receive<Map<String, String>>()
                val nickname = params["nickname"]
                val holeIndex = params["holeIndex"]?.toIntOrNull()
                val game = GameManager.getGame(id)
                if (game != null && nickname != null && holeIndex != null) {
                    // Определяем номер игрока
                    val player = when (nickname) {
                        game.player1 -> 1
                        game.player2 -> 2
                        else -> return@post call.respond(HttpStatusCode.BadRequest, "Нет такого игрока")
                    }
                    val result = com.kalah.game.makeMove(game, player, holeIndex)
                    if (result) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Некорректный ход")
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Параметры некорректны")
                }
            }
        }
        webSocket("/games/{id}/track") {
            val id = call.parameters["id"] ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Нет id"))
            GameManager.getGame(id)?.let {
                outgoing.send(Frame.Text(Json.encodeToString(it)))
            }
            for (frame in incoming) {
                
            }
        }
    }
}

fun AuthManager.getAllUsers(): List<String> = javaClass.getDeclaredField("users").apply { isAccessible = true }.get(this) as Map<String, String>.keys.toList()
fun LobbyManager.getAllLobbies(): List<Lobby> = javaClass.getDeclaredField("lobbies").apply { isAccessible = true }.get(this) as Map<String, Lobby>.values.toList() 