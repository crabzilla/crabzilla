package io.github.crabzilla.vertx

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dagger.Module
import dagger.Provides
import io.github.crabzilla.core.DomainEvent
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import java.util.*
import javax.inject.Singleton

interface EventsProjector<DAO> {

  val eventsChannelId: String
  val daoClass: Class<DAO>

  fun handle(uowList: List<ProjectionData>)

  fun write(dao: DAO, targetId: String, event: DomainEvent)

}

data class ProjectionData(val uowId: UUID, val uowSequence: Long,
                          val targetId: String, val events: List<DomainEvent>)

@Module
class ProjectionDbModule {

  @Provides
  @Singleton
  @ProjectionDatabase
  fun jdbcClient(@ProjectionDatabase dataSource: HikariDataSource, vertx: Vertx): JDBCClient {
    return JDBCClient.create(vertx, dataSource)
  }

  @Provides
  @Singleton
  @ProjectionDatabase
  fun hikariDs(config: JsonObject): HikariDataSource {
    val hikariConfig = HikariConfig()
    hikariConfig.driverClassName = config.getString("READ_DATABASE_DRIVER")
    hikariConfig.jdbcUrl = config.getString("READ_DATABASE_URL")
    hikariConfig.username = config.getString("READ_DATABASE_USER")
    hikariConfig.password = config.getString("READ_DATABASE_PASSWORD")
    hikariConfig.connectionTimeout = 5000
    hikariConfig.maximumPoolSize = config.getInteger("READ_DATABASE_POOL_MAX_SIZE")!!
    hikariConfig.addDataSourceProperty("cachePrepStmts", "true")
    hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250")
    hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
    hikariConfig.isAutoCommit = false
    hikariConfig.isReadOnly = false
    return HikariDataSource(hikariConfig)
  }

}
