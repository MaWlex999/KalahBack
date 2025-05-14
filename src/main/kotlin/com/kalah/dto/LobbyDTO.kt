package com.kalah.dto

import com.kalah.dto.UserDTO

// Статусы: waiting, started, finished

data class LobbyDTO(
    val id: Int,
    val name: String,
    val players: List<UserDTO>,
    val maxPlayers: Int,
    val ownerId: Int,
    val status: String
) 