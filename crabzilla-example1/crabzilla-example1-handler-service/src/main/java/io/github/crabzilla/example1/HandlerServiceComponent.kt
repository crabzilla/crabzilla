package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import dagger.Component
import io.github.crabzilla.core.entity.Entity
import io.github.crabzilla.example1.customer.CustomerModule
import io.github.crabzilla.vertx.entity.EntityCommandHandlerVerticle
import io.github.crabzilla.vertx.entity.EntityCommandRestVerticle
import io.github.crabzilla.vertx.modules.qualifiers.WriteDatabase
import javax.inject.Singleton

// tag::component[]
@Singleton
@Component(modules = [CustomerModule::class, HandlerServiceModule::class])
interface HandlerServiceComponent {

  fun commandVerticles(): Set<EntityCommandHandlerVerticle<out Entity>>

  fun restVerticles(): Set<EntityCommandRestVerticle<out Any>>

  @WriteDatabase
  fun datasource(): HikariDataSource
}

// end::component[]