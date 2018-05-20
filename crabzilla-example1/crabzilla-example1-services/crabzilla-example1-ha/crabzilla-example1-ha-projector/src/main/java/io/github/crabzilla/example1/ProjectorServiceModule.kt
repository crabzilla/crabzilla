package io.github.crabzilla.example1

import dagger.Module
import io.github.crabzilla.vertx.modules.CrabzillaModule
import io.github.crabzilla.vertx.modules.ProjectionDbModule
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

@Module(includes = [(Example1ProjectorModule::class), (ProjectionDbModule::class)])
class ProjectorServiceModule(vertx: Vertx, config: JsonObject) : CrabzillaModule(vertx, config)
