package io.github.crabzilla.example1

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.TypeLiteral
import com.google.inject.multibindings.MapBinder
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.entity.EntityCommand
import io.github.crabzilla.core.entity.EntityId
import io.github.crabzilla.core.entity.EntityUnitOfWork
import io.github.crabzilla.example1.services.SampleInternalServiceImpl
import io.github.crabzilla.vertx.codecs.JacksonGenericCodec
import io.github.crabzilla.vertx.entity.EntityCommandExecution
import io.github.crabzilla.vertx.projection.EventProjector
import io.github.crabzilla.vertx.projection.EventsProjectionVerticle
import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.circuitbreaker.CircuitBreakerOptions
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.json.Json.mapper
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin
import javax.inject.Singleton

class Example1Module(private val vertx: Vertx, private val config: JsonObject) : AbstractModule() {

  override fun configure() {

    configureVertx()

    // services
    bind(SampleInternalService::class.java).to(SampleInternalServiceImpl::class.java).asEagerSingleton()

    // event projection verticles
    val mapbinder = MapBinder.newMapBinder(binder(), String::class.java, Verticle::class.java)

    val type = object : TypeLiteral<EventsProjectionVerticle<CustomerSummaryDao>>() {

    }

    mapbinder.addBinding("example1.events.projector").to(type)

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
  fun jdbi(dataSource: HikariDataSource): Jdbi {
    val jdbi = Jdbi.create(dataSource)
    jdbi.installPlugin(SqlObjectPlugin())
    jdbi.installPlugin(KotlinPlugin())
    jdbi.installPlugin(KotlinSqlObjectPlugin())
    return jdbi
  }


  @Provides
  @Singleton
  fun eventsProjector(jdbi: Jdbi): EventProjector<CustomerSummaryDao> {
    return Example1EventProjector("example1", CustomerSummaryDao::class.java, jdbi)
  }

  @Provides
  @Singleton
  fun eventsProjectorVerticle(jdbi: Jdbi,
                              eventsProjector: EventProjector<CustomerSummaryDao>): EventsProjectionVerticle<CustomerSummaryDao> {
    val circuitBreaker = CircuitBreaker.create("events-projection-circuit-breaker", vertx,
            CircuitBreakerOptions()
                    .setMaxFailures(5) // number SUCCESS failure before opening the circuit
                    .setTimeout(2000) // consider a failure if the operation does not succeed in time
                    .setFallbackOnFailure(true) // do we call the fallback on failure
                    .setResetTimeout(10000) // time spent in open state before attempting to re-try
    )
    return EventsProjectionVerticle(eventsProjector, circuitBreaker)
  }

  @Provides
  @Singleton
  fun jdbcClient(vertx: Vertx, dataSource: HikariDataSource): JDBCClient {
    return JDBCClient.create(vertx, dataSource)
  }

  @Provides
  @Singleton
  fun hikariDs(): HikariDataSource {

    val hikariConfig = HikariConfig()
    hikariConfig.driverClassName = config.getString("database.driver")
    hikariConfig.jdbcUrl = config.getString("database.url")
    hikariConfig.username = config.getString("database.user")
    hikariConfig.password = config.getString("database.password")
    hikariConfig.connectionTimeout = 5000
    hikariConfig.maximumPoolSize = config.getInteger("database.pool.max.size")!!
    hikariConfig.addDataSourceProperty("cachePrepStmts", "true")
    hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250")
    hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
    hikariConfig.isAutoCommit = false
    // config.setTransactionIsolation("TRANSACTION_REPEATABLE_READ");
    hikariConfig.transactionIsolation = "TRANSACTION_SERIALIZABLE"
    return HikariDataSource(hikariConfig)
  }


  fun configureVertx() {

    mapper.registerModule(ParameterNamesModule())
            .registerModule(Jdk8Module())
            .registerModule(JavaTimeModule())
            .registerModule(KotlinModule())

    vertx.eventBus().registerDefaultCodec(EntityCommandExecution::class.java,
            JacksonGenericCodec(mapper, EntityCommandExecution::class.java))

    vertx.eventBus().registerDefaultCodec(EntityId::class.java,
            JacksonGenericCodec(mapper, EntityId::class.java))

    vertx.eventBus().registerDefaultCodec(EntityCommand::class.java,
            JacksonGenericCodec(mapper, EntityCommand::class.java))

    vertx.eventBus().registerDefaultCodec(DomainEvent::class.java,
            JacksonGenericCodec(mapper, DomainEvent::class.java))

    vertx.eventBus().registerDefaultCodec(EntityUnitOfWork::class.java,
            JacksonGenericCodec(mapper, EntityUnitOfWork::class.java))

  }

}
