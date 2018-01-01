package io.github.crabzilla.vertx

import io.github.crabzilla.vertx.modules.CrabzillaModule
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import java.io.File


val log = org.slf4j.LoggerFactory.getLogger("CrabzillaVertx")

fun configHandler(vertx: Vertx, configFile: String?,
                  handler: (JsonObject) -> Unit, shutdownHook: () -> Unit) {

  val retriever = ConfigRetriever.create(vertx, cfgOptions(configFile))

  retriever.getConfig { ar ->

    if (ar.failed()) {
      log.error("failed to load config", ar.cause())
      return@getConfig
    }

    val config = ar.result()

    Runtime.getRuntime().addShutdownHook(object : Thread() {
      override fun run() {
        shutdownHook.invoke()
        vertx.close()
      }
    })

    handler.invoke(config)

  }

}

private fun cfgOptions(configFile: String?): ConfigRetrieverOptions {

  if (configFile != null && !configFile.isEmpty()
          && File(configFile).exists()) {

    val file = ConfigStoreOptions()
            .setType("file")
            .setFormat("properties")
            .setConfig(JsonObject().put("path", configFile))

    log.info("Using config {}", configFile)

    return ConfigRetrieverOptions().addStore(file)
  }

  val defaultConfigFile = CrabzillaModule::class.java.classLoader
          .getResource("conf/config.properties")!!.path

  val file = ConfigStoreOptions()
          .setType("file")
          .setFormat("properties")
          .setConfig(JsonObject().put("path", defaultConfigFile))

  log.info("Using config {}", defaultConfigFile)

  return ConfigRetrieverOptions().addStore(file)

}