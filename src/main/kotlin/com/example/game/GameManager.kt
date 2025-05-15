package com.example.game

import com.example.models.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json

class GameManager {
    private val users = mutableMapOf<String?, UserDTO>()
    private val lobbies = mutableMapOf<Int, LobbyDTO>()
    private val games = mutableMapOf<Int, KalahGameStateDTO>()
    private var nextLobbyId = 1
    private var nextGameId = 1

    private val lobbyUpdates = MutableSharedFlow<LobbyDTO>()
    private val gameUpdates = MutableSharedFlow<KalahGameStateDTO>()

    // User management
    fun getAllUsers(): List<UserDTO> = users.values.toList()

    fun createUser(login: String?, password: String?): UserDTO {
        val user = UserDTO(login ?: "???", emptyMap(), login, password)
        users[login] = user
        return user
    }

    fun getUserByCredentials(login: String, password: String): UserDTO? {
        return users.values.find { it.login == login && it.password == password }
    }

    // Lobby management
    fun getAllLobbies(): List<LobbyDTO> = lobbies.values.toList()

    fun createLobby(lobby: LobbyDTO): LobbyDTO {
        val newLobby = lobby.copy(id = nextLobbyId++)
        lobbies[newLobby.id] = newLobby
        return newLobby
    }

    fun joinLobby(lobbyId: Int, user: UserDTO): LobbyDTO? {
        val lobby = lobbies[lobbyId] ?: return null
        if (lobby.guest != null) return null
        
        val updatedLobby = lobby.copy(guest = user)
        lobbies[lobbyId] = updatedLobby
        return updatedLobby
    }

    fun startGame(lobbyId: Int): Boolean {
        val lobby = lobbies[lobbyId] ?: return false
        if (lobby.guest == null) return false

        val gameState = KalahGameStateDTO(
            holesCount = lobby.initialHoles,
            initialStonesCount = lobby.initialStones,
            player1Holes = MutableList(lobby.initialHoles) { lobby.initialStones },
            player2Holes = MutableList(lobby.initialHoles) { lobby.initialStones },
            player1Kalah = 0,
            player2Kalah = 0,
            currentGameStatus = "IN_PROGRESS",
            currentPlayerInd = 0,
            player1Nickname = lobby.creator.nickname,
            player2Nickname = lobby.guest.nickname
        )
        
        games[nextGameId] = gameState
        return true
    }

    fun trackLobby(lobbyId: Int): SharedFlow<LobbyDTO> {
        return lobbyUpdates.asSharedFlow()
    }

    // Game management
    fun makeMove(gameId: Int, playerNickname: String, holeIndex: Int): Boolean {
        val game = games[gameId] ?: return false
        if (game.isMakingMove) return false
        
        val isPlayer1 = game.player1Nickname == playerNickname
        val isPlayer2 = game.player2Nickname == playerNickname
        if (!isPlayer1 && !isPlayer2) return false
        
        val currentPlayerInd = if (isPlayer1) 0 else 1
        if (game.currentPlayerInd != currentPlayerInd) return false

        game.isMakingMove = true
        
        val holes = if (isPlayer1) game.player1Holes.toMutableList() else game.player2Holes.toMutableList()
        val kalah = if (isPlayer1) game.player1Kalah else game.player2Kalah
        
        if (holeIndex < 0 || holeIndex >= holes.size || holes[holeIndex] == 0) {
            game.isMakingMove = false
            return false
        }

        var stones = holes[holeIndex]
        holes[holeIndex] = 0
        var currentIndex = holeIndex + 1
        var currentPlayer = currentPlayerInd

        while (stones > 0) {
            if (currentIndex >= holes.size) {
                if (currentPlayer == currentPlayerInd) {
                    if (isPlayer1) game.player1Kalah++ else game.player2Kalah++
                    stones--
                }
                currentPlayer = 1 - currentPlayer
                currentIndex = 0
                continue
            }

            if (currentPlayer == currentPlayerInd) {
                holes[currentIndex]++
                stones--
            }
            currentIndex++
        }

        // Check if last stone was placed in empty hole
        if (currentIndex > 0 && currentIndex <= holes.size && holes[currentIndex - 1] == 1) {
            val oppositeIndex = holes.size - currentIndex
            val oppositeHoles = if (isPlayer1) game.player2Holes.toMutableList() else game.player1Holes.toMutableList()
            if (oppositeHoles[oppositeIndex] > 0) {
                if (isPlayer1) {
                    game.player1Kalah += holes[currentIndex - 1] + oppositeHoles[oppositeIndex]
                } else {
                    game.player2Kalah += holes[currentIndex - 1] + oppositeHoles[oppositeIndex]
                }
                holes[currentIndex - 1] = 0
                oppositeHoles[oppositeIndex] = 0
            }
        }

        // Update game state
        if (isPlayer1) {
            game.player1Holes = holes
        } else {
            game.player2Holes = holes
        }

        // Check if game is over
        val isGameOver = game.player1Holes.all { it == 0 } || game.player2Holes.all { it == 0 }
        if (isGameOver) {
            game.player1Kalah += game.player1Holes.sum()
            game.player2Kalah += game.player2Holes.sum()
            game.player1Holes = MutableList(game.holesCount) { 0 }
            game.player2Holes = MutableList(game.holesCount) { 0 }
            game.currentGameStatus = "FINISHED"
        } else {
            game.currentPlayerInd = 1 - game.currentPlayerInd
        }

        game.isMakingMove = false
        return true
    }

    fun trackGame(gameId: Int): SharedFlow<KalahGameStateDTO> {
        return gameUpdates.asSharedFlow()
    }
} 