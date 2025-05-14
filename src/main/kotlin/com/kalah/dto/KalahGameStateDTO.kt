package com.kalah.dto

import com.kalah.dto.UserDTO

data class KalahGameStateDTO(
    var holesCount: Int,
    var initialStonesCount: Int,
    var player1Holes: MutableList<Int>,
    var player2Holes: MutableList<Int>,
    var player1Kalah: Int,
    var player2Kalah: Int,
    var currentGameStatus: String, // Используем String для передачи по сети
    var currentPlayerInd: Int,
    var player1Nickname: String,
    var player2Nickname: String,
    var isMakingMove: Boolean = false
) 