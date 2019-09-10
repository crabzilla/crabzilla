package io.github.crabzilla.framework

import io.vertx.core.AsyncResult
import io.vertx.core.Handler

interface EntityCommandHandlerFactory<E: Entity> {

  fun createHandler(cmdMetadata: CommandMetadata, command: Command, snapshot: Snapshot<E>,
                    handler: Handler<AsyncResult<UnitOfWork>>): EntityCommandHandler<E>

}
