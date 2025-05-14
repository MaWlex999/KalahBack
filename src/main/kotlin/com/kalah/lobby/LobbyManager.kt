package com.kalah.lobby

import com.kalah.dto.LobbyDTO
import com.kalah.dto.UserDTO
import java.util.concurrent.atomic.AtomicInteger

object LobbyManager {
    private val lobbies = mutableListOf<LobbyDTO>()
    private val idCounter = AtomicInteger(1)

    fun getAll(): List<LobbyDTO> = lobbies.toList()

    fun create(name: String, owner: UserDTO, maxPlayers: Int = 2): LobbyDTO {
        val lobby = LobbyDTO(
            id = idCounter.getAndIncrement(),
            name = name,
            players = listOf(owner),
            maxPlayers = maxPlayers,
            ownerId = owner.id,
            status = "waiting"
        )
        lobbies.add(lobby)
        return lobby
    }

    fun join(lobbyId: Int, user: UserDTO): LobbyDTO? {
        val idx = lobbies.indexOfFirst { it.id == lobbyId }
        if (idx == -1) return null
        val lobby = lobbies[idx]
        if (lobby.players.any { it.id == user.id } || lobby.players.size >= lobby.maxPlayers) return null
        val updated = lobby.copy(players = lobby.players + user)
        lobbies[idx] = updated
        return updated
    }

    fun updateStatus(lobbyId: Int, status: String) {
        val idx = lobbies.indexOfFirst { it.id == lobbyId }
        if (idx != -1) {
            lobbies[idx] = lobbies[idx].copy(status = status)
        }
    }

    fun getById(lobbyId: Int): LobbyDTO? = lobbies.find { it.id == lobbyId }
} 