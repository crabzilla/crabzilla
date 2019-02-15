package io.github.crabzilla.vertx

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import io.github.crabzilla.*
import io.vertx.core.AbstractVerticle
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.spi.VerticleFactory

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
    return "${prefix()}:$verticleName"
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
