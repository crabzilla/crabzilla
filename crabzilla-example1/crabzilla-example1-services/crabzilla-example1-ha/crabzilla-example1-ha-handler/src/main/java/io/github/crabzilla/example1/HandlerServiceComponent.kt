package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import dagger.Component
import dagger.Provides
import io.github.crabzilla.core.Entity
import io.github.crabzilla.example1.customer.CustomerModule
import io.github.crabzilla.vertx.modules.qualifiers.WriteDatabase
import io.github.crabzilla.vertx.verticles.CommandVerticle
import io.github.crabzilla.vertx.verticles.HealthVerticle
import io.vertx.core.json.JsonObject
import io.vertx.ext.healthchecks.HealthCheckHandler
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(CustomerModule::class, HandlerServiceModule::class))
interface HandlerServiceComponent {

  fun commandVerticles(): Set<CommandVerticle<out Entity>>

  fun healthVerticle(): HealthVerticle

  @WriteDatabase
  fun datasource(): HikariDataSource


}
