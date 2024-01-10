package io.skinnydoo.articles

import arrow.core.Either
import arrow.core.getOrElse
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.skinnydoo.API_V1
import io.skinnydoo.articles.tags.Tag
import io.skinnydoo.common.*
import io.skinnydoo.users.User
import org.koin.core.qualifier.named
import org.koin.ktor.ext.inject
import java.util.*

const val ARTICLES = "/articles"

@Resource(ARTICLES)
data class ArticlesRoute(
  val tag: String = "",
  val author: String = "",
  val favorited: String = "",
  val limit: Int = 20,
  val offset: Int = 0,
)

@Resource("$ARTICLES/feed")
data class ArticleFeedRoute(val limit: Int = 20, val offset: Int = 0)

@Resource("$ARTICLES/{slug}")
data class ArticleRoute(val slug: String) {

  @Resource("/comments")
  data class Comments(val parent: ArticleRoute) {

    @Resource("/{id}")
    data class Comment(val parent: Comments, val id: Int)
  }

  @Resource("/favorite")
  data class Favorite(val parent: ArticleRoute)
}

/**
 * Get most recent articles globally. Auth is optional.
 */
fun Route.allArticles() {
  val getAllArticles by inject<GetAllArticlesUseCase>(named("allArticles"))

  defaultAuthenticate(optional = true) {
    get<ArticlesRoute> { params ->
      val selfId = call.principal<User>()?.id

      val tag = params.tag.ifEmpty { null }?.let(::Tag)
      val favoritedBy = params.favorited.ifEmpty { null }?.let(::Username)
      val username = params.author.ifEmpty { null }?.let(::Username)
      val limit = Limit.fromInt(params.limit).getOrElse { Limit.default }
      val offset = Offset.fromInt(params.offset).getOrElse { Offset.default }

      getAllArticles(selfId, tag, username, favoritedBy, limit, offset)
        .map { ArticleListResponse(it) }
        .fold({ handleErrors(it) }, { call.respond(it) })
    }
  }
}

/**
 * Get most recent articles from users you follow. Auth is required
 */
fun Route.articleFeed() {
  val feedArticles by inject<GetFeedArticlesUseCase>(named("feed"))

  defaultAuthenticate() {
    get<ArticleFeedRoute> { params ->
      val self = call.principal<User>()
        ?: return@get call.respond(HttpStatusCode.Unauthorized, ErrorEnvelope(mapOf("body" to listOf("Unauthorized"))))

      val limit = Limit.fromInt(params.limit).getOrElse { Limit.default }
      val offset = Offset.fromInt(params.offset).getOrElse { Offset.default }
      feedArticles(self.id, limit, offset)
        .map { ArticleListResponse(it) }
        .fold({ handleErrors(it) }, { call.respond(it) })
    }
  }
}

/**
 * Create an article. Auth is required
 */
fun Route.createArticle() {
  val addArticle by inject<AddArticleUseCase>(named("addArticle"))

  defaultAuthenticate() {
    post<ArticlesRoute> {
      val body = call.receive<CreateArticleRequest>().article

      val self = call.principal<User>()
        ?: return@post call.respond(
          HttpStatusCode.Unauthorized,
          ErrorEnvelope(mapOf("body" to listOf("Unauthorized")))
        )

      addArticle(body, self.id)
        .map { ArticleResponse(it) }
        .fold({ handleErrors(it) }) { call.respond(HttpStatusCode.Created, it) }
    }
  }
}

/**
 * Get an article. Auth is optional
 */
fun Route.getArticleWithSlug() {
  val getArticleWithSlug by inject<GetArticleWithSlugUseCase>(named("getArticle"))

  defaultAuthenticate(optional = true) {
    get<ArticleRoute> { params ->
      val userId = call.principal<User>()?.id

      Either.catch { UUID.fromString(params.slug) }
        .mapLeft { InvalidSlug(it.localizedMessage) }
        .map(::Slug)
        .fold({ handleErrors(it) }) { s ->
          getArticleWithSlug(s, userId)
            .map { ArticleResponse(it) }
            .fold({ handleErrors(it) }) { call.respond(it) }
        }
    }
  }
}

/**
 * Update an article. Auth is required
 */
fun Route.updateArticle() {
  val updateArticle by inject<UpdateArticleUseCase>(named("updateArticle"))

  defaultAuthenticate() {
    put<ArticleRoute> { params ->
      val self = call.principal<User>()
        ?: return@put call.respond(HttpStatusCode.Unauthorized, ErrorEnvelope(mapOf("body" to listOf("Unauthorized"))))

      val body = call.receive<UpdateArticleRequest>().article

      Slug.fromString(params.slug)
        .toEither { InvalidSlug() }
        .fold({ handleErrors(it) }) { slug ->
          updateArticle(slug, body, self.id).map { ArticleResponse(it) }
            .fold({ handleErrors(it) }, { call.respond(it) })
        }
    }
  }
}

/**
 * Delete an article. Auth is required
 */
fun Route.deleteArticle() {
  val deleteArticleWithSlug by inject<DeleteArticleUseCase>(named("deleteArticle"))

  defaultAuthenticate() {
    delete<ArticleRoute> { params ->
      val user = call.principal<User>()
        ?: return@delete call.respond(
          status = HttpStatusCode.Unauthorized,
          message = ErrorEnvelope(mapOf("body" to listOf("Unauthorized")))
        )

      Slug.fromString(params.slug)
        .toEither { InvalidSlug() }
        .fold({ handleErrors(it) }) { slug ->
          deleteArticleWithSlug(slug, user.id)
            .fold({ handleErrors(it) }, { call.respond(HttpStatusCode.NoContent) })
        }
    }
  }
}

fun Route.favoriteArticle() {
  val favorArticle by inject<FavorArticleUseCase>(named("favorArticle"))

  defaultAuthenticate() {
    post<ArticleRoute.Favorite> { params ->
      val user = call.principal<User>()
        ?: return@post call.respond(
          status = HttpStatusCode.Unauthorized,
          message = ErrorEnvelope(mapOf("body" to listOf("Unauthorized")))
        )

      Slug.fromString(params.parent.slug)
        .toEither { InvalidSlug() }
        .fold({ handleErrors(it) }) { slug ->
          favorArticle(slug, user.id).map { ArticleResponse(it) }
            .fold({ handleErrors(it) }, { call.respond(it) })
        }
    }
  }
}

fun Route.unFavoriteArticle() {
  val unFavorArticle by inject<UnFavorArticleUseCase>(named("unFavorArticle"))

  defaultAuthenticate() {
    delete<ArticleRoute.Favorite> { params ->
      val user = call.principal<User>()
        ?: return@delete call.respond(
          status = HttpStatusCode.Unauthorized,
          message = ErrorEnvelope(mapOf("body" to listOf("Unauthorized")))
        )

      Slug.fromString(params.parent.slug)
        .toEither { InvalidSlug() }
        .fold({ handleErrors(it) }) { slug ->
          unFavorArticle(slug, user.id).map { ArticleResponse(it) }
            .fold({ handleErrors(it) }, { call.respond(it) })
        }
    }
  }
}

fun Application.registerArticleRoutes() {
  routing {
    route(API_V1) {
      createArticle()
      getArticleWithSlug()
      updateArticle()
      deleteArticle()
      allArticles()
      articleFeed()
      favoriteArticle()
      unFavoriteArticle()
    }
  }
}
