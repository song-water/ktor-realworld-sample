package io.skinnydoo.common.logging

import mu.KotlinLogging
import org.koin.core.logger.KOIN_TAG
import org.koin.core.logger.Level
import org.koin.core.logger.Logger
import org.koin.core.logger.MESSAGE

private val logger = KotlinLogging.logger(KOIN_TAG)

class KotlinLoggingKoinLogger(level: Level = Level.INFO) : Logger(level) {
  private fun logOnLevel(msg: MESSAGE) {
    when (this.level) {
      Level.DEBUG -> logger.debug { msg }
      Level.INFO -> logger.info { msg }
      Level.ERROR -> logger.error { msg }
      else -> logger.error { msg }
    }
  }

  override fun display(level: Level, msg: MESSAGE) {
    if (this.level <= level) logOnLevel(msg)
  }
}
