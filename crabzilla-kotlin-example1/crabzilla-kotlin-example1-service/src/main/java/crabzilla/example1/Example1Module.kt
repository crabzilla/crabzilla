package crabzilla.example1

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.name.Names
import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariDataSource
import crabzilla.example1.customer.CustomerModule
import crabzilla.example1.services.SampleInternalServiceImpl
import crabzilla.model.*
import crabzilla.stack.CommandExecution
import crabzilla.stack.EventProjector
import crabzilla.vertx.codecs.JacksonGenericCodec
import crabzilla.vertx.verticles.EventsProjectionVerticle
import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.circuitbreaker.CircuitBreakerOptions
import io.vertx.core.Vertx
import io.vertx.core.json.Json.mapper
import io.vertx.ext.jdbc.JDBCClient
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin
import java.util.*

internal class Example1Module(val vertx: Vertx) : AbstractModule() {

  override fun configure() {

    configureVertx()

    // aggregates
    install(CustomerModule())

    // database
    install(DatabaseModule())

    // services
    bind(SampleInternalService::class.java).to(SampleInternalServiceImpl::class.java).asEagerSingleton()

    // exposes properties to guice
    setCfgProps()

  }

  private fun setCfgProps() {

    val config = ConfigFactory.load()
    val props = Properties()

    config.entrySet().forEach { e ->
      val key = e.key.replace("example1.", "")
      val value = e.value.render().replace("\"", "")
      props.put(key, value)
    }

    Names.bindProperties(binder(), props)
  }

  @Provides
  @Singleton
  fun vertx(): Vertx {
    return vertx
  }

  @Provides
  @Singleton
  fun eventsProjector(jdbi: Jdbi): EventProjector<CustomerSummaryDao> {
    return Example1EventProjector("example1", CustomerSummaryDao::class.java, jdbi)
  }

  @Provides
  @Singleton
  fun eventsProjectorVerticle(eventProjector: EventProjector<CustomerSummaryDao>): EventsProjectionVerticle<CustomerSummaryDao> {
    val circuitBreaker = CircuitBreaker.create("events-projection-circuit-breaker", vertx,
            CircuitBreakerOptions()
                    .setMaxFailures(5) // number SUCCESS failure before opening the circuit
                    .setTimeout(2000) // consider a failure if the operation does not succeed in time
                    .setFallbackOnFailure(true) // do we call the fallback on failure
                    .setResetTimeout(10000))  // time spent in open state before attempting to re-try
    return EventsProjectionVerticle(eventProjector, circuitBreaker)
  }

  @Provides
  @Singleton
  fun jdbi(ds: HikariDataSource): Jdbi {
    val jdbi = Jdbi.create(ds)
    jdbi.installPlugin(SqlObjectPlugin())
    jdbi.installPlugin(KotlinPlugin())
    jdbi.installPlugin(KotlinSqlObjectPlugin())
    return jdbi
  }

  @Provides
  @Singleton
  fun jdbcClient(vertx: Vertx, dataSource: HikariDataSource): JDBCClient {
    return JDBCClient.create(vertx, dataSource)
  }


  //  Not being used yet. This can improve a lot serialization speed (it's binary).
  //  But so far it was not necessary.
  //  @Provides
  //  @Singleton
  //  FSTConfiguration conf() {
  //    return FSTConfiguration.createDefaultConfiguration();
  //  }

  fun configureVertx() {

    mapper.registerModule(ParameterNamesModule())
            .registerModule(Jdk8Module())
            .registerModule(JavaTimeModule())
            .registerModule(KotlinModule())

    vertx.eventBus().registerDefaultCodec(CommandExecution::class.java,
            JacksonGenericCodec(mapper, CommandExecution::class.java))

    vertx.eventBus().registerDefaultCodec(EntityId::class.java,
            JacksonGenericCodec(mapper, EntityId::class.java))

    vertx.eventBus().registerDefaultCodec(Command::class.java,
            JacksonGenericCodec(mapper, EntityCommand::class.java))

    vertx.eventBus().registerDefaultCodec(DomainEvent::class.java,
            JacksonGenericCodec(mapper, DomainEvent::class.java))

    vertx.eventBus().registerDefaultCodec(EntityUnitOfWork::class.java,
            JacksonGenericCodec(mapper, EntityUnitOfWork::class.java))

  }

}
