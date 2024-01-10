package io.skinnydoo.users

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.skinnydoo.API_V1
import io.skinnydoo.common.*
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.ktor.ext.inject

@Resource("/users/login")
class UserLoginRoute

@Resource("/users")
class UserCreateRoute

@Resource("/user")
class UserRoute

/**
 * Register a new user.
 *
 * POST /v1/users
 */
fun Route.createUser() {
  val registerUser by inject<RegisterUser>(named("register"))
  val jwtService by inject<JwtService> { parametersOf(application.environment.jwtConfig(JwtService.CONFIG_PATH)) }

  post<UserCreateRoute> {
    val newUser = call.receive<RegisterUserRequest>().user
    val hashedPassword = Password(hash(newUser.password.value))
    val newUserWithHashedPassword = newUser.copy(password = hashedPassword)

    registerUser(newUserWithHashedPassword).map {
      UserResponse(LoggedInUser.fromUser(it, token = jwtService.generateToken(it)))
    }.fold({ handleErrors(it) }, { call.respond(HttpStatusCode.Created, it) })
  }
}

/**
 * Login for existing user.
 *
 * POST v1/users/login
 */
fun Route.loginUser() {
  val loginWithEmail by inject<LoginUser>(named("login"))
  val jwtService by inject<JwtService> { parametersOf(application.environment.jwtConfig(JwtService.CONFIG_PATH)) }

  post<UserLoginRoute> {
    val body = call.receive<UserLoginRequest>().user
    loginWithEmail(body).map { UserResponse(LoggedInUser.fromUser(it, token = jwtService.generateToken(it))) }
      .fold({ handleErrors(it) }, { call.respond(HttpStatusCode.Created, it) })
  }
}

/**
 * Gets the currently logged-in user.
 *
 * GET /v1/user
 */
fun Route.getCurrentUser() {
  defaultAuthenticate() {
    get<UserRoute> {
      val loggedInUser = call.principal<User>() ?: return@get call.respond(
        HttpStatusCode.Unauthorized,
        ErrorEnvelope(mapOf("body" to listOf("Unknown user")))
      )
      call.respond(UserResponse(LoggedInUser.fromUser(loggedInUser, token = "")))
    }
  }
}

/**
 * Updated user information for current user
 * PUT /v1/user
 */
fun Route.updateUser() {
  val updateUser by inject<UpdateUser>(named("updateUser"))

  defaultAuthenticate() {
    put<UserRoute> {
      val body = call.receive<UserUpdateRequest>().user

      val loggedInUser = call.principal<User>() ?: return@put call.respond(
        HttpStatusCode.Unauthorized,
        ErrorEnvelope(mapOf("body" to listOf("Unknown user")))
      )

      updateUser(loggedInUser.id, body).map { UserResponse(LoggedInUser.fromUser(it, token = "")) }
        .fold({ handleErrors(it) }, { call.respond(it) })
    }
  }
}

fun Application.registerUserRoutes() {
  routing {
    route(API_V1) {
      createUser()
      loginUser()
      getCurrentUser()
      updateUser()
    }
  }
}
