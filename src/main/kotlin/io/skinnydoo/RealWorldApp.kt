package io.skinnydoo

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.skinnydoo.articles.comments.registerCommentRoutes
import io.skinnydoo.articles.registerArticleRoutes
import io.skinnydoo.articles.tags.registerTagsRoutes
import io.skinnydoo.common.JwtService
import io.skinnydoo.common.configure
import io.skinnydoo.common.db.DatabaseFactory
import io.skinnydoo.common.db.dbConfig
import io.skinnydoo.common.jwtConfig
import io.skinnydoo.common.koinModules
import io.skinnydoo.profiles.registerProfileRoutes
import io.skinnydoo.users.GetUserWithId
import io.skinnydoo.users.registerUserRoutes
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.slf4j.event.Level

const val API_V1 = "api/v1"

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
fun Application.module(koinModules: List<Module> = koinModules()) {
  install(Koin) { configure(koinModules) }

  val dbFactory by inject<DatabaseFactory> {
    parametersOf(environment.dbConfig("ktor.database"))
  }
  val json by inject<Json>()
  val jwtService by inject<JwtService> {
    parametersOf(environment.jwtConfig(JwtService.CONFIG_PATH))
  }
  val getUserWithId by inject<GetUserWithId>(named("getUserWithId"))

  dbFactory.connect()

  install(DefaultHeaders) { header("X-Engine", "Ktor") }
  install(CallLogging) {
    level = Level.DEBUG
    filter { call ->
      call.request.path().startsWith("/")
    }
  }
  install(ContentNegotiation) {
    json(json)
  }
  install(StatusPages) {
    configure()
  }
  install(Resources)

  authentication {
    configure(jwtService) { userId ->
      getUserWithId(userId).orNull()
    }
  }

  registerUserRoutes()
  registerProfileRoutes()
  registerArticleRoutes()
  registerCommentRoutes()
  registerTagsRoutes()
}
