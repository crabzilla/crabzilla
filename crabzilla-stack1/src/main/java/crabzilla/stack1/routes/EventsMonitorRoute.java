package crabzilla.stack1.routes;

import com.google.gson.Gson;
import crabzilla.model.SchedulingMonitoringFn;
import lombok.NonNull;
import org.apache.camel.builder.RouteBuilder;

public class EventsMonitorRoute extends RouteBuilder {

  private static final String TARGET_ENDPOINT = "TARGET_ENDPOINT";

  @NonNull final String eventsChannelId;
  @NonNull final SchedulingMonitoringFn schedulingMonitoringFn;
  @NonNull final Gson gson;

  public EventsMonitorRoute(@NonNull String eventsChannelId,
                            @NonNull SchedulingMonitoringFn schedulingMonitoringFn,
                            @NonNull Gson gson) {
    this.eventsChannelId = eventsChannelId;
    this.schedulingMonitoringFn = schedulingMonitoringFn;
    this.gson = gson;
  }

  @Override
  public void configure() throws Exception {

//    fromF("seda:%s-events?multipleConsumers=true", eventsChannelId)
//      .routeId(eventsChannelId + "-events-monitor")
//      .process( e-> {
//        final UnitOfWork unitOfWork = e.getIn().getBody(UnitOfWork.class);
//        e.getOut().setBody(unitOfWork.getEvents(), List.class);
//        e.getOut().setHeaders(e.getIn().getHeaders());
//      })
//      .split(body())
//      .process(e -> {
//        final Event event = e.getIn().getBody(Event.class);
//        final Optional<SchedulingMonitoringFn.CommandMessage> command = schedulingMonitoringFn.apply(event);
//        e.getOut().setBody(command.isPresent() ? command.get() : null);
//        e.getOut().setHeaders(e.getIn().getHeaders());
//      })
//      .filter(bodyAs(SchedulingMonitoringFn.CommandMessage.class).isNotNull())
//      .process(e -> {
//        final SchedulingMonitoringFn.CommandMessage msg = e.getIn().getBody(SchedulingMonitoringFn.CommandMessage.class);
//        final String cmdAsJson = gson.toJson(msg.getCommand(), Command.class);
//        final String targetEndpoint = String.format("direct:handle-%s", commandId(msg.getCommandClass()));
//        e.getOut().setBody(cmdAsJson, String.class);
//        e.getOut().setHeaders(e.getIn().getHeaders());
//        e.getOut().setHeader(TARGET_ENDPOINT, targetEndpoint);
//      })
//      .recipientList(header(TARGET_ENDPOINT))
//      ;
  }

}
