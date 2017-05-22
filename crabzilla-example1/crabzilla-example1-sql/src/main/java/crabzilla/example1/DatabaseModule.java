package crabzilla.example1;

import com.google.inject.Exposed;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.skife.jdbi.v2.DBI;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.sql.DataSource;

public class DatabaseModule extends PrivateModule {

  @Override
  protected void configure() {

  }

  @Provides
  @Singleton
  @Exposed
  public HikariDataSource hikariDataSource(HikariConfig config) {
    return new HikariDataSource(config);
  }

  @Provides
  @Singleton
  @Exposed
  DBI cfg(DataSource ds, @Named("database.query.timeout.ms") int dbQueryTimeoutMs) {
    return new DBI(ds);
  }

  @Provides
  @Singleton
  @Exposed
  public DataSource dataSource(HikariDataSource dataSource) {
    return dataSource;
  }

  @Provides
  @Singleton
  public HikariConfig config(@Named("database.driver") String dbDriver,
                             @Named("database.url") String dbUrl,
                             @Named("database.user") String dbUser,
                             @Named("database.password") String dbPwd,
                             @Named("database.pool.max.size") Integer databaseMaxSize) {

    HikariConfig config = new HikariConfig();
    config.setDriverClassName(dbDriver);
    config.setJdbcUrl(dbUrl);
    config.setUsername(dbUser);
    config.setPassword(dbPwd);
    config.setConnectionTimeout(5000);
    config.setMaximumPoolSize(databaseMaxSize);
    config.addDataSourceProperty("cachePrepStmts", "true");
    config.addDataSourceProperty("prepStmtCacheSize", "250");
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    config.setAutoCommit(false);
    // config.setTransactionIsolation("TRANSACTION_REPEATABLE_READ");
    config.setTransactionIsolation("TRANSACTION_SERIALIZABLE");
    return config;
  }

}
