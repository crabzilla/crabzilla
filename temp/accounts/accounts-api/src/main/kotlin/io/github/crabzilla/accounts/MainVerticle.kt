package io.github.crabzilla.accounts

import io.github.crabzilla.accounts.web.WebVerticle
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory

class MainVerticle : AbstractVerticle() {

  companion object {
    private val log = LoggerFactory.getLogger(MainVerticle::class.java)
    private val node = ManagementFactory.getRuntimeMXBean().name

    @JvmStatic
    fun main(args: Array<String>) {
      Vertx.vertx().deployVerticle(MainVerticle())
    }

  }

  override fun start(startPromise: Promise<Void>) {

    fun configRetriever(vertx: Vertx): ConfigRetriever {
      val fileStore = ConfigStoreOptions()
        .setType("file")
        .setConfig(JsonObject().put("path", "./../conf/config.json"))
      val options = ConfigRetrieverOptions().addStore(fileStore)
      return ConfigRetriever.create(vertx, options)
    }

    log.info("**** Node {} will start", node)
    configRetriever(vertx).config
      .compose { config ->
        log.info("**** config " + config.encodePrettily())
        log.info("**** clustered? ${vertx.isClustered}")
        // try to deploy web verticle
        val opt1 = DeploymentOptions().setConfig(config).setHa(false).setInstances(4)
        vertx.deployVerticle(WebVerticle::class.qualifiedName, opt1)
          .onSuccess {
            log.info("CommandWebVerticle started")
          }
      }
      .onFailure {
        startPromise.fail(it)
        log.error(it.message, it)
      }
      .onSuccess {
        startPromise.complete()
      }
  }

  override fun stop() {
    log.info("**** Stopped")
  }

}
