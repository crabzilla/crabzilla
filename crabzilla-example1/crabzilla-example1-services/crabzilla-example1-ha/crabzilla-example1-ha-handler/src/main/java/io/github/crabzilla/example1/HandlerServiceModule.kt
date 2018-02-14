package io.github.crabzilla.example1

import dagger.Module
import io.github.crabzilla.vertx.modules.CrabzillaModule
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject


// tag::module[]
@Module(includes = arrayOf(HandlerModule::class))
class HandlerServiceModule(vertx: Vertx, config: JsonObject) : CrabzillaModule(vertx, config) {

}
// end::module[]
