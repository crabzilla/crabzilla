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
import io.github.crabzilla.vertx.entity.EntityCommandExecution
import io.github.crabzilla.vertx.projection.ProjectionData
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.spi.VerticleFactory

private val log = org.slf4j.LoggerFactory.getLogger("CrabzillaVertx")

fun configHandler(vertx: Vertx, handler: (JsonObject) -> Unit, shutdownHook: () -> Unit) {

  val envOptions = ConfigStoreOptions().setType("env")
  val retrieverOptions = ConfigRetrieverOptions().addStore(envOptions)
  val retriever = ConfigRetriever.create(vertx, retrieverOptions)

  retriever.getConfig { ar ->

    if (ar.failed()) {
      log.error("failed to load configuration", ar.cause())
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

fun deployVerticles(vertx: Vertx, verticles: Set<CrabzillaVerticle>, deploymentOptions: DeploymentOptions = DeploymentOptions()) {
 verticles.forEach({
   vertx.deployVerticle(it, deploymentOptions) { event ->
      if (!event.succeeded()) {
        log.error("Error deploying verticle ${it.name}", event.cause())
      } else {
        log.info("Verticle: ${it.name} deployed with ID: ${event.result()}", event.cause())
      }
    }
 })
}

fun deployVerticlesByName(vertx: Vertx, verticles: Set<String>, deploymentOptions: DeploymentOptions = DeploymentOptions()) {
  verticles.forEach({
    vertx.deployVerticle(it, deploymentOptions) { event ->
      if (!event.succeeded()) {
        log.error("Error deploying verticle ${it}", event.cause())
      } else {
        log.info("Verticle $it deployed with ID: ${event.result()}", event.cause())
      }
  }
  })
}

abstract class CrabzillaVerticle(open val name: String, val role: VerticleRole) : AbstractVerticle()

class CrabzillaVerticleFactory(verticles: Set<CrabzillaVerticle>, val role: VerticleRole) : VerticleFactory {

  private val map = verticles.associateBy({it.name}, {it})

  override fun prefix(): String {
    return role.prefix()
  }

  @Throws(Exception::class)
  override fun createVerticle(name: String, classLoader: ClassLoader): Verticle? {
    return map[name.removePrefix(prefix() + ":")]
  }

}

enum class VerticleRole {

  REST, HANDLER, PROJECTOR, POOLER ;

  fun verticle(verticleName: String): String {
    return "${prefix()}.toLowerCase():$verticleName"
  }

  fun prefix(): String {
    return this.name.toLowerCase()
  }

}

class DbConcurrencyException(s: String) : RuntimeException(s)
