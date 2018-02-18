package io.github.crabzilla.vertx.modules

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dagger.Module
import dagger.Provides
import io.github.crabzilla.vertx.ProjectionDatabase
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin
import javax.inject.Singleton

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
  fun jdbi(@ProjectionDatabase dataSource: HikariDataSource): Jdbi {
    val jdbi = Jdbi.create(dataSource)
    jdbi.installPlugin(SqlObjectPlugin())
    jdbi.installPlugin(KotlinPlugin())
    jdbi.installPlugin(KotlinSqlObjectPlugin())
    return jdbi
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
