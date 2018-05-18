package io.github.crabzilla.example1

import dagger.Module
import io.github.crabzilla.vertx.CrabzillaModule
import io.github.crabzilla.vertx.ProjectionDbModule
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject


// tag::module[]
@Module(includes = [(Example1ProjectorModule::class), (ProjectionDbModule::class)])
class ProjectorServiceModule(vertx: Vertx, config: JsonObject) : CrabzillaModule(vertx, config) {

}
// end::module[]
