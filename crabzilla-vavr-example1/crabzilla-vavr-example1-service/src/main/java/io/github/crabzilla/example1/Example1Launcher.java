package io.github.crabzilla.example1;

import com.google.inject.Guice;
import com.google.inject.Inject;
import io.github.crabzilla.example1.customer.Customer;
import io.github.crabzilla.example1.customer.CustomerModule;
import io.github.crabzilla.vertx.entity.EntityCommandExecution;
import io.github.crabzilla.vertx.helpers.StringHelper;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static io.github.crabzilla.example1.customer.CustomerData.*;
import static io.github.crabzilla.vertx.helpers.ConfigHelper.cfgOptions;
import static io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME;
import static java.lang.System.setProperty;

@Slf4j
public class Example1Launcher {

  @Inject
  Map<String, Verticle> aggregateRootVerticles;

  public static void main(String[] args) throws Exception {

    setProperty(LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
    LoggerFactory.getLogger(LoggerFactory.class); // Required for Logback to work in Vertx

    Arrays.asList(args).forEach(s -> log.info("arg -> " + s));

    final OptionParser parser = new OptionParser();
    parser.accepts( "conf" ).withRequiredArg();
    parser.allowsUnrecognizedOptions();

    final OptionSet options = parser.parse( args);

    val configFile = (String) options.valueOf("conf");
    val vertx = Vertx.vertx();

    ConfigRetriever retriever = ConfigRetriever.create(vertx, cfgOptions(configFile));

    retriever.getConfig(ar -> {

      if (ar.failed()) {
        log.error("failed to load config", ar.cause());
        return;
      }

      JsonObject config = ar.result();
      log.info("config = {}", config.encodePrettily());

      val launcher = new Example1Launcher();
      val injector = Guice.createInjector(new Example1Module(vertx, config), new CustomerModule());

      injector.injectMembers(launcher);

      for (Map.Entry<String,Verticle> v: launcher.aggregateRootVerticles.entrySet()) {
        vertx.deployVerticle(v.getValue(), event -> log.info("Deployed {} ? {}", v.getKey(), event.succeeded()));
      }

      // a test
      launcher.justForTest(vertx);

    });

  }

  private void justForTest(Vertx vertx) {

    val customerId = new CustomerId(UUID.randomUUID().toString());
//    val customerId = new CustomerId("customer123");
    val createCustomerCmd = new CreateCustomer(UUID.randomUUID(), customerId, "a good customer");
    val options = new DeliveryOptions().setCodecName("EntityCommand");

    // create customer command
    vertx.eventBus().<EntityCommandExecution>send(StringHelper.commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {

      log.info("Successful create customer test? {}", asyncResult.succeeded());

      if (asyncResult.succeeded()) {

        log.info("Result: {}", asyncResult.result().body());

        val activateCustomerCmd = new ActivateCustomer(UUID.randomUUID(), createCustomerCmd.getTargetId(), "because I want it");

        // activate customer command
        vertx.eventBus().<EntityCommandExecution>send(StringHelper.commandHandlerId(Customer.class), activateCustomerCmd, options, asyncResult2 -> {

          log.info("Successful activate customer test? {}", asyncResult2.succeeded());

          if (asyncResult2.succeeded()) {
            log.info("Result: {}", asyncResult2.result().body());
          } else {
            log.info("Cause: {}", asyncResult2.cause());
            log.info("Message: {}", asyncResult2.cause().getMessage());
          }

        });

      } else {
        log.info("Cause: {}", asyncResult.cause());
        log.info("Message: {}", asyncResult.cause().getMessage());
      }

    });

  }

}
