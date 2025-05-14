package com.kalah.auth

import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.auth.jwt.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

object AuthManager {
    private val users = ConcurrentHashMap<String, String>() // login -> password
    private const val secret = "super_secret_key"
    private const val issuer = "kalah-server"
    private const val audience = "kalah-client"
    private const val validityInMs = 36_000_00 * 24 // 24 часа
    private val algorithm = Algorithm.HMAC256(secret)

    fun register(login: String, password: String): Boolean {
        if (users.containsKey(login)) return false
        users[login] = password
        return true
    }

    fun authenticate(login: String, password: String): Boolean {
        return users[login] == password
    }

    fun generateToken(login: String): String {
        val now = System.currentTimeMillis()
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("login", login)
            .withExpiresAt(Date(now + validityInMs))
            .sign(algorithm)
    }

    fun verifyToken(token: String): String? {
        return try {
            val verifier = JWT.require(algorithm).withIssuer(issuer).withAudience(audience).build()
            val decoded = verifier.verify(token)
            decoded.getClaim("login").asString()
        } catch (e: Exception) {
            null
        }
    }
}

@Serializable
data class RegisterRequest(val login: String, val password: String)
@Serializable
data class AuthRequest(val login: String, val password: String)
@Serializable
data class AuthResponse(val token: String)

fun Route.authRoutes() {
    route("/auth") {
        post("/register") {
            val req = call.receive<RegisterRequest>()
            if (AuthManager.register(req.login, req.password)) {
                call.respondText("Регистрация успешна")
            } else {
                call.respondText("Пользователь уже существует", status = HttpStatusCode.Conflict)
            }
        }
        post("/login") {
            val req = call.receive<AuthRequest>()
            if (AuthManager.authenticate(req.login, req.password)) {
                val token = AuthManager.generateToken(req.login)
                call.respond(AuthResponse(token))
            } else {
                call.respondText("Неверный логин или пароль", status = HttpStatusCode.Unauthorized)
            }
        }
    }
} 