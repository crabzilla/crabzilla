package io.github.crabzilla.vertx.modules

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dagger.Module
import dagger.Provides
import io.github.crabzilla.vertx.qualifiers.WriteDatabase
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin
import javax.inject.Singleton

@Module
class WriteDbModule {

  @Provides
  @Singleton
  @WriteDatabase
  fun jdbcClient(@WriteDatabase dataSource: HikariDataSource, vertx: Vertx): JDBCClient {
    return JDBCClient.create(vertx, dataSource)
  }

  @Provides
  @Singleton
  @WriteDatabase
  fun jdbi(@WriteDatabase dataSource: HikariDataSource): Jdbi {
    val jdbi = Jdbi.create(dataSource)
    jdbi.installPlugin(SqlObjectPlugin())
    jdbi.installPlugin(KotlinPlugin())
    jdbi.installPlugin(KotlinSqlObjectPlugin())
    return jdbi
  }

  @Provides
  @Singleton
  @WriteDatabase
  fun hikariDs(config: JsonObject): HikariDataSource {

    val hikariConfig = HikariConfig()
    hikariConfig.driverClassName = config.getString("WRITE_DATABASE_DRIVER")
    hikariConfig.jdbcUrl = config.getString("WRITE_DATABASE_URL")
    hikariConfig.username = config.getString("WRITE_DATABASE_USER")
    hikariConfig.password = config.getString("WRITE_DATABASE_PASSWORD")
    hikariConfig.connectionTimeout = 5000
    hikariConfig.maximumPoolSize = config.getInteger("WRITE_DATABASE_POOL_MAX_SIZE")!!
    hikariConfig.addDataSourceProperty("cachePrepStmts", "true")
    hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250")
    hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
    hikariConfig.isAutoCommit = false
    hikariConfig.isReadOnly =  false
    hikariConfig.transactionIsolation = "TRANSACTION_SERIALIZABLE"
    return HikariDataSource(hikariConfig)
  }

}
