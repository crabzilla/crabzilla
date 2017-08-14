package crabzilla.example1;

import com.google.inject.Guice;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.example1.aggregates.customer.CustomerId;
import crabzilla.example1.aggregates.customer.commands.ActivateCustomerCmd;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.vertx.CommandExecution;
import crabzilla.vertx.verticles.EventsProjectionVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;

import static crabzilla.vertx.util.StringHelper.commandHandlerId;
import static io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME;
import static java.lang.System.setProperty;

@Slf4j
public class Example1Launcher {

  @Inject
  Map<String, Verticle> aggregateRootVerticles;

  @Inject
  EventsProjectionVerticle projectionVerticle;

  static Vertx vertx;

  public static void main(String args[]) throws InterruptedException {

    val launcher = new Example1Launcher();
    val clusterManager = new HazelcastClusterManager();
    val options = new VertxOptions().setClusterManager(clusterManager);

    Vertx.clusteredVertx(options, (AsyncResult<Vertx> res) -> {

      if (res.succeeded()) {

        vertx = res.result();

        EventBus eventBus = vertx.eventBus();
        log.info("We now have a clustered event bus: " + eventBus);

        setProperty (LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName ());
        LoggerFactory.getLogger (LoggerFactory.class); // Required for Logback to work in Vertx

        Guice.createInjector(new Example1Module(vertx)).injectMembers(launcher);

        for (Map.Entry<String,Verticle> v: launcher.aggregateRootVerticles.entrySet()) {
          vertx.deployVerticle(v.getValue(), event -> log.info("Deployed {} ? {}", v.getKey(), event.succeeded()));
        }

        vertx.deployVerticle(launcher.projectionVerticle, event -> log.info("Deployed ? {}", event.succeeded()));

        // a test
        launcher.justForTest();

      } else {
        log.error("Failed: ", res.cause());
      }
    });

  }

  private void justForTest() {

    val customerId = new CustomerId(UUID.randomUUID().toString());
//    val customerId = new CustomerId("customer123");
    val createCustomerCmd = new CreateCustomerCmd(UUID.randomUUID(), customerId, "a good customer");
    val options = new DeliveryOptions().setCodecName("Command");

    // create customer command
    vertx.eventBus().<CommandExecution>send(commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {

      log.info("Successful create customer test? {}", asyncResult.succeeded());

      if (asyncResult.succeeded()) {

        log.info("Result: {}", asyncResult.result().body());

        val activateCustomerCmd = new ActivateCustomerCmd(UUID.randomUUID(), createCustomerCmd.getTargetId(), "because I want it");

        // activate customer command
        vertx.eventBus().<CommandExecution>send(commandHandlerId(Customer.class), activateCustomerCmd, options, asyncResult2 -> {

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
