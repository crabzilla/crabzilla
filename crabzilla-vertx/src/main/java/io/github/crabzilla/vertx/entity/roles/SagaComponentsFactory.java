package io.github.crabzilla.vertx.entity.roles;

import io.github.crabzilla.core.Command;
import io.github.crabzilla.core.DomainEvent;
import io.github.crabzilla.core.entity.Entity;

import java.util.List;
import java.util.function.Function;

public interface SagaComponentsFactory<A extends Entity> extends AggregateComponentsFactory<A> {

  Function<DomainEvent, List<Command>> reactionFn();

}

