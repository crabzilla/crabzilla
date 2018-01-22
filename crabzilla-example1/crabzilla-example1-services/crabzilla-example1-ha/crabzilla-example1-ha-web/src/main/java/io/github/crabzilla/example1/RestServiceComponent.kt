package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import dagger.Component
import io.github.crabzilla.vertx.entity.EntityCommandRestVerticle
import io.github.crabzilla.vertx.qualifiers.WriteDatabase
import javax.inject.Singleton

// tag::component[]
@Singleton
@Component(modules = [RestServiceModule::class])
interface RestServiceComponent {

  fun restVerticles(): Set<EntityCommandRestVerticle<out Any>>

  @WriteDatabase
  fun datasource(): HikariDataSource
}

// end::component[]
