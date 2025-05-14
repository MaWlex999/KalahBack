package com.kalah.dto

import com.kalah.dto.UserDTO

data class KalahGameStateDTO(
    val lobbyId: Int,
    val board: List<Int>,
    val currentPlayerId: Int,
    val players: List<UserDTO>,
    val isFinished: Boolean,
    val winnerId: Int?
) 