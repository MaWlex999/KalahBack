package com.kalah.auth

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.kalah.dto.UserDTO

fun Route.authRoutes() {
    route("/users") {
        get {
            val login = call.request.queryParameters["login"]
            val password = call.request.queryParameters["password"]
            val filter = call.request.queryParameters["filter"]
            when {
                login != null && password != null -> {
                    val user = UserManager.authorize(login, password)
                    if (user != null) call.respond(user)
                    else call.respondText("Not found", status = io.ktor.http.HttpStatusCode.NotFound)
                }
                filter != null -> call.respond(UserManager.filterByName(filter))
                else -> call.respond(UserManager.getAll())
            }
        }
        post {
            val login = call.request.queryParameters["login"] ?: return@post call.respondText("No login", status = io.ktor.http.HttpStatusCode.BadRequest)
            val password = call.request.queryParameters["password"] ?: return@post call.respondText("No password", status = io.ktor.http.HttpStatusCode.BadRequest)
            val nickname = call.request.queryParameters["nickname"] ?: login
            val ok = UserManager.register(login, password, nickname)
            if (ok) call.respondText("OK") else call.respondText("Already exists", status = io.ktor.http.HttpStatusCode.Conflict)
        }
        put {
            val login = call.request.queryParameters["login"] ?: return@put call.respondText("No login", status = io.ktor.http.HttpStatusCode.BadRequest)
            val password = call.request.queryParameters["password"] ?: return@put call.respondText("No password", status = io.ktor.http.HttpStatusCode.BadRequest)
            val newUser = call.receive<UserDTO>()
            val ok = UserManager.update(login, newUser)
            if (ok) call.respondText("OK") else call.respondText("Not found", status = io.ktor.http.HttpStatusCode.NotFound)
        }
    }
} 