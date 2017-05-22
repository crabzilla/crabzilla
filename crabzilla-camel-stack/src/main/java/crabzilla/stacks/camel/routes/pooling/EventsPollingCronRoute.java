package crabzilla.stacks.camel.routes.pooling;

import crabzilla.util.StringHelper;
import lombok.NonNull;
import org.apache.camel.builder.RouteBuilder;

import javax.inject.Named;

public class EventsPollingCronRoute extends RouteBuilder {

	final String eventsChannelId;
	final String eventsCronPooling;

	public EventsPollingCronRoute(@NonNull String eventsChannelId,
																@NonNull @Named("events.cron.polling") String eventsCronPooling) {
		this.eventsChannelId = eventsChannelId;
		this.eventsCronPooling = eventsCronPooling;
  }

	@Override
	public void configure() throws Exception {

    fromF("quartz2://events/%s?cron=%s",
            eventsChannelId, StringHelper.camelizedCron(eventsCronPooling))
      .routeId("pool-events-cron" + eventsChannelId)
      .startupOrder(10)
      .threads(1)
      .log("fired")
      .toF("direct:pool-events-%s", eventsChannelId);

  }
}
