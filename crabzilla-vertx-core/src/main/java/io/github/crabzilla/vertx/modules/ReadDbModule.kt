package io.github.crabzilla.vertx.modules

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dagger.Module
import dagger.Provides
import io.github.crabzilla.vertx.qualifiers.ReadDatabase
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin
import javax.inject.Singleton

@Module
class ReadDbModule {

  @Provides
  @Singleton
  @ReadDatabase
  fun jdbcClient(@ReadDatabase dataSource: HikariDataSource, vertx: Vertx): JDBCClient {
    return JDBCClient.create(vertx, dataSource)
  }

  @Provides
  @Singleton
  @ReadDatabase
  fun jdbi(@ReadDatabase dataSource: HikariDataSource): Jdbi {
    val jdbi = Jdbi.create(dataSource)
    jdbi.installPlugin(SqlObjectPlugin())
    jdbi.installPlugin(KotlinPlugin())
    jdbi.installPlugin(KotlinSqlObjectPlugin())
    return jdbi
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
//    hikariConfig.addDataSourceProperty("initializationFailTimeout", "10000")
    hikariConfig.isAutoCommit = true
    hikariConfig.isReadOnly =  true
    hikariConfig.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    return HikariDataSource(hikariConfig)
  }

}
