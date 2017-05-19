package crabzilla.stack1.routes;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import crabzilla.UnitOfWork;
import crabzilla.Version;
import crabzilla.example1.Example1Module;
import crabzilla.example1.aggregates.CustomerModule;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.example1.aggregates.customer.CustomerCmdHandler;
import crabzilla.example1.aggregates.customer.CustomerId;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import crabzilla.model.Command;
import crabzilla.stack.EventRepository;
import crabzilla.stack.Snapshot;
import crabzilla.stack.SnapshotReader;
import lombok.val;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.processor.idempotent.MemoryIdempotentRepository;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class CommandHandlingRouteTest extends CamelTestSupport {

  static final Injector injector = Guice.createInjector(new CustomerModule(), new Example1Module());

  @EndpointInject(uri = "mock:result")
  protected MockEndpoint resultEndpoint;

  @Produce(uri = "direct:start")
  protected ProducerTemplate template;

  @Inject
  Supplier<Customer> supplier;
  @Inject
  CustomerCmdHandler commandHandlerFn;
  @Inject
  Gson gson;

  @Mock
  SnapshotReader<CustomerId, Customer> snapshotReader;
  @Mock
  EventRepository eventRepository;

  @Before
  public void init() throws Exception {
    injector.injectMembers(this);
    MockitoAnnotations.initMocks(this);
    val route = new CommandHandlingRoute<>(Customer.class, snapshotReader, commandHandlerFn, eventRepository, gson,
            new MemoryIdempotentRepository());
    context.addRoutes(route);
  }

  @After
  public void afterRun() throws Exception {
  }

  @Test
  public void valid_command_must_return_valid_unit_of_work() throws EventRepository.DbConcurrencyException, InterruptedException {

    val customerId = new CustomerId("customer#1");

    when(snapshotReader.getSnapshot(eq(customerId)))
            .thenReturn(new Snapshot<>(supplier.get(), new Version(0)));

    val createCustomerCmd = new CreateCustomerCmd(UUID.randomUUID(), customerId, "customer1");

    val asJson = gson.toJson(createCustomerCmd, Command.class);

    template.requestBody(asJson);

    verify(snapshotReader).getSnapshot(eq(customerId));

    val expectedEvent = new CustomerCreated(customerId, "customer1");
    val expectedUow = UnitOfWork.of(createCustomerCmd, new Version(1), Arrays.asList(expectedEvent));

    ArgumentCaptor<UnitOfWork> argument = ArgumentCaptor.forClass(UnitOfWork.class);

    verify(eventRepository).append(argument.capture());

    val resultingUow = argument.getValue();

    assertEquals(resultingUow.getCommand(), expectedUow.getCommand());
    assertEquals(resultingUow.getEvents(), expectedUow.getEvents());
    assertEquals(resultingUow.getVersion(), expectedUow.getVersion());

    verifyNoMoreInteractions(snapshotReader, eventRepository);

    val expectedBody = gson.toJson(resultingUow);

    resultEndpoint.expectedMessageCount(1);

    val receivedBody = resultEndpoint.getExchanges().get(0).getIn().getBody(String.class);

    assertEquals(expectedBody, receivedBody);

    resultEndpoint.assertIsSatisfied();

    assertEquals(resultEndpoint.getExchanges().get(0).getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), 201);
  }

  @Test
  public void invalid_command_must_return_error() throws InterruptedException {

    val customerId = new CustomerId("customer#1");
    val currentCustomer = Customer.of(customerId, "customer1", false, null);

    when(snapshotReader.getSnapshot(eq(customerId)))
            .thenReturn(new Snapshot<>(currentCustomer, new Version(1)));

    val createCustomerCmd = new CreateCustomerCmd(UUID.randomUUID(), customerId, "customer1");

    val asJson = gson.toJson(createCustomerCmd, Command.class);

    template.requestBody(asJson);

    verify(snapshotReader).getSnapshot(eq(customerId));

    verifyNoMoreInteractions(snapshotReader, eventRepository);

    val expectedBody = gson.toJson(Arrays.asList("customer already created"), List.class);

    resultEndpoint.expectedMessageCount(1);

    assertEquals(resultEndpoint.getExchanges().get(0).getIn().getHeader(Exchange.HTTP_RESPONSE_CODE), 400);

    val receivedBody = resultEndpoint.getExchanges().get(0).getIn().getBody(String.class);

    assertEquals(expectedBody, receivedBody);

    resultEndpoint.assertIsSatisfied();

  }

  @Override
  protected RouteBuilder createRouteBuilder() {
    return new RouteBuilder() {
      public void configure() {
        from("direct:start")
                .streamCaching()
                .to("direct:handle-cmd-customer")
                //        .log("** final result ${body}")
                .to("mock:result");
      }
    };
  }

}
