package com.kalah.dto

data class UserDTO(
    val id: Int,
    val login: String,
    val nickname: String,
    val avatarUrl: String? = null
) 