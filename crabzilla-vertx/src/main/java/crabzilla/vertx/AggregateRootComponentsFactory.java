package crabzilla.vertx;

import crabzilla.model.Aggregate;
import crabzilla.model.Snapshot;
import crabzilla.model.SnapshotPromoter;
import crabzilla.model.StateTransitionsTracker;
import crabzilla.stack.AggregateRootFunctionsFactory;
import crabzilla.vertx.repositories.EntityUnitOfWorkRepository;
import crabzilla.vertx.verticles.CommandHandlerVerticle;
import crabzilla.vertx.verticles.CommandRestVerticle;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Vertx;
import io.vertx.ext.jdbc.JDBCClient;
import lombok.val;
import net.jodah.expiringmap.ExpiringMap;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static crabzilla.stack.StringHelper.circuitBreakerId;

public interface AggregateRootComponentsFactory<A extends Aggregate>

        extends AggregateRootFunctionsFactory<A> {

  Class<A> clazz();

  Function<A, A> depInjectionFn();

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
