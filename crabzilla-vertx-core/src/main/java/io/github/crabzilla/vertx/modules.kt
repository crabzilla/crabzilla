package io.github.crabzilla.vertx

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dagger.Module
import dagger.Provides
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin
import javax.inject.Singleton

@Module(includes = arrayOf(WriteDbModule::class, ReadDbModule::class))
open class CrabzillaModule(val vertx: Vertx, val config: JsonObject) {

  init {
    initVertx(vertx)
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

}

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
    hikariConfig.isAutoCommit = true
    hikariConfig.isReadOnly =  true
    return HikariDataSource(hikariConfig)
  }

}

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
