package com.kalah.auth

import com.kalah.dto.UserDTO

object UserManager {
    private val users = mutableListOf<UserDTO>()

    fun register(login: String, password: String, nickname: String): Boolean {
        if (users.any { it.login == login }) return false
        users.add(UserDTO(nickname = nickname, wins = emptyMap(), login = login, password = password))
        return true
    }

    fun authorize(login: String, password: String): UserDTO? {
        return users.find { it.login == login && it.password == password }
    }

    fun getAll(): List<UserDTO> = users.toList()

    fun filterByName(filter: String): List<UserDTO> =
        users.filter { it.nickname.contains(filter, ignoreCase = true) }

    fun update(login: String, newUser: UserDTO): Boolean {
        val idx = users.indexOfFirst { it.login == login }
        if (idx == -1) return false
        users[idx] = newUser.copy(login = login)
        return true
    }
} 