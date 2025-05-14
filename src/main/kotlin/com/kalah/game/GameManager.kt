package com.kalah.game

import com.kalah.dto.KalahGameStateDTO
import com.kalah.dto.UserDTO
import java.util.concurrent.ConcurrentHashMap

object GameManager {
    private val games = ConcurrentHashMap<Int, KalahGameStateDTO>()

    fun startGame(lobbyId: Int, players: List<UserDTO>): KalahGameStateDTO {
        val board = List(14) { if (it == 6 || it == 13) 0 else 6 } // классическая доска калах
        val state = KalahGameStateDTO(
            lobbyId = lobbyId,
            board = board,
            currentPlayerId = players.first().id,
            players = players,
            isFinished = false,
            winnerId = null
        )
        games[lobbyId] = state
        return state
    }

    fun getGame(lobbyId: Int): KalahGameStateDTO? = games[lobbyId]

    fun makeMove(lobbyId: Int, playerId: Int, holeIndex: Int): KalahGameStateDTO? {
        val state = games[lobbyId] ?: return null
        val nextPlayerId = state.players.firstOrNull { it.id != playerId }?.id ?: playerId
        val newState = state.copy(currentPlayerId = nextPlayerId)
        games[lobbyId] = newState
        return newState
    }

    fun finishGame(lobbyId: Int, winnerId: Int?) {
        val state = games[lobbyId] ?: return
        games[lobbyId] = state.copy(isFinished = true, winnerId = winnerId)
    }
} 