package crabzilla.example1

import com.google.inject.Exposed
import com.google.inject.PrivateModule
import com.google.inject.Provides
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

import javax.inject.Named
import javax.inject.Singleton

internal class KDatabaseModule : PrivateModule() {

  override fun configure() {

  }


  @Provides
  @Singleton
  fun config(@Named("database.driver") dbDriver: String,
             @Named("database.url") dbUrl: String,
             @Named("database.user") dbUser: String,
             @Named("database.password") dbPwd: String,
             @Named("database.pool.max.size") databaseMaxSize: Int?): HikariConfig {

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
    return config
  }

  @Provides
  @Singleton
  @Exposed
  fun hikariDataSource(config: HikariConfig): HikariDataSource {
    return HikariDataSource(config)
  }

}
