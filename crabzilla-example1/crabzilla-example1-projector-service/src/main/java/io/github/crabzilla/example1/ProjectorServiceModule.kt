package io.github.crabzilla.example1

import dagger.Module
import dagger.Provides
import io.github.crabzilla.vertx.modules.CrabzillaModule
import io.github.crabzilla.vertx.modules.ProjectionDbModule
import io.github.crabzilla.vertx.qualifiers.WriteDatabase
import io.github.crabzilla.vertx.projection.ProjectionRepository
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import javax.inject.Singleton


// tag::module[]
@Module(includes = [ProjectorModule::class, ProjectionDbModule::class])
class ProjectorServiceModule(vertx: Vertx, config: JsonObject) : CrabzillaModule(vertx, config) {

  @Provides
  @Singleton
  fun projectionRepo(@WriteDatabase jdbiClient: JDBCClient): ProjectionRepository {
    return ProjectionRepository(jdbiClient)
  }

}
// end::module[]
