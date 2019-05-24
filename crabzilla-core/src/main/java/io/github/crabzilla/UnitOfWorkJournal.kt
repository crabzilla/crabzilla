package io.github.crabzilla

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import java.math.BigInteger

interface UnitOfWorkJournal {

  fun append(unitOfWork: UnitOfWork, aHandler: Handler<AsyncResult<BigInteger>>)
}
