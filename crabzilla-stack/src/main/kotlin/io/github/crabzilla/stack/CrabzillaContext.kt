package io.github.crabzilla.stack

import io.github.crabzilla.core.FeatureComponent
import io.github.crabzilla.stack.command.CommandServiceApi
import io.github.crabzilla.stack.command.CommandServiceOptions
import io.github.crabzilla.stack.subscription.SubscriptionApi
import io.github.crabzilla.stack.subscription.SubscriptionConfig
import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.pgclient.PgPool
import io.vertx.pgclient.pubsub.PgSubscriber

interface CrabzillaContext {

  companion object {
    const val POSTGRES_NOTIFICATION_CHANNEL = "crabzilla_channel"
    const val EVENTBUS_GLOBAL_TOPIC = "crabzilla.eventbus.global-topic"
  }

  fun vertx(): Vertx

  fun pgPool(): PgPool

  fun deploy(subscriptionOptions: DeploymentOptions = DeploymentOptions().setInstances(1)): Future<Void>

  fun <S : Any, C : Any, E : Any> commandService(
    component: FeatureComponent<S, C, E>,
    jsonObjectSerDer: JsonObjectSerDer<S, C, E>,
    options: CommandServiceOptions = CommandServiceOptions(),
  ): CommandServiceApi<C>

  fun subscription(config: SubscriptionConfig, eventProjector: EventProjector? = null): SubscriptionApi

  fun pgSubscriber(): PgSubscriber

}
