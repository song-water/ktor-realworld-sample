package io.skinnydoo.common

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*

fun StatusPagesConfig.configure() {
  exception<Throwable> { call, cause ->
    call.respond(
      HttpStatusCode.InternalServerError,
      ErrorEnvelope(mapOf("body" to listOf(cause.localizedMessage)))
    )
  }
}

suspend fun PipelineContext<Unit, ApplicationCall>.handleErrors(error: LoginErrors) = when (error) {
  LoginErrors.EmailUnknown -> {
    val errorBody = ErrorEnvelope(mapOf("body" to listOf("Unknown email")))
    call.respond(status = HttpStatusCode.Unauthorized, message = errorBody)
  }

  LoginErrors.PasswordInvalid -> {
    val errorBody = ErrorEnvelope(mapOf("body" to listOf("Invalid password")))
    call.respond(status = HttpStatusCode.Unauthorized, message = errorBody)
  }
}

suspend fun PipelineContext<Unit, ApplicationCall>.handleErrors(error: UserErrors) = when (error) {
  is UserErrors.UserAlreadyExist -> {
    val errorBody = ErrorEnvelope(mapOf("body" to listOf(error.message)))
    call.respond(HttpStatusCode.UnprocessableEntity, errorBody)
  }

  is UserErrors.UserNotFound -> {
    val errorBody = ErrorEnvelope(mapOf("body" to listOf(error.message)))
    call.respond(HttpStatusCode.NotFound, errorBody)
  }
}

suspend fun PipelineContext<Unit, ApplicationCall>.handleErrors(error: ArticleErrors) = when (error) {
  is ArticleErrors.ArticleNotFound -> {
    val errorBody = ErrorEnvelope(mapOf("body" to listOf("Article with slug ${error.slug} does not exist")))
    call.respond(HttpStatusCode.NotFound, errorBody)
  }

  ArticleErrors.AuthorNotFound -> call.respond(HttpStatusCode.InternalServerError)
  Forbidden -> call.respond(HttpStatusCode.Unauthorized)
  is ServerError -> {
    val errorBody = ErrorEnvelope(mapOf("body" to listOf(error.message)))
    call.respond(HttpStatusCode.InternalServerError, errorBody)
  }

  is ArticleErrors.CommentNotFound -> {
    val errorBody = ErrorEnvelope(mapOf("body" to listOf("Comment with id ${error.commentId} does not exist")))
    call.respond(HttpStatusCode.NotFound, errorBody)
  }
}

suspend fun PipelineContext<Unit, ApplicationCall>.handleErrors(error: InvalidPropertyError) = when (error) {
  is InvalidPropertyError.SlugInvalid -> {
    val errorBody = ErrorEnvelope(mapOf("body" to listOf(error.message)))
    call.respond(HttpStatusCode.UnprocessableEntity, errorBody)
  }
}

suspend fun PipelineContext<Unit, ApplicationCall>.handleErrors(error: CommonErrors) = when (error) {
  is ServerError -> {
    val errorBody = ErrorEnvelope(mapOf("body" to listOf(error.message)))
    call.respond(HttpStatusCode.InternalServerError, errorBody)
  }

  Forbidden -> call.respond(HttpStatusCode.Unauthorized)
}
