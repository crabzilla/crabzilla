package io.github.crabzilla.vertx.helpers;

import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;

import java.io.File;

import static org.slf4j.LoggerFactory.getLogger;

public class ConfigHelper {

  static Logger log = getLogger(ConfigHelper.class);

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

    String defaultConfigFile = ConfigHelper.class.getClassLoader()
            .getResource("conf/config.properties").getPath();

    ConfigStoreOptions file = new ConfigStoreOptions()
            .setType("file")
            .setFormat("properties")
            .setConfig(new JsonObject().put("path", defaultConfigFile));

    log.info("Using config {}", defaultConfigFile);

    return new ConfigRetrieverOptions().addStore(file);

  }

}
