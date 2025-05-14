package com.kalah.game

import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.Collections
import java.util.concurrent.CopyOnWriteArraySet
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object GameManager {
    private val games = ConcurrentHashMap<String, KalahGame>()
    val gameSessions = mutableMapOf<String, MutableSet<DefaultWebSocketServerSession>>()

    fun createGame(player1: String, player2: String): KalahGame {
        val id = UUID.randomUUID().toString()
        val game = KalahGame(id, player1, player2)
        games[id] = game
        return game
    }

    fun getGame(gameId: String): KalahGame? = games[gameId]
}

@Serializable
data class KalahGame(
    val id: String,
    val player1: String,
    val player2: String,
    var board: List<Int> = List(14) { if (it == 6 || it == 13) 0 else 6 },
    var currentPlayer: Int = 1,
    var isFinished: Boolean = false,
    var winner: Int? = null
)

@Serializable
data class CreateGameRequest(val player1: String, val player2: String)
@Serializable
data class MoveRequest(val gameId: String, val player: Int, val pit: Int)

fun Route.gameRoutes() {
    route("/game") {
        post("/create") {
            val req = call.receive<CreateGameRequest>()
            val game = GameManager.createGame(req.player1, req.player2)
            call.respond(game)
        }
        post("/move") {
            val req = call.receive<MoveRequest>()
            val game = GameManager.getGame(req.gameId)
            if (game == null || game.isFinished) {
                call.respondText("Игра не найдена или завершена", status = io.ktor.http.HttpStatusCode.BadRequest)
                return@post
            }
            val result = makeMove(game, req.player, req.pit)
            if (result) {
                call.respond(game)
            } else {
                call.respondText("Некорректный ход", status = io.ktor.http.HttpStatusCode.BadRequest)
            }
        }
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respondText("Нет id", status = io.ktor.http.HttpStatusCode.BadRequest)
            val game = GameManager.getGame(id)
            if (game != null) {
                call.respond(game)
            } else {
                call.respondText("Игра не найдена", status = io.ktor.http.HttpStatusCode.NotFound)
            }
        }
    }
}

fun Route.websocketRoutes() {
    webSocket("/ws/game/{gameId}") {
        val gameId = call.parameters["gameId"] ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Нет gameId"))
        val sessions = GameManager.gameSessions.getOrPut(gameId) { Collections.synchronizedSet(mutableSetOf()) }
        sessions.add(this)
        try {
            GameManager.getGame(gameId)?.let {
                outgoing.send(Frame.Text(Json.encodeToString(it)))
            }
            for (frame in incoming) {
            }
        } finally {
            sessions.remove(this)
        }
    }
}

suspend fun notifyGameUpdate(game: KalahGame) {
    val sessions = GameManager.gameSessions[game.id] ?: return
    val json = Json.encodeToString(game)
    val toRemove = mutableListOf<DefaultWebSocketServerSession>()
    for (session in sessions) {
        try {
            session.outgoing.send(Frame.Text(json))
        } catch (e: Exception) {
            toRemove.add(session)
        }
    }
    sessions.removeAll(toRemove)
}

fun makeMove(game: KalahGame, player: Int, pit: Int): Boolean {
    if (game.isFinished) return false
    if (game.currentPlayer != player) return false
    val pits = game.board.toMutableList()
    val start = if (player == 1) 0 else 7
    val end = if (player == 1) 5 else 12
    val store = if (player == 1) 6 else 13
    if (pit !in start..end || pits[pit] == 0) return false
    var stones = pits[pit]
    pits[pit] = 0
    var idx = pit
    while (stones > 0) {
        idx = (idx + 1) % 14
        if ((player == 1 && idx == 13) || (player == 2 && idx == 6)) continue
        pits[idx]++
        stones--
    }
    // Проверка на захват
    if (player == 1 && idx in 0..5 && pits[idx] == 1 && pits[12 - idx] > 0) {
        pits[6] += pits[idx] + pits[12 - idx]
        pits[idx] = 0
        pits[12 - idx] = 0
    }
    if (player == 2 && idx in 7..12 && pits[idx] == 1 && pits[12 - idx] > 0) {
        pits[13] += pits[idx] + pits[12 - idx]
        pits[idx] = 0
        pits[12 - idx] = 0
    }
    // Проверка на окончание игры
    val side1 = pits.subList(0, 6).sum()
    val side2 = pits.subList(7, 13).sum()
    if (side1 == 0 || side2 == 0) {
        pits[6] += side1
        pits[13] += side2
        for (i in 0..5) pits[i] = 0
        for (i in 7..12) pits[i] = 0
        game.isFinished = true
        game.winner = when {
            pits[6] > pits[13] -> 1
            pits[13] > pits[6] -> 2
            else -> 0
        }
    }
    if ((player == 1 && idx != 6) || (player == 2 && idx != 13)) {
        game.currentPlayer = if (player == 1) 2 else 1
    }
    game.board = pits
    GlobalScope.launch {
        notifyGameUpdate(game)
    }
    return true
} 