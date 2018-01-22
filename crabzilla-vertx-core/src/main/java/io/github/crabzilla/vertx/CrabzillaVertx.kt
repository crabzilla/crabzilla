package io.github.crabzilla.vertx

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.entity.EntityCommand
import io.github.crabzilla.core.entity.EntityId
import io.github.crabzilla.core.entity.EntityUnitOfWork
import io.github.crabzilla.vertx.codecs.JacksonGenericCodec
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.DeploymentOptions
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import java.io.File

val log = org.slf4j.LoggerFactory.getLogger("CrabzillaVertx")

fun configHandler(vertx: Vertx, configFile: String?, defaultConfigFile: String?,
                  handler: (JsonObject) -> Unit, shutdownHook: () -> Unit) {

  val retriever = ConfigRetriever.create(vertx, cfgOptions(configFile, defaultConfigFile))

  retriever.getConfig { ar ->

    if (ar.failed()) {
      log.error("failed to load config file ${configFile}", ar.cause())
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

private fun cfgOptions(configFile: String?, defaultConfigFile: String?): ConfigRetrieverOptions {

  if (configFile != null && !configFile.isEmpty()
          && File(configFile).exists()) {

    val file = ConfigStoreOptions()
            .setType("file")
            .setFormat("properties")
            .setConfig(JsonObject().put("path", configFile))

    log.info("Using config {}", configFile)

    return ConfigRetrieverOptions().addStore(file)
  }

  val file = ConfigStoreOptions()
          .setType("file")
          .setFormat("properties")
          .setConfig(JsonObject().put("path", defaultConfigFile))

  log.info("Using config {}", defaultConfigFile)

  return ConfigRetrieverOptions().addStore(file)

}

fun initVertx(vertx: Vertx) {

  Json.mapper.registerModule(ParameterNamesModule())
          .registerModule(Jdk8Module())
          .registerModule(JavaTimeModule())
          .registerModule(KotlinModule())
          .enable(SerializationFeature.INDENT_OUTPUT)

  //    Json.mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);

  vertx.eventBus().registerDefaultCodec(ProjectionData::class.java,
          JacksonGenericCodec(Json.mapper, ProjectionData::class.java))

  vertx.eventBus().registerDefaultCodec(EntityCommandExecution::class.java,
          JacksonGenericCodec(Json.mapper, EntityCommandExecution::class.java))

  vertx.eventBus().registerDefaultCodec(EntityId::class.java,
          JacksonGenericCodec(Json.mapper, EntityId::class.java))

  vertx.eventBus().registerDefaultCodec(EntityCommand::class.java,
          JacksonGenericCodec(Json.mapper, EntityCommand::class.java))

  vertx.eventBus().registerDefaultCodec(DomainEvent::class.java,
          JacksonGenericCodec(Json.mapper, DomainEvent::class.java))

  vertx.eventBus().registerDefaultCodec(EntityUnitOfWork::class.java,
          JacksonGenericCodec(Json.mapper, EntityUnitOfWork::class.java))

}

fun deployVerticles(vertx: Vertx, verticles: Set<Verticle>, deploymentOptions: DeploymentOptions = DeploymentOptions()) {
 verticles.forEach({
   vertx.deployVerticle(it, deploymentOptions) { event ->
      if (!event.succeeded()) log.error("Error deploying verticle ${it}", event.cause())
    }
 })
}

fun deployVerticlesByName(vertx: Vertx, verticles: Set<String>, deploymentOptions: DeploymentOptions = DeploymentOptions()) {
  verticles.forEach({
    vertx.deployVerticle(it, deploymentOptions) { event ->
      if (!event.succeeded()) log.error("Error deploying verticle ${it}", event.cause())
    }
  })
}