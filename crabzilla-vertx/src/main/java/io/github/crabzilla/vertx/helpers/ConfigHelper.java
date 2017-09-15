package io.github.crabzilla.vertx.helpers;

import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.File;

@Slf4j
public class ConfigHelper {

  public static ConfigRetrieverOptions cfgOptions(String configFile){

    if (configFile!= null && !configFile.isEmpty()
            && new File(configFile).exists()) {

      ConfigStoreOptions file = new ConfigStoreOptions()
              .setType("file")
              .setFormat("properties")
              .setConfig(new JsonObject().put("path", configFile));

      log.info("Using config {}", configFile);

      return new ConfigRetrieverOptions().addStore(file);
    }

    val defaultConfigFile = ConfigHelper.class.getClassLoader()
            .getResource("conf/config.properties").getPath();

    ConfigStoreOptions file = new ConfigStoreOptions()
            .setType("file")
            .setFormat("properties")
            .setConfig(new JsonObject().put("path", defaultConfigFile));

    log.info("Using config {}", defaultConfigFile);

    return new ConfigRetrieverOptions().addStore(file);

  }

}
