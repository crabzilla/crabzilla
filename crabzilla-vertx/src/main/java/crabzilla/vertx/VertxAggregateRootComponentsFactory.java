package crabzilla.vertx;

import crabzilla.model.AggregateRoot;
import crabzilla.stack.AggregateRootFunctionsFactory;
import crabzilla.vertx.repositories.VertxUnitOfWorkRepository;
import crabzilla.vertx.verticles.CommandHandlerVerticle;
import crabzilla.vertx.verticles.CommandRestVerticle;

public interface VertxAggregateRootComponentsFactory<A extends AggregateRoot>

        extends AggregateRootFunctionsFactory<A> {

  CommandRestVerticle<A> restVerticle();

  CommandHandlerVerticle<A> cmdHandlerVerticle();

  VertxUnitOfWorkRepository uowRepository();

}
