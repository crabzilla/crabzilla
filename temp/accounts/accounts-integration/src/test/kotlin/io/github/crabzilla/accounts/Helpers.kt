package io.github.crabzilla.accounts

import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet

object Helpers {

  fun configRetriever(vertx: Vertx): ConfigRetriever {
    val fileStore = ConfigStoreOptions()
      .setType("file")
      .setConfig(JsonObject().put("path", "./../conf/config.json"))
    val options = ConfigRetrieverOptions().addStore(fileStore)
    return ConfigRetriever.create(vertx, options)
  }

  fun RowSet<Row>.asJson(key: String): JsonObject {
    val json = JsonObject()
    this.forEach {
      val rowAsJson = it.toJson()
      json.put(rowAsJson.getString("id"), rowAsJson)
    }
    return json
  }

}