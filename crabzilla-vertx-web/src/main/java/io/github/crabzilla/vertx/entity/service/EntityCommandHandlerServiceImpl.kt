package io.github.crabzilla.vertx.entity.service

import io.github.crabzilla.core.entity.EntityCommand
import io.github.crabzilla.vertx.EntityCommandExecution
import io.github.crabzilla.vertx.ProjectionData
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.http.CaseInsensitiveHeaders
import org.slf4j.LoggerFactory

class EntityCommandHandlerServiceImpl(private val vertx: Vertx, private val projectionEndpoint: String) :
  EntityCommandHandlerService {

  init {
    log.info("will publish resulting events to {}", projectionEndpoint)
  }

  override fun postCommand(handlerEndpoint: String, command: EntityCommand,
                           handler: Handler<AsyncResult<EntityCommandExecution>>) {

    val options = DeliveryOptions().setCodecName(EntityCommand::class.java.simpleName)

    vertx.eventBus().send<EntityCommand>(handlerEndpoint, command, options) { response ->

      if (!response.succeeded()) {
        log.error("eventbus.handleCommand", response.cause())
        handler.handle(Future.failedFuture(response.cause()))
        return@send
      }

      val result = response.result().body() as EntityCommandExecution
      log.info("result = {}", result)
      val headers = CaseInsensitiveHeaders().add("uowSequence", result.uowSequence.toString())
      val optionsUow = DeliveryOptions().setCodecName(ProjectionData::class.simpleName).setHeaders(headers)

      val uow = result.unitOfWork

      if (uow != null) {
        val pd = ProjectionData(uow.unitOfWorkId, result.uowSequence!!, uow.targetId().stringValue(), uow.events)
        vertx.eventBus().publish(projectionEndpoint, pd, optionsUow)
      }

      handler.handle(Future.succeededFuture(result))

    }

  }

  companion object {

    internal var log = LoggerFactory.getLogger(EntityCommandHandlerServiceImpl::class.java)
  }

}

