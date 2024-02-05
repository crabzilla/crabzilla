package io.github.crabzilla.example1.customer

import io.github.crabzilla.context.ViewTrigger
import io.vertx.core.Future
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection

class CustomersViewTrigger(private val eventBus: EventBus) : ViewTrigger {
  override fun checkCondition(viewAsJson: JsonObject): Boolean {
    return viewAsJson.getBoolean("is_active") == false
  }

  override fun handleTrigger(
    sqlConnection: SqlConnection,
    viewAsJson: JsonObject,
  ): Future<Void> {
    return eventBus.request<Void>(EVENTBUS_ADDRESS, viewAsJson).mapEmpty()
  }

  companion object {
    const val EVENTBUS_ADDRESS = "customer-view-trigger-handler"
  }
}
