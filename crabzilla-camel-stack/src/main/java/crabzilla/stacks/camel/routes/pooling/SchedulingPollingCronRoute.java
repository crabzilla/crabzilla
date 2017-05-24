package crabzilla.stacks.camel.routes.pooling;

import crabzilla.stack.util.StringHelper;
import lombok.NonNull;
import org.apache.camel.builder.RouteBuilder;

import javax.inject.Named;

public class SchedulingPollingCronRoute extends RouteBuilder {

	final String eventsChannelId;
	final String eventsCronPooling;

	public SchedulingPollingCronRoute(@NonNull String eventsChannelId,
                                    @NonNull @Named("scheduling.cron.polling") String eventsCronPooling) {
		this.eventsChannelId = eventsChannelId;
		this.eventsCronPooling = eventsCronPooling;
  }

	@Override
	public void configure() throws Exception {

    fromF("quartz2://scheduling/%s?cron=%s",
            eventsChannelId, StringHelper.camelizedCron(eventsCronPooling))
      .routeId("pool-scheduling-cron" + eventsChannelId)
      .startupOrder(10)
      .threads(1)
      .log("fired")
      .toF("direct:pool-scheduling-%s", eventsChannelId);

  }
}
