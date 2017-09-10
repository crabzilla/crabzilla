package io.github.crabzilla.example1;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.crabzilla.core.DomainEvent;
import io.github.crabzilla.core.entity.EntityCommand;
import io.github.crabzilla.core.entity.EntityId;
import io.github.crabzilla.core.entity.EntityUnitOfWork;
import io.github.crabzilla.example1.services.SampleInternalService;
import io.github.crabzilla.example1.services.SampleInternalServiceImpl;
import io.github.crabzilla.vertx.codecs.JacksonGenericCodec;
import io.github.crabzilla.vertx.entity.EntityCommandExecution;
import io.github.crabzilla.vertx.projection.EventProjector;
import io.github.crabzilla.vertx.projection.EventsProjectionVerticle;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import lombok.val;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import javax.inject.Singleton;

import static io.vertx.core.json.Json.mapper;

class Example1Module extends AbstractModule {

  private final Vertx vertx;
  private final JsonObject config;

  Example1Module(Vertx vertx, JsonObject config) {
    this.vertx = vertx;
    this.config = config;
  }

  @Override
  protected void configure() {

    configureVertx();

    // services
    bind(SampleInternalService.class).to(SampleInternalServiceImpl.class).asEagerSingleton();

    // event projection verticles
    MapBinder<String, Verticle> mapbinder =
            MapBinder.newMapBinder(binder(), String.class, Verticle.class);

    TypeLiteral<EventsProjectionVerticle<CustomerSummaryDao>> type =
            new TypeLiteral<EventsProjectionVerticle<CustomerSummaryDao>>() {};

    mapbinder.addBinding("example1.events.projector").to(type);

  }

  @Provides
  @Singleton
  Vertx vertx() {
    return vertx;
  }

  @Provides
  @Singleton
  JsonObject config() {
    return config;
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
  public EventsProjectionVerticle<CustomerSummaryDao> eventsProjectorVerticle(Jdbi jdbi,
                                                                              EventProjector<CustomerSummaryDao> eventsProjector) {
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
  public HikariDataSource hikariDs() {

    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setDriverClassName(config.getString("database.driver"));
    hikariConfig.setJdbcUrl(config.getString("database.url"));
    hikariConfig.setUsername(config.getString("database.user"));
    hikariConfig.setPassword(config.getString("database.password"));
    hikariConfig.setConnectionTimeout(5000);
    hikariConfig.setMaximumPoolSize(config.getInteger("database.pool.max.size"));
    hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
    hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
    hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    hikariConfig.setAutoCommit(false);
    // config.setTransactionIsolation("TRANSACTION_REPEATABLE_READ");
    hikariConfig.setTransactionIsolation("TRANSACTION_SERIALIZABLE");
    return new HikariDataSource(hikariConfig);
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
