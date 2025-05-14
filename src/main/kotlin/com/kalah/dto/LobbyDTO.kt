package com.kalah.dto

import com.kalah.dto.UserDTO

// Статусы: waiting, started, finished

data class LobbyDTO(
    val id: Int,
    val name: String,
    val initialStones: Int,
    val initialHoles: Int,
    val creator: UserDTO,
    val guest: UserDTO?
) 