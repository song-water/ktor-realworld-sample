package io.skinnydoo.articles.tags

import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.skinnydoo.API_V1
import io.skinnydoo.common.handleErrors
import org.koin.core.qualifier.named
import org.koin.ktor.ext.inject

@Resource("/tags")
class TagsRoute

fun Route.getTags() {
  val allTags by inject<GetTags>(named("tags"))

  get<TagsRoute> {
    allTags().map(::TagsResponse).fold({ handleErrors(it) }, { call.respond(it) })
  }
}

fun Application.registerTagsRoutes() {
  routing {
    route(API_V1) {
      getTags()
    }
  }
}
