package io.github.crabzilla.web.command

import io.github.crabzilla.core.command.CrabzillaContext
import io.github.crabzilla.core.command.Entity
import io.vertx.ext.web.Router
import org.slf4j.LoggerFactory

class WebResourcesRegistry {

  companion object {
    private val log = LoggerFactory.getLogger(WebResourcesRegistry::class.java)
  }

  fun <E : Entity> add(router: Router, ctx: CrabzillaContext, webCtx: WebResourceContext<E>): WebResourcesRegistry {
    log.info("adding web command handler for entity ${webCtx.cmdAware.entityName}")
    val cmdHandlerComponent = EntityComponent(ctx, webCtx.snapshotRepo, webCtx.cmdAware)
    WebResourceDeployer(webCtx.cmdAware.entityName, webCtx.cmdTypeMap, cmdHandlerComponent, router).deployWebRoutes()
    return this
  }
}
