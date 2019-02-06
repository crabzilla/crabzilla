package io.github.crabzilla.web

import io.github.crabzilla.Command
import io.github.crabzilla.CommandExecution
import io.github.crabzilla.UnitOfWork
import io.github.crabzilla.vertx.ProjectionData.Companion.fromUnitOfWork
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.http.CaseInsensitiveHeaders
import org.slf4j.LoggerFactory

open class CommandHandlerService(private val vertx: Vertx, private val projectionEndpoint: String) {

  companion object {
    internal var log = LoggerFactory.getLogger(CommandHandlerService::class.java)
  }

  private val commandDeliveryOptions = DeliveryOptions().setCodecName(Command::class.java.simpleName)

  init {
    log.info("will publish resulting events to {}", projectionEndpoint)
  }

  fun postCommand(handlerEndpoint: String, command: Command, handler: Handler<AsyncResult<CommandExecution>>) {

    log.info("posting a command to {}", handlerEndpoint)

    vertx.eventBus().send<Command>(handlerEndpoint, command, commandDeliveryOptions) { response ->

      if (!response.succeeded()) {
        log.error("postCommand", response.cause())
        handler.handle(Future.failedFuture(response.cause()))
        return@send
      }

      val result = response.result().body() as CommandExecution
      val uow = result.unitOfWork
      val uowSequence = result.uowSequence ?: 0

      log.info("result = {}", result)

      if (uow != null && uowSequence != 0) {
        val headers = CaseInsensitiveHeaders().add("uowSequence", uowSequence.toString())
        val eventsDeliveryOptions = DeliveryOptions().setCodecName(UnitOfWork::class.simpleName).setHeaders(headers)
        vertx.eventBus().publish(projectionEndpoint, fromUnitOfWork(uowSequence, uow), eventsDeliveryOptions)
      }

      handler.handle(Future.succeededFuture(result))

    }

  }

}
