package io.github.crabzilla

import io.vertx.core.AsyncResult
import io.vertx.core.Handler

typealias CommandHandlerFactory<E> = (CommandMetadata, Command, Snapshot<E>,
                                      Handler<AsyncResult<UnitOfWork>>) -> EntityCommandHandler<E>

typealias Version = Int
