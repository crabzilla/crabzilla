package io.github.crabzilla.accounts

import com.hazelcast.config.Config
import io.github.crabzilla.accounts.processors.PendingTransfersVerticle
import io.github.crabzilla.pgclient.deployProjector
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.spi.cluster.ClusterManager
import io.vertx.spi.cluster.hazelcast.ConfigUtil
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
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

    fun clusterMgr(): ClusterManager {
      val hazelcastConfig: Config = ConfigUtil.loadConfig().setLiteMember(false)
      hazelcastConfig.setProperty("hazelcast.logging.type", "slf4j")
//      hazelcastConfig.cpSubsystemConfig.cpMemberCount = 3
      return HazelcastClusterManager(hazelcastConfig)
    }

    fun configRetriever(vertx: Vertx): ConfigRetriever {
      val fileStore = ConfigStoreOptions()
        .setType("file")
        .setConfig(JsonObject().put("path", "./../conf/config.json"))
      val options = ConfigRetrieverOptions().addStore(fileStore)
      return ConfigRetriever.create(vertx, options)
    }

    val options = VertxOptions().setClusterManager(clusterMgr()).setHAEnabled(true)
    Vertx.clusteredVertx(options)
      .compose { vertx ->
        configRetriever(vertx).config
          .compose { config ->
            log.info("**** Node {} will start", node)
            log.info("**** config " + config.encodePrettily())
            log.info("**** clustered? ${vertx.isClustered}")
            vertx.deployProcessor(config, PendingTransfersVerticle::class.java)
              .compose {
                vertx.deployProjector(config, "service:projectors.accounts.AccountsView")
              }
              .compose {
                vertx.deployProjector(config, "service:projectors.transfers.TransfersView")
              }
              .onFailure {
                startPromise.fail(it)
                log.error(it.message, it)
              }
              .onSuccess {
                startPromise.complete()
              }
          }
      }

  }

  override fun stop() {
    log.info("**** Stopped")
  }

}
