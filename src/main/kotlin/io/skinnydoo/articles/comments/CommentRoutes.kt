package io.skinnydoo.articles.comments

import arrow.core.Either
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.skinnydoo.API_V1
import io.skinnydoo.articles.ArticleRoute
import io.skinnydoo.common.*
import io.skinnydoo.users.User
import org.koin.core.qualifier.named
import org.koin.ktor.ext.inject
import java.util.*
import kotlin.text.get

fun Route.getCommentsForArticle() {
  val commentsForArticle by inject<GetCommentsForArticleUseCase>(named("commentsForArticle"))

  defaultAuthenticate(optional = true) {
    get<ArticleRoute.Comments> { params ->
      val userId = call.principal<User>()?.id

      Either.catch { Slug(UUID.fromString(params.parent.slug)) }
        .mapLeft { InvalidSlug(it.localizedMessage) }
        .fold({ handleErrors(it) }) { slug ->
          commentsForArticle(slug, userId).map(::CommentsResponse).fold({ handleErrors(it) }, { call.respond(it) })
        }
    }
  }
}

fun Route.addCommentForArticle() {
  val addComments by inject<AddCommentForArticleUseCase>(named("addComments"))

  defaultAuthenticate() {
    post<ArticleRoute.Comments> { params ->
      val userId = call.principal<User>()?.id ?: return@post call.respond(HttpStatusCode.Unauthorized,
        ErrorEnvelope(mapOf("body" to listOf("Unauthorized"))))

      val body = call.receive<CreateCommentRequest>()

      Either.catch { Slug(UUID.fromString(params.parent.slug)) }
        .mapLeft { InvalidSlug(it.localizedMessage) }
        .fold({ handleErrors(it) }) { slug ->
          addComments(slug, userId, body.comment).map { CommentResponse(it) }
            .fold({ handleErrors(it) }, { call.respond(HttpStatusCode.Created, it) })
        }
    }
  }
}

fun Route.removeCommentForArticle() {
  val removeComments by inject<RemoveCommentFromArticleUseCase>(named("removeComments"))

  defaultAuthenticate() {
    delete<ArticleRoute.Comments.Comment> { params ->
      val userId = call.principal<User>()?.id ?: return@delete call.respond(HttpStatusCode.Unauthorized,
        ErrorEnvelope(mapOf("body" to listOf("Unauthorized"))))

      Either.catch { Slug(UUID.fromString(params.parent.parent.slug)) }
        .mapLeft { InvalidSlug(it.localizedMessage) }
        .fold({ handleErrors(it) }) { slug ->
          removeComments(slug, userId, CommentId(params.id)).fold({ handleErrors(it) },
            { call.respond(HttpStatusCode.NoContent) })
        }
    }
  }
}

fun Application.registerCommentRoutes() {
  routing {
    route(API_V1) {
      getCommentsForArticle()
      addCommentForArticle()
      removeCommentForArticle()
    }
  }
}
