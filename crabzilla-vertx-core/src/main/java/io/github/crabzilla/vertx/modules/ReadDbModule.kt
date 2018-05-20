package io.github.crabzilla.vertx.modules

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dagger.Module
import dagger.Provides
import io.github.crabzilla.vertx.modules.qualifiers.ReadDatabase
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import javax.inject.Singleton

@Module
class ReadDbModule {

  private val log = org.slf4j.LoggerFactory.getLogger(ReadDbModule::class.java)

  @Provides
  @Singleton
  @ReadDatabase
  fun jdbcClient(@ReadDatabase dataSource: HikariDataSource, vertx: Vertx): JDBCClient {
    return JDBCClient.create(vertx, dataSource)
  }

  @Provides
  @Singleton
  @ReadDatabase
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
    hikariConfig.isAutoCommit = true
    hikariConfig.isReadOnly =  true
    return HikariDataSource(hikariConfig)
  }

}
