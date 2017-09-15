package io.github.crabzilla.example1;

import com.google.inject.Guice;
import com.google.inject.Inject;
import io.github.crabzilla.example1.customer.Customer;
import io.github.crabzilla.example1.customer.CustomerModule;
import io.github.crabzilla.vertx.entity.EntityCommandExecution;
import io.github.crabzilla.vertx.helpers.StringHelper;
import io.github.crabzilla.vertx.projection.EventsProjectionVerticle;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AsyncResult;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
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
public class Example1LauncherHz {

  @Inject
  Map<String, Verticle> aggregateRootVerticles;

  @Inject
  EventsProjectionVerticle<CustomerSummaryDao> projectionVerticle;

  public static void main(String[] args) throws Exception {

    setProperty(LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
    LoggerFactory.getLogger(LoggerFactory.class); // Required for Logback to work in Vertx

    Arrays.asList(args).forEach(s -> System.out.println("arg -> " + s));

    final OptionParser parser = new OptionParser();
    parser.accepts( "conf" ).withRequiredArg();
    parser.allowsUnrecognizedOptions();

    final OptionSet options = parser.parse( args);

    val configFile = (String) options.valueOf("conf");
    val clusterManager = new HazelcastClusterManager();
    val vertxOptions = new VertxOptions().setClusterManager(clusterManager);

    Vertx.clusteredVertx(vertxOptions, (AsyncResult<Vertx> res) -> {

      if (res.succeeded()) {

        val vertx = res.result();

        ConfigRetriever retriever = ConfigRetriever.create(vertx, cfgOptions(configFile));

        retriever.getConfig(ar -> {
          if (ar.failed()) {
            log.error("failed to load config", ar.cause());
            return;
          }

          JsonObject config = ar.result();
          log.info("config = {}", config.encodePrettily());

          val launcher = new Example1LauncherHz();
          val injector = Guice.createInjector(new Example1Module(vertx, config),  new CustomerModule());

          injector.injectMembers(launcher);

          for (Map.Entry<String,Verticle> v: launcher.aggregateRootVerticles.entrySet()) {
            vertx.deployVerticle(v.getValue(), event -> log.info("Deployed {} ? {}", v.getKey(), event.succeeded()));
          }

          vertx.deployVerticle(launcher.projectionVerticle, event -> log.info("Deployed {} ? {}", "projectionVerticle", event.succeeded()));

          // a test
          launcher.justForTest(vertx);

        });

      } else {
        log.error("Failed: ", res.cause());
      }
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
