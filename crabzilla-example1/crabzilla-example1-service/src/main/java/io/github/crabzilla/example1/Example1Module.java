package io.github.crabzilla.example1;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.crabzilla.core.DomainEvent;
import io.github.crabzilla.core.entity.EntityCommand;
import io.github.crabzilla.core.entity.EntityId;
import io.github.crabzilla.core.entity.EntityUnitOfWork;
import io.github.crabzilla.example1.customer.CustomerModule;
import io.github.crabzilla.example1.services.SampleInternalService;
import io.github.crabzilla.example1.services.SampleInternalServiceImpl;
import io.github.crabzilla.vertx.codecs.JacksonGenericCodec;
import io.github.crabzilla.vertx.entity.EntityCommandExecution;
import io.github.crabzilla.vertx.projection.EventProjector;
import io.github.crabzilla.vertx.projection.EventsProjectionVerticle;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Vertx;
import io.vertx.ext.jdbc.JDBCClient;
import lombok.val;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import javax.inject.Named;
import java.util.Properties;

import static io.vertx.core.json.Json.mapper;

class Example1Module extends AbstractModule {

  final Vertx vertx;

  Example1Module(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  protected void configure() {

    configureVertx();
    // aggregates
    install(new CustomerModule());
    // services
    bind(SampleInternalService.class).to(SampleInternalServiceImpl.class).asEagerSingleton();
    // exposes properties to guice
    setCfgProps();

  }

  private void setCfgProps() {

    final Config config = ConfigFactory.load();
    final Properties props = new Properties();

    config.entrySet().forEach(e -> {
      final String key = e.getKey().replace("example1.", "");
      final String value = e.getValue().render().replace("\"", "");
      props.put(key, value);
    });

    Names.bindProperties(binder(), props);
  }

  @Provides
  @Singleton
  Vertx vertx() {
    return vertx;
  }

  @Provides
  @Singleton
  public Jdbi jdbi(HikariDataSource dataSource) {
    val jdbi = Jdbi.create(dataSource);
    jdbi.installPlugin(new SqlObjectPlugin());
//    jdbi.registerRowMapper(ConstructorMapper.factory(CustomerSummary.class)); // TODO how to avoid this ?
    return jdbi;
  }

  @Provides
  @Singleton
  public EventProjector<CustomerSummaryDao> eventsProjector(Jdbi jdbi) {
    return new Example1EventProjector("example1", CustomerSummaryDao.class, jdbi) ;
  }

  @Provides
  @Singleton
  public EventsProjectionVerticle<CustomerSummaryDao> eventsProjectorVerticle(EventProjector<CustomerSummaryDao> eventsProjector) {
    val circuitBreaker = CircuitBreaker.create("events-projection-circuit-breaker", vertx,
            new CircuitBreakerOptions()
                    .setMaxFailures(5) // number SUCCESS failure before opening the circuit
                    .setTimeout(2000) // consider a failure if the operation does not succeed in time
                    .setFallbackOnFailure(true) // do we call the fallback on failure
                    .setResetTimeout(10000) // time spent in open state before attempting to re-try
    );
    return new EventsProjectionVerticle<>(eventsProjector, circuitBreaker) ;
  }

  @Provides
  @Singleton
  JDBCClient jdbcClient(Vertx vertx, HikariDataSource dataSource) {
    return JDBCClient.create(vertx, dataSource);
  }

  @Provides
  @Singleton
  public HikariDataSource config(@Named("database.driver") String dbDriver,
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
    return new HikariDataSource(config);
  }

//  Not being used yet. This can improve a lot serialization speed (it's binary). But so far it was not necessary.
//  @Provides
//  @Singleton
//  FSTConfiguration conf() {
//    return FSTConfiguration.createDefaultConfiguration();
//  }

  void configureVertx() {

    mapper.registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());

    vertx.eventBus().registerDefaultCodec(EntityCommandExecution.class,
            new JacksonGenericCodec<>(mapper, EntityCommandExecution.class));

    vertx.eventBus().registerDefaultCodec(EntityId.class,
            new JacksonGenericCodec<>(mapper, EntityId.class));

    vertx.eventBus().registerDefaultCodec(EntityCommand.class,
            new JacksonGenericCodec<>(mapper, EntityCommand.class));

    vertx.eventBus().registerDefaultCodec(DomainEvent.class,
            new JacksonGenericCodec<>(mapper, DomainEvent.class));

    vertx.eventBus().registerDefaultCodec(EntityUnitOfWork.class,
            new JacksonGenericCodec<>(mapper, EntityUnitOfWork.class));

  }

}
