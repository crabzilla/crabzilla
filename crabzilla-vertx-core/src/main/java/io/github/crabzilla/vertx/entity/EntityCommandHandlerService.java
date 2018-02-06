package io.github.crabzilla.vertx.entity;

import io.github.crabzilla.core.entity.EntityCommand;
import io.github.crabzilla.vertx.entity.EntityCommandExecution;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;


public interface EntityCommandHandlerService {

  void postCommand(String handlerEndpoint, EntityCommand command, Handler<AsyncResult<EntityCommandExecution>> handler);

}
