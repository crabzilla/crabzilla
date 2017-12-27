package io.github.crabzilla.example1

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import dagger.Module
import dagger.Provides
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.entity.EntityCommand
import io.github.crabzilla.core.entity.EntityId
import io.github.crabzilla.core.entity.EntityUnitOfWork
import io.github.crabzilla.example1.repositories.CustomerRepositoryImpl
import io.github.crabzilla.example1.repositories.CustomerSummaryDao
import io.github.crabzilla.example1.services.SampleInternalServiceImpl
import io.github.crabzilla.vertx.codecs.JacksonGenericCodec
import io.github.crabzilla.vertx.entity.EntityCommandExecution
import io.github.crabzilla.vertx.modules.qualifiers.QueryDatabase
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import org.jdbi.v3.core.Jdbi
import javax.inject.Singleton

@Module
class Example1Module(val vertx: Vertx, val config: JsonObject) {

  init {
    configureVertx()
  }

  @Provides
  @Singleton
  fun vertx(): Vertx {
    return vertx
  }

  @Provides
  @Singleton
  fun config(): JsonObject {
    return config
  }

  @Provides
  @Singleton
  fun customerSummaryDao(@QueryDatabase jdbi: Jdbi) : CustomerSummaryDao {
    return jdbi.onDemand(CustomerSummaryDao::class.java)
  }

  @Provides
  @Singleton
  fun customerRepository(dao: CustomerSummaryDao): CustomerRepository {
    return CustomerRepositoryImpl(dao)
  }

  @Provides
  @Singleton
  fun service(): SampleInternalService {
    return SampleInternalServiceImpl()
  }

  fun configureVertx() {

    Json.mapper.registerModule(ParameterNamesModule())
            .registerModule(Jdk8Module())
            .registerModule(JavaTimeModule())
            .registerModule(KotlinModule())
            .enable(SerializationFeature.INDENT_OUTPUT)

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

}
