package io.github.crabzilla.vertx

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import io.github.crabzilla.Command
import io.github.crabzilla.DomainEvent
import io.github.crabzilla.EntityId
import io.github.crabzilla.UnitOfWork
import io.github.crabzilla.vertx.helpers.JacksonGenericCodec
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.AbstractVerticle
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.spi.VerticleFactory
import io.vertx.kotlin.config.ConfigRetrieverOptions

private val log = org.slf4j.LoggerFactory.getLogger("CrabzillaVertx")

fun initVertx(vertx: Vertx) {

  Json.mapper.registerModule(ParameterNamesModule())
    .registerModule(Jdk8Module())
    .registerModule(JavaTimeModule())
    .registerModule(KotlinModule())
    .enable(SerializationFeature.INDENT_OUTPUT)

  //    Json.mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);

  vertx.eventBus().registerDefaultCodec(ProjectionData::class.java,
    JacksonGenericCodec(Json.mapper, ProjectionData::class.java))

  vertx.eventBus().registerDefaultCodec(CommandExecution::class.java,
    JacksonGenericCodec(Json.mapper, CommandExecution::class.java))

  vertx.eventBus().registerDefaultCodec(EntityId::class.java,
    JacksonGenericCodec(Json.mapper, EntityId::class.java))

  vertx.eventBus().registerDefaultCodec(Command::class.java,
    JacksonGenericCodec(Json.mapper, Command::class.java))

  vertx.eventBus().registerDefaultCodec(DomainEvent::class.java,
    JacksonGenericCodec(Json.mapper, DomainEvent::class.java))

  vertx.eventBus().registerDefaultCodec(UnitOfWork::class.java,
    JacksonGenericCodec(Json.mapper, UnitOfWork::class.java))

}

enum class VerticleRole {

  REST, HANDLER, PROJECTOR, POOLER ;

  fun verticle(verticleName: String): String {
    return "${prefix()}:${verticleName}"
  }

  fun prefix(): String {
    return this.name.toLowerCase()
  }

}

abstract class CrabzillaVerticle(open val name: String, open val role: VerticleRole) : AbstractVerticle()

class CrabzillaVerticleFactory(verticles: Set<CrabzillaVerticle>, private val role: VerticleRole) : VerticleFactory {

  private val map = verticles.associateBy({it.name}, {it})

  override fun prefix(): String {
    return role.prefix()
  }

  @Throws(Exception::class)
  override fun createVerticle(name: String, classLoader: ClassLoader): Verticle? {
    return map[name.removePrefix(prefix() + ":")]
  }

}


fun configHandler(vertx: Vertx, envOptions: ConfigStoreOptions, handler: (JsonObject) -> Unit, shutdownHook: () -> Unit) {

  val retrieverOptions = ConfigRetrieverOptions().addStore(envOptions)
  val retriever = ConfigRetriever.create(vertx, retrieverOptions)

  retriever.getConfig { ar ->

    if (ar.failed()) {
      log.error("failed to load configuration", ar.cause())
      return@getConfig
    }

    val config = ar.result()

    log.info("config = {}", config.encodePrettily())

    Runtime.getRuntime().addShutdownHook(object : Thread() {
      override fun run() {
        shutdownHook.invoke()
        vertx.close()
      }
    })

    handler.invoke(config)

  }

}
