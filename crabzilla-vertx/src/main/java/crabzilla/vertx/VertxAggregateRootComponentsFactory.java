package crabzilla.vertx;

import com.github.benmanes.caffeine.cache.Cache;
import crabzilla.model.AggregateRoot;
import crabzilla.model.Snapshot;
import crabzilla.stack.AggregateRootComponentsFactory;
import crabzilla.vertx.verticles.CommandHandlerVerticle;
import crabzilla.vertx.verticles.CommandRestVerticle;

public interface VertxAggregateRootComponentsFactory<A extends AggregateRoot>

        extends AggregateRootComponentsFactory<A> {

  Cache<String, Snapshot<A>> cache() ;

  CommandRestVerticle<A> restVerticle();

  CommandHandlerVerticle<A> cmdHandlerVerticle();

}
