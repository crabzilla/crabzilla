package io.github.crabzilla.example1

import dagger.Module
import dagger.Provides
import io.github.crabzilla.vertx.CrabzillaModule
import io.github.crabzilla.vertx.UnitOfWorkRepository
import io.github.crabzilla.vertx.WriteDatabase
import io.github.crabzilla.vertx.impl.UnitOfWorkRepositoryImpl
import io.github.crabzilla.vertx.modules.ProjectionDbModule
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import javax.inject.Singleton


// tag::module[]
@Module(includes = [(Example1ProjectorModule::class), (ProjectionDbModule::class)])
class ProjectorServiceModule(vertx: Vertx, config: JsonObject) : CrabzillaModule(vertx, config) {

  @Provides
  @Singleton
  fun projectionRepo(@WriteDatabase jdbiClient: JDBCClient): UnitOfWorkRepository {
    return UnitOfWorkRepositoryImpl(jdbiClient)
  }

}
// end::module[]
