package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import dagger.Component
import io.github.crabzilla.core.Entity
import io.github.crabzilla.example1.customer.CustomerModule
import io.github.crabzilla.vertx.modules.qualifiers.WriteDatabase
import io.github.crabzilla.vertx.verticles.CommandHandlerVerticle
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(CustomerModule::class, HandlerServiceModule::class))
interface HandlerServiceComponent {

  fun commandVerticles(): Set<CommandHandlerVerticle<out Entity>>

  @WriteDatabase
  fun datasource(): HikariDataSource
}
