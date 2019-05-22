package io.github.crabzilla

import io.vertx.core.AsyncResult
import io.vertx.core.Handler

typealias CommandHandlerFactory<E> = (CommandMetadata, Command, Snapshot<E>,
                                      Handler<AsyncResult<UnitOfWork>>) -> CommandHandler<E>

typealias Version = Int

fun cmdHandlerEndpoint(entityName: String): String {
  return "$entityName-cmd-handler"
}
