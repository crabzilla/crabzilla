package crabzilla.example1;

import com.google.gson.Gson;
import com.google.inject.Guice;
import crabzilla.UnitOfWork;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.example1.aggregates.customer.CustomerId;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.stacks.vertx.codecs.CommandCodec;
import crabzilla.stacks.vertx.verticles.CommandHandlerVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import java.util.UUID;

import static crabzilla.util.StringHelper.commandHandlerId;

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

    Guice.createInjector(new Example1VertxModule()).injectMembers(launcher);

    launcher.vertx.deployVerticle(launcher.comdVerticle, event -> {
      log.info("Deployed ? {}", event.succeeded());
    });

    launcher.test();

  }

  private void test() {

    val customerId = new CustomerId(UUID.randomUUID().toString());

    val createCustomerCmd = new CreateCustomerCmd(UUID.randomUUID(), customerId, "customer2");

    val options = new DeliveryOptions().setCodecName(new CommandCodec(gson).name());

    vertx.eventBus().send(commandHandlerId(Customer.class), createCustomerCmd, options, asyncResult -> {
      log.info("Successful test? {}", asyncResult.succeeded());
      log.info("Result: {}", asyncResult.result().body());

      log.info("Matches command ? {}", ((UnitOfWork)asyncResult.result().body()).getCommand().equals(createCustomerCmd));

    });

  }

}
