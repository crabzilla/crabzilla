package io.github.crabzilla.web.example1

import io.github.crabzilla.pgc.Crabzilla
import io.github.crabzilla.web.WebEntityComponent
import io.vertx.ext.web.Router

fun Crabzilla.addWebResource(resourceName: String, entityName: String, router: Router) {
  Crabzilla.log.info("adding resource $name")
  WebEntityComponent(entities[entityName]!!, resourceName = resourceName).deployWebRoutes(router)
}
