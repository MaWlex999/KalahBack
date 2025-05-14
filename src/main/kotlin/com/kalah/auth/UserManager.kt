package com.kalah.auth

import com.kalah.dto.UserDTO
import java.util.concurrent.atomic.AtomicInteger

object UserManager {
    private val users = mutableListOf<UserDTO>()
    private val idCounter = AtomicInteger(1)

    fun register(login: String, password: String, nickname: String): Boolean {
        if (users.any { it.login == login }) return false
        users.add(UserDTO(idCounter.getAndIncrement(), login, nickname))
        return true
    }

    fun authorize(login: String, password: String): UserDTO? {
        // Для простоты пароль не проверяем, только login
        return users.find { it.login == login }
    }

    fun getAll(): List<UserDTO> = users.toList()

    fun filterByName(filter: String): List<UserDTO> =
        users.filter { it.nickname.contains(filter, ignoreCase = true) }

    fun update(login: String, newUser: UserDTO): Boolean {
        val idx = users.indexOfFirst { it.login == login }
        if (idx == -1) return false
        users[idx] = newUser.copy(id = users[idx].id, login = login)
        return true
    }
} 