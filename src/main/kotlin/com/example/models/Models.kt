package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class UserDTO(
    val nickname: String,
    val wins: Map<String, Int>,
    val login: String?,
    val password: String?
)

@Serializable
data class LobbyDTO(
    val id: Int,
    val name: String,
    val initialStones: Int,
    val initialHoles: Int,
    val creator: UserDTO,
    val guest: UserDTO?
)

@Serializable
data class KalahGameStateDTO(
    var holesCount: Int,
    var initialStonesCount: Int,
    var player1Holes: List<Int>,
    var player2Holes: List<Int>,
    var player1Kalah: Int,
    var player2Kalah: Int,
    var currentGameStatus: String,
    var currentPlayerInd: Int,
    var player1Nickname: String,
    var player2Nickname: String,
    var isMakingMove: Boolean = false
)

@Serializable
data class MoveDTO(
    val gameId: Int,
    val playerNickname: String,
    val holeIndex: Int
) 