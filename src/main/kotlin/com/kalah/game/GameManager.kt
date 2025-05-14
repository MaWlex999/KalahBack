package com.kalah.game

import com.kalah.dto.KalahGameStateDTO
import com.kalah.dto.UserDTO
import com.kalah.dto.GameStatus
import java.util.concurrent.ConcurrentHashMap

object GameManager {
    private val games = ConcurrentHashMap<Int, KalahGameStateDTO>()

    fun startGame(lobbyId: Int, player1: UserDTO, player2: UserDTO, holes: Int, stones: Int): KalahGameStateDTO {
        val state = KalahGameStateDTO(
            holesCount = holes,
            initialStonesCount = stones,
            player1Holes = MutableList(holes) { stones },
            player2Holes = MutableList(holes) { stones },
            player1Kalah = 0,
            player2Kalah = 0,
            currentGameStatus = GameStatus.PLAYING.name,
            currentPlayerInd = 1,
            player1Nickname = player1.nickname,
            player2Nickname = player2.nickname,
            isMakingMove = false
        )
        games[lobbyId] = state
        return state
    }

    fun getGame(lobbyId: Int): KalahGameStateDTO? = games[lobbyId]

    fun makeMove(lobbyId: Int, player: Int, holeIndex: Int): KalahGameStateDTO? {
        val state = games[lobbyId]?.copy(
            player1Holes = games[lobbyId]?.player1Holes?.toMutableList() ?: return null,
            player2Holes = games[lobbyId]?.player2Holes?.toMutableList() ?: return null
        ) ?: return null
        if (state.currentGameStatus != GameStatus.PLAYING.name) return state
        val holesCount = state.holesCount
        val initialStonesCount = state.initialStonesCount
        var stones = if (player == 1) state.player1Holes[holeIndex] else state.player2Holes[holeIndex]
        if (stones == 0) return state
        if (player == 1) state.player1Holes[holeIndex] = 0 else state.player2Holes[holeIndex] = 0
        var currentIndex = holeIndex
        var isPlayer1 = (player == 1)
        var isLastInKalah = false
        var isLastInEmpty = false
        while (stones > 0) {
            if (isPlayer1) {
                if (currentIndex < holesCount - 1) {
                    currentIndex++
                    state.player1Holes[currentIndex]++
                } else {
                    state.player1Kalah++
                    isLastInKalah = true
                    isPlayer1 = false
                    currentIndex = holesCount - 1
                }
            } else {
                if (currentIndex > 0) {
                    currentIndex--
                    state.player2Holes[currentIndex]++
                } else {
                    state.player2Kalah++
                    isLastInKalah = true
                    isPlayer1 = true
                    currentIndex = 0
                }
            }
            stones--
            if (stones == 0) {
                isLastInEmpty = if (isPlayer1) state.player1Holes[currentIndex] == 1 else state.player2Holes[currentIndex] == 1
            }
        }
        // Захват камней, если последний в пустую свою лунку
        if (isLastInEmpty) {
            if (isPlayer1 && state.player1Holes[currentIndex] == 1) {
                state.player1Kalah += state.player2Holes[currentIndex] + 1
                state.player1Holes[currentIndex] = 0
                state.player2Holes[currentIndex] = 0
            } else if (!isPlayer1 && state.player2Holes[currentIndex] == 1) {
                state.player2Kalah += state.player1Holes[currentIndex] + 1
                state.player2Holes[currentIndex] = 0
                state.player1Holes[currentIndex] = 0
            }
        }
        checkForEnding(state)
        state.currentPlayerInd = if (isLastInKalah) player else if (player == 1) 2 else 1
        games[lobbyId] = state
        return state
    }

    private fun checkForEnding(s: KalahGameStateDTO) {
        val totalStones = s.initialStonesCount * s.holesCount
        if (s.player1Kalah > totalStones) {
            s.currentGameStatus = GameStatus.PLAYER1_WIN.name
            return
        }
        if (s.player2Kalah > totalStones) {
            s.currentGameStatus = GameStatus.PLAYER2_WIN.name
            return
        }
        if (s.player1Holes.all { it == 0 } || s.player2Holes.all { it == 0 }) {
            if (s.player1Holes.all { it == 0 }) {
                s.player2Kalah += s.player2Holes.sum()
            } else {
                s.player1Kalah += s.player1Holes.sum()
            }
            s.currentGameStatus = when {
                s.player1Kalah > s.player2Kalah -> GameStatus.PLAYER1_WIN.name
                s.player1Kalah < s.player2Kalah -> GameStatus.PLAYER2_WIN.name
                else -> GameStatus.DRAW.name
            }
        }
    }
} 