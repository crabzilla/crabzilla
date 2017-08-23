package io.github.crabzilla.example1;

import com.google.inject.Guice;
import io.github.crabzilla.example1.customer.Customer;
import io.github.crabzilla.model.EntityCommand;
import io.github.crabzilla.stack.CommandExecution;
import io.github.crabzilla.vertx.verticles.EventsProjectionVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;

import static io.github.crabzilla.example1.customer.CustomerData.*;
import static io.github.crabzilla.stack.StringHelper.commandHandlerId;
import static io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME;
import static java.lang.System.setProperty;

@Slf4j
public class Example1Launcher {

  static Vertx vertx;

  @Inject
  Map<String, Verticle> aggregateRootVerticles;

  @Inject
  EventsProjectionVerticle<CustomerSummaryDao> projectionVerticle;

  public static void main(String args[]) throws InterruptedException {

    val launcher = new Example1Launcher();

    vertx = Vertx.vertx();

    setProperty (LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName ());
    LoggerFactory.getLogger (LoggerFactory.class); // Required for Logback to work in Vertx

    Guice.createInjector(new Example1Module(vertx)).injectMembers(launcher);

    for (Map.Entry<String,Verticle> v: launcher.aggregateRootVerticles.entrySet()) {
      vertx.deployVerticle(v.getValue(), event -> log.info("Deployed {} ? {}", v.getKey(), event.succeeded()));
    }

    vertx.deployVerticle(launcher.projectionVerticle, event -> log.info("Deployed ? {}", event.succeeded()));

    // a test
    launcher.justForTest();

  }

  private void justForTest() {

    val customerId = new CustomerId(UUID.randomUUID().toString());
//    val customerId = new CustomerId("customer123");
    val createCustomerCmd = new CreateCustomer(UUID.randomUUID(), customerId, "a good customer");
    val options = new DeliveryOptions().setCodecName(EntityCommand.class.getSimpleName());

    // create customer command
    vertx.eventBus().<CommandExecution>send(commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {

      log.info("Successful create customer test? {}", asyncResult.succeeded());

      if (asyncResult.succeeded()) {

        log.info("Result: {}", asyncResult.result().body());

        val activateCustomerCmd = new ActivateCustomer(UUID.randomUUID(), createCustomerCmd.getTargetId(),
                "because I want it");

        // activate customer command
        vertx.eventBus().<CommandExecution>send(commandHandlerId(Customer.class),
                activateCustomerCmd, options, asyncResult2 -> {

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
