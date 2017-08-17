package crabzilla.vertx;

import crabzilla.model.*;
import crabzilla.vertx.repositories.EntityUnitOfWorkRepository;
import crabzilla.vertx.verticles.CommandHandlerVerticle;
import crabzilla.vertx.verticles.CommandRestVerticle;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Vertx;
import io.vertx.ext.jdbc.JDBCClient;
import lombok.val;
import net.jodah.expiringmap.ExpiringMap;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static crabzilla.stack.StringHelper.circuitBreakerId;

public interface AggregateRootComponentsFactory<A extends Aggregate> {

  Class<A> clazz();

  Supplier<A> supplierFn() ;

  Function<A, A> depInjectionFn() ;

  BiFunction<DomainEvent, A, A> stateTransitionFn() ;

  Function<EntityCommand, List<String>> cmdValidatorFn() ;

  BiFunction<EntityCommand, Snapshot<A>, CommandHandlerResult> cmdHandlerFn() ;

  JDBCClient jdbcClient();

  Vertx vertx();

  // default impl

  default SnapshotPromoter<A> snapshotPromoter() {
    return new SnapshotPromoter<>(supplierFn(),
            instance -> new StateTransitionsTracker<>(instance, stateTransitionFn(), depInjectionFn()));
  }

  default CommandRestVerticle<A> restVerticle() {
    return new CommandRestVerticle<>(clazz());
  }

  default CommandHandlerVerticle<A> cmdHandlerVerticle() {

    final ExpiringMap<String, Snapshot<A>> cache = ExpiringMap.builder()
            .expiration(5, TimeUnit.MINUTES)
            .maxSize(10_000)
            .build();

    val circuitBreaker = CircuitBreaker.create(circuitBreakerId(clazz()), vertx(),
            new CircuitBreakerOptions()
                    .setMaxFailures(5) // number SUCCESS failure before opening the circuit
                    .setTimeout(2000) // consider a failure if the operation does not succeed in time
                    .setFallbackOnFailure(true) // do we call the fallback on failure
                    .setResetTimeout(10000) // time spent in open state before attempting to re-try
    );

    return new CommandHandlerVerticle<>(clazz(), supplierFn().get(), cmdHandlerFn(),
            cmdValidatorFn(), snapshotPromoter(), uowRepository(), cache, circuitBreaker);
  }

  default EntityUnitOfWorkRepository uowRepository() {
    return new EntityUnitOfWorkRepository(clazz(), jdbcClient());
  }

}
