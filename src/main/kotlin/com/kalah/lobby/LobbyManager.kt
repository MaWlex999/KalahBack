package com.kalah.lobby

import com.kalah.dto.LobbyDTO
import com.kalah.dto.UserDTO
import java.util.concurrent.atomic.AtomicInteger

object LobbyManager {
    private val lobbies = mutableListOf<LobbyDTO>()
    private val idCounter = AtomicInteger(1)

    fun getAll(): List<LobbyDTO> = lobbies.toList()

    fun create(name: String, initialStones: Int, initialHoles: Int, creator: UserDTO): LobbyDTO {
        val lobby = LobbyDTO(
            id = idCounter.getAndIncrement(),
            name = name,
            initialStones = initialStones,
            initialHoles = initialHoles,
            creator = creator,
            guest = null
        )
        lobbies.add(lobby)
        return lobby
    }

    fun join(lobbyId: Int, user: UserDTO): LobbyDTO? {
        val idx = lobbies.indexOfFirst { it.id == lobbyId }
        if (idx == -1) return null
        val lobby = lobbies[idx]
        if (lobby.guest != null) return null
        val updated = lobby.copy(guest = user)
        lobbies[idx] = updated
        return updated
    }

    fun getById(lobbyId: Int): LobbyDTO? = lobbies.find { it.id == lobbyId }
} 