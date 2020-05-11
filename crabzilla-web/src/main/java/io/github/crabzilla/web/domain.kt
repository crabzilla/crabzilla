package io.github.crabzilla.web

import io.github.crabzilla.core.CrabzillaContext
import io.github.crabzilla.core.Entity
import io.github.crabzilla.core.EntityComponent
import io.github.crabzilla.core.WebResourceContext
import io.vertx.ext.web.Router
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("web-pgc-domain")

fun <E : Entity> addResourceForEntity(
  router: Router,
  ctx: CrabzillaContext,
  webCtx: WebResourceContext<E>
) {
  log.info("adding web command handler for entity $webCtx.entityName on resource $webCtx.resourceName")
  val cmdHandlerComponent = EntityComponent(ctx, webCtx.entityName, webCtx.snapshotRepo, webCtx.cmdAware)
  WebDeployer(webCtx.resourceName, webCtx.cmdTypeMap, cmdHandlerComponent, router).deployWebRoutes()
}
