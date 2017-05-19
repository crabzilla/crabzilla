package crabzilla.stack1.routes;

import crabzilla.model.EventsProjector;
import crabzilla.stack.ProjectionData;
import lombok.NonNull;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.AbstractListAggregationStrategy;
import org.apache.camel.processor.aggregate.MemoryAggregationRepository;
import org.apache.camel.spi.IdempotentRepository;

import java.util.List;

public class EventsProjectionRoute extends RouteBuilder {

  final EventsProjector eventsdao;
  final IdempotentRepository idempotentRepo;
  final boolean multipleConsumers;
  final long completionInterval;
  final int completionSize;

  public EventsProjectionRoute(@NonNull EventsProjector eventsdao,
                               @NonNull IdempotentRepository idempotentRepo,
                               boolean multipleConsumers, long completionInterval, int completionSize) {
    this.eventsdao = eventsdao;
    this.idempotentRepo = idempotentRepo;
    this.multipleConsumers = multipleConsumers;
    this.completionInterval = completionInterval;
    this.completionSize = completionSize;
  }

	@Override
	public void configure() throws Exception {

    fromF("seda:%s-events?multipleConsumers=%b", eventsdao.getEventsChannelId(), multipleConsumers)
      .routeId("st-events-projector" + eventsdao.getEventsChannelId())
      .threads(1)
      .process(e -> {
        final ProjectionData uow = e.getIn().getBody(ProjectionData.class);
        e.getOut().setHeader(HeadersConstants.UNIT_OF_WORK_ID, uow.getUowId());
        e.getOut().setBody(uow);
      })
      .idempotentConsumer(header(HeadersConstants.UNIT_OF_WORK_ID)).messageIdRepository(idempotentRepo)
      .aggregate(body()).aggregationStrategy(new Strategy())
        .completionInterval(completionInterval).completionSize(completionSize)
        .aggregationRepository(new MemoryAggregationRepository())
      .log("will process ${body.class.name}")
      .process(e -> {
        final List<ProjectionData> list = e.getIn().getBody(List.class);
        eventsdao.handle(list);
      })
      .log("after st-events-projector: ${body}");

    // TODO update last uow sequence

	}

  private class Strategy extends AbstractListAggregationStrategy<ProjectionData> {
    @Override
    public ProjectionData getValue(Exchange exchange) {
      return exchange.getIn().getBody(ProjectionData.class);
    }
  }

}


