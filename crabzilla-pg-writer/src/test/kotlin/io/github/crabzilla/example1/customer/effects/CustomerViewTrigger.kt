package io.github.crabzilla.example1.customer.effects

import io.github.crabzilla.context.ViewTrigger
import io.vertx.core.Future
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection

class CustomerViewTrigger(private val eventBus: EventBus) : ViewTrigger {
  override fun checkCondition(viewAsJson: JsonObject): Boolean {
    return viewAsJson.getBoolean("is_active") == false
  }

  override fun handleTrigger(
    sqlConnection: SqlConnection,
    viewAsJson: JsonObject,
  ): Future<Void> {
    // notice you can use publish instead of request/reply
    val result = eventBus.request<Void>(EVENTBUS_ADDRESS, viewAsJson)
    if (result.failed()) {
      throw result.cause()
    } else {
      return Future.succeededFuture()
    }
  }

  companion object {
    const val EVENTBUS_ADDRESS = "customer-view-trigger-handler"
  }
}
