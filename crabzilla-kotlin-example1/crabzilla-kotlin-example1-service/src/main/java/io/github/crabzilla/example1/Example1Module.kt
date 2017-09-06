package io.github.crabzilla.example1

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.name.Names
import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.entity.EntityCommand
import io.github.crabzilla.core.entity.EntityId
import io.github.crabzilla.core.entity.EntityUnitOfWork
import io.github.crabzilla.example1.customer.CustomerModule
import io.github.crabzilla.example1.services.SampleInternalServiceImpl
import io.github.crabzilla.vertx.codecs.JacksonGenericCodec
import io.github.crabzilla.vertx.entity.EntityCommandExecution
import io.github.crabzilla.vertx.projection.EventProjector
import io.github.crabzilla.vertx.projection.EventsProjectionVerticle
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
import javax.inject.Named

internal class Example1Module(val vertx: Vertx) : AbstractModule() {

  override fun configure() {
    configureVertx()
    // aggregates
    install(CustomerModule())
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
  fun eventsProjectorVerticle(eventProjector: EventProjector<CustomerSummaryDao>):
          EventsProjectionVerticle<CustomerSummaryDao> {
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

  @Provides
  @Singleton
  fun config(@Named("database.driver") dbDriver: String,
             @Named("database.url") dbUrl: String,
             @Named("database.user") dbUser: String,
             @Named("database.password") dbPwd: String,
             @Named("database.pool.max.size") databaseMaxSize: Int?): HikariDataSource {

    val config = HikariConfig()
    config.driverClassName = dbDriver
    config.jdbcUrl = dbUrl
    config.username = dbUser
    config.password = dbPwd
    config.connectionTimeout = 5000
    config.maximumPoolSize = databaseMaxSize!!
    config.addDataSourceProperty("cachePrepStmts", "true")
    config.addDataSourceProperty("prepStmtCacheSize", "250")
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
    config.isAutoCommit = false
    // config.setTransactionIsolation("TRANSACTION_REPEATABLE_READ");
    config.transactionIsolation = "TRANSACTION_SERIALIZABLE"
    return HikariDataSource(config)
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

    vertx.eventBus().registerDefaultCodec(EntityCommandExecution::class.java,
            JacksonGenericCodec(mapper, EntityCommandExecution::class.java))

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