
package io.skinnydoo.profiles

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.skinnydoo.API_V1
import io.skinnydoo.common.ErrorEnvelope
import io.skinnydoo.common.Username
import io.skinnydoo.common.defaultAuthenticate
import io.skinnydoo.common.handleErrors
import io.skinnydoo.users.User
import org.koin.core.qualifier.named
import org.koin.ktor.ext.inject

@Resource("/profiles/{username}")
data class UserProfileRoute(val username: String)

@Resource("/profiles/{username}/follow")
data class FollowUserRoute(val username: String)

fun Route.getUserProfile() {
  val getProfileForUser by inject<GetUserProfileUseCase>(named("getUserProfile"))

  defaultAuthenticate(optional = true) {
    get<UserProfileRoute> { params ->
      val self = call.principal<User>()
      getProfileForUser(self?.id, Username(params.username))
        .map { ProfileResponse(it) }
        .fold({ handleErrors(it) }, { call.respond(it) })
    }
  }
}

fun Route.followUser() {
  val followUser by inject<FollowUserUseCase>(named("followUser"))

  defaultAuthenticate() {
    post<FollowUserRoute> { params ->
      val self = call.principal<User>()
        ?: return@post call.respond(
          HttpStatusCode.Unauthorized,
          ErrorEnvelope(mapOf("body" to listOf("Unauthorized")))
        )

      followUser(self.id, Username(params.username))
        .map { ProfileResponse(it) }
        .fold({ handleErrors(it) }, { call.respond(it) })
    }
  }
}

fun Route.unfollowUser() {
  val unfollowUser by inject<UnfollowUserUseCase>(named("unfollowUser"))

  defaultAuthenticate() {
    delete<FollowUserRoute> { params ->
      val self = call.principal<User>()
        ?: return@delete call.respond(
          HttpStatusCode.Unauthorized,
          ErrorEnvelope(mapOf("body" to listOf("Unauthorized")))
        )

      unfollowUser(self.id, Username(params.username))
        .map { ProfileResponse(it) }
        .fold({ handleErrors(it) }, { call.respond(it) })
    }
  }
}

fun Application.registerProfileRoutes() {
  routing {
    route(API_V1) {
      getUserProfile()
      followUser()
      unfollowUser()
    }
  }
}
