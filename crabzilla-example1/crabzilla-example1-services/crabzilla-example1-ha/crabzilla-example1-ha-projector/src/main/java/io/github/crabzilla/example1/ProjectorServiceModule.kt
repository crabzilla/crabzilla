package io.github.crabzilla.example1

import dagger.Module
import dagger.Provides
import io.github.crabzilla.vertx.entity.EntityUnitOfWorkRepository
import io.github.crabzilla.vertx.entity.impl.EntityUnitOfWorkRepositoryImpl
import io.github.crabzilla.vertx.modules.CrabzillaModule
import io.github.crabzilla.vertx.modules.ProjectionDbModule
import io.github.crabzilla.vertx.modules.qualifiers.WriteDatabase
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import javax.inject.Singleton


// tag::module[]
@Module(includes = arrayOf(ProjectorModule::class, ProjectionDbModule::class))
class ProjectorServiceModule(vertx: Vertx, config: JsonObject) : CrabzillaModule(vertx, config) {

  @Provides
  @Singleton
  fun projectionRepo(@WriteDatabase jdbiClient: JDBCClient): EntityUnitOfWorkRepository {
    return EntityUnitOfWorkRepositoryImpl(jdbiClient)
  }

}
// end::module[]
