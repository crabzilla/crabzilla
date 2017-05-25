package crabzilla.example1;

import com.google.gson.Gson;
import com.google.inject.Guice;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.example1.aggregates.customer.CustomerId;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.model.UnitOfWork;
import crabzilla.stack.vertx.codecs.gson.CommandCodec;
import crabzilla.stack.vertx.verticles.CommandHandlerVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import java.util.UUID;

import static crabzilla.stack.util.StringHelper.commandHandlerId;
import static io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME;
import static java.lang.System.setProperty;

@Slf4j
public class Example1VertxLauncher {

  @Inject
  CommandHandlerVerticle<Customer> comdVerticle;

  @Inject
  Vertx vertx;

  @Inject
  Gson gson;

  public static void main(String args[]) throws InterruptedException {

    final Example1VertxLauncher launcher = new Example1VertxLauncher();

    ClusterManager mgr = new HazelcastClusterManager();

    VertxOptions options = new VertxOptions().setClusterManager(mgr);

    Vertx.clusteredVertx(options, res -> {

      if (res.succeeded()) {

        Vertx vertx = res.result();
        EventBus eventBus = vertx.eventBus();
        System.out.println("We now have a clustered event bus: " + eventBus);

        setProperty (LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName ());
        LoggerFactory.getLogger (LoggerFactory.class); // Required for Logback to work in Vertx

        Guice.createInjector(new Example1VertxModule(vertx)).injectMembers(launcher);

        launcher.vertx.deployVerticle(launcher.comdVerticle, event -> log.info("Deployed ? {}", event.succeeded()));

        // a test
        launcher.postNewCustomerJustForTest();

      } else {
        System.out.println("Failed: " + res.cause());
      }
    });

  }

  private void postNewCustomerJustForTest() {

    val customerId = new CustomerId(UUID.randomUUID().toString());

    val createCustomerCmd = new CreateCustomerCmd(UUID.randomUUID(), customerId, "customer2");

    val options = new DeliveryOptions().setCodecName(new CommandCodec(gson).name());

    vertx.eventBus().send(commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {

      log.info("Successful postNewCustomerJustForTest? {}", asyncResult.succeeded());

      if (asyncResult.succeeded()) {
        log.info("Result: {}", asyncResult.result().body());
        log.info("Matches command ? {}", ((UnitOfWork)asyncResult.result().body()).getCommand().equals(createCustomerCmd));
      } else {
        log.info("Cause: {}", asyncResult.cause());
        log.info("Message: {}", asyncResult.cause().getMessage());
      }

    });

  }

}
