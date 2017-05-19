package crabzilla.stack1.routes;

import crabzilla.example1.aggregates.customer.CustomerId;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import crabzilla.model.EventsProjector;
import crabzilla.stack.EventRepository;
import crabzilla.stack.ProjectionData;
import lombok.val;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class EventsPollingRouteTest extends CamelTestSupport {

  final static String eventsChannelId = "channelExample1";

  @Produce(uri = "direct:start")
  ProducerTemplate template;

  @EndpointInject(uri = "mock:result")
  MockEndpoint resultEndpoint;

  int events_max_rows_query = 10;
  int events_backoff_failures_threshold = 3;
  int events_backoff_idle_threshold = 3;
  int events_backoff_multiplier = 2;

  @Before
  public void init() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void single_events_list() throws Exception {

    val repoMock = mock(EventRepository.class, withSettings().verboseLogging());
    val eventDaoMock = mock(EventsProjector.class, withSettings().verboseLogging());
    when(eventDaoMock.getEventsChannelId()).thenReturn(eventsChannelId);
    val route = new EventsPollingRoute(repoMock, eventDaoMock,
            events_backoff_failures_threshold,
            events_backoff_idle_threshold,
            events_backoff_multiplier,
            events_max_rows_query);
    context.addRoutes(route);
    val uowId = "uow#1";
    val aggregateRootId = "id-1";
    val cmd1 = new CreateCustomerCmd(UUID.randomUUID(), new CustomerId("c1"), "customer1");
    val event1 = new CustomerCreated(cmd1.getTargetId(), cmd1.getName());

    final List<ProjectionData> tuplesList =
            Arrays.asList(new ProjectionData(uowId, 1L, aggregateRootId, Arrays.asList(event1)));

    when(eventDaoMock.getLastUowSeq()).thenReturn(0L);

    when(repoMock.getAllSince(eq(0L), eq(10))).thenReturn(tuplesList);

//    resultEndpoint.expectedBodiesReceived(tuplesList);

    resultEndpoint.expectedMessageCount(1);

    template.sendBody(true);

    resultEndpoint.assertIsSatisfied(100);

    verify(eventDaoMock).getLastUowSeq();

    verify(repoMock).getAllSince(eq(0L), eq(10));

    verifyNoMoreInteractions(repoMock);

    final List<ProjectionData> result = resultEndpoint.getExchanges().get(0).getIn().getBody(List.class);

    assertEquals(result,  tuplesList);

  }

  @Test
  public void when_failure_it_must_increase_failures() throws Exception {

    val repoMock = mock(EventRepository.class); // , withSettings().verboseLogging());
    val eventDaoMock = mock(EventsProjector.class, withSettings().verboseLogging());
    when(eventDaoMock.getEventsChannelId()).thenReturn(eventsChannelId);
    val route = new EventsPollingRoute(repoMock, eventDaoMock,
            events_backoff_failures_threshold,
            events_backoff_idle_threshold,
            events_backoff_multiplier,
            events_max_rows_query);
    context.addRoutes(route);

    when(eventDaoMock.getLastUowSeq())
            .thenThrow(new RuntimeException("fail 1"), new RuntimeException("fail 2"), new RuntimeException("fail 2"))
    ;

    template.requestBody(true);

    verify(eventDaoMock).getLastUowSeq();

    AssertionsForInterfaceTypes.assertThat(route.failures.get()).isEqualTo(1);

    verifyNoMoreInteractions(repoMock);

  }

  @Test
  public void when_idle_it_must_increase_idles() throws Exception {

    val repoMock = mock(EventRepository.class); // , withSettings().verboseLogging());
    val eventDaoMock = mock(EventsProjector.class, withSettings().verboseLogging());
    when(eventDaoMock.getEventsChannelId()).thenReturn(eventsChannelId);
    val route = new EventsPollingRoute(repoMock, eventDaoMock,
            events_backoff_failures_threshold,
            events_backoff_idle_threshold,
            events_backoff_multiplier,
            events_max_rows_query);
    context.addRoutes(route);

    when(eventDaoMock.getLastUowSeq()).thenReturn(0L);

    when(repoMock.getAllSince(eq(0L), eq(10))).thenReturn(Arrays.asList());

    template.requestBody(true);

    verify(eventDaoMock).getLastUowSeq();

    verify(repoMock).getAllSince(eq(0L), eq(10));

    AssertionsForInterfaceTypes.assertThat(route.idles.get()).isEqualTo(1);

    verifyNoMoreInteractions(repoMock);

  }

  @Test
  public void after_3_failures_it_must_skip_2_pools_and_then_work() throws Exception {

    val repoMock = mock(EventRepository.class); //, withSettings().verboseLogging());
    val eventDaoMock = mock(EventsProjector.class, withSettings().verboseLogging());
    when(eventDaoMock.getEventsChannelId()).thenReturn(eventsChannelId);
    val route = new EventsPollingRoute(repoMock, eventDaoMock,
            events_backoff_failures_threshold,
            events_backoff_idle_threshold,
            events_backoff_multiplier,
            events_max_rows_query);
    context.addRoutes(route);

    val uowId = "uow#1";
    val aggregateRootId = "id-1";
    val cmd1 = new CreateCustomerCmd(UUID.randomUUID(), new CustomerId("c1"), "customer1");
    val event1 = new CustomerCreated(cmd1.getTargetId(), cmd1.getName());

    final List<ProjectionData> tuplesList =
            Arrays.asList(new ProjectionData(uowId, 1L, aggregateRootId, Arrays.asList(event1)));

    when(eventDaoMock.getLastUowSeq()).thenReturn(0L, 0L, 0L, 0L, 0L, 0L, 0L);

    when(repoMock.getAllSince(eq(0L), eq(10)))
            .thenThrow(new RuntimeException("fail 1"), new RuntimeException("fail 2"), new RuntimeException("fail 2"))
            .thenReturn(tuplesList)

    ;

    resultEndpoint.expectedBodiesReceived(1, 2, 3, tuplesList);

    // force circuit breaker to open
    template.sendBody(1);
    template.sendBody(2);
    template.sendBody(3);

    // retry and skip 2 times and close the circuit breaker
    template.sendBody(4);
    template.sendBody(5);

    // this should work
    template.sendBody(6);

    resultEndpoint.assertIsSatisfied(1000);

    verify(eventDaoMock, times(4)).getLastUowSeq();

    verify(repoMock, times(4)).getAllSince(eq(0L), eq(10));

    verifyNoMoreInteractions(repoMock);

  }

  @Override
  protected RouteBuilder createRouteBuilder() throws Exception {

    return new RouteBuilder() {
      @Override
      public void configure() throws Exception {
        from("direct:start")
                .toF("direct:pool-events-%s", eventsChannelId)
                .log("*** from pooling -> type: ${body.getClass().getName()} value: ${body}")
                .to("mock:result");
      }
    };

  }

}