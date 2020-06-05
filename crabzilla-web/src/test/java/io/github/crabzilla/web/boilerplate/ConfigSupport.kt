package io.github.crabzilla.web.boilerplate

import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.logging.SLF4JLogDelegateFactory
import org.slf4j.Logger

object ConfigSupport {

  private val log: Logger = org.slf4j.LoggerFactory.getLogger(ConfigSupport::class.java)

  fun getConfig(vertx: Vertx, configFile: String): Future<JsonObject> {
    // slf4j setup
    System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME,
      SLF4JLogDelegateFactory::class.java.name)
    org.slf4j.LoggerFactory.getLogger(LoggerFactory::class.java)
    // Jackson setup
    DatabindCodec.mapper()
      .registerModule(Jdk8Module())
      .disable(WRITE_DATES_AS_TIMESTAMPS)
    // get config
    val promise = Promise.promise<JsonObject>()
    configRetriever(vertx, configFile).getConfig { gotConfig ->
      if (gotConfig.succeeded()) {
        val config = gotConfig.result()
        log.info("*** config:\n${config.encodePrettily()}")
        val httpPort = config.getInteger("HTTP_PORT")
        log.info("*** HTTP_PORT: $httpPort")
        promise.complete(config)
      } else {
        promise.fail(gotConfig.cause())
      }
    }
    return promise.future()
  }

  private fun configRetriever(vertx: Vertx, configFile: String): ConfigRetriever {
    val envOptions = ConfigStoreOptions()
      .setType("file")
      .setFormat("properties")
      .setConfig(JsonObject().put("path", configFile))
    val options = ConfigRetrieverOptions().addStore(envOptions)
    return ConfigRetriever.create(vertx, options)
  }
}
