package io.github.crabzilla.example1

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dagger.Provides
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.entity.EntityCommand
import io.github.crabzilla.core.entity.EntityId
import io.github.crabzilla.core.entity.EntityUnitOfWork
import io.github.crabzilla.vertx.codecs.JacksonGenericCodec
import io.github.crabzilla.vertx.entity.EntityCommandExecution
import io.vertx.core.Vertx
import io.vertx.core.json.Json.mapper
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin
import javax.inject.Singleton

//@Module
class CrabzillaModule {

  // @Provides
  @Singleton
  fun vertx(): Vertx {
    return Vertx.vertx()
  }

  // @Provides
  @Singleton
  fun jdbcClient(vertx: Vertx, dataSource: HikariDataSource): JDBCClient {
    return JDBCClient.create(vertx, dataSource)
  }

  // @Provides
  @Singleton
  fun jdbi(dataSource: HikariDataSource): Jdbi {
    val jdbi = Jdbi.create(dataSource)
    jdbi.installPlugin(SqlObjectPlugin())
    jdbi.installPlugin(KotlinPlugin())
    jdbi.installPlugin(KotlinSqlObjectPlugin())
    return jdbi
  }

  // @Provides
  @Singleton
  fun hikariDs(config: JsonObject): HikariDataSource {

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


  fun configureVertx(vertx: Vertx) {

    mapper.registerModule(ParameterNamesModule())
            .registerModule(Jdk8Module())
            .registerModule(JavaTimeModule())
            .registerModule(KotlinModule())
            .enable(SerializationFeature.INDENT_OUTPUT)

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
