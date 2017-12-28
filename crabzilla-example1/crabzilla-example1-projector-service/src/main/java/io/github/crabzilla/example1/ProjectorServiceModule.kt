package io.github.crabzilla.example1

import dagger.Module
import io.github.crabzilla.vertx.modules.CrabzillaModule
import io.github.crabzilla.vertx.modules.ProjectionDbModule
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject


// tag::module[]
@Module(includes = [ProjectorModule::class, ProjectionDbModule::class])
class ProjectorServiceModule(vertx: Vertx, config: JsonObject) : CrabzillaModule(vertx, config)
// end::module[]