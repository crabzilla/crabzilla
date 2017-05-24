package crabzilla.stacks.camel.routes.pooling;

import crabzilla.model.EventsProjector;
import crabzilla.model.ProjectionData;
import crabzilla.stack.EventRepository;
import lombok.NonNull;
import org.apache.camel.Predicate;
import org.apache.camel.builder.RouteBuilder;

import javax.inject.Named;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.camel.builder.PredicateBuilder.not;

public class EventsPollingRoute extends RouteBuilder {

	final EventRepository repo;
	final EventsProjector eventsProjectorDao;
	final int eventsBackoffFailuresThreshold;
  final int eventsBackoffIdlesThreshold;
  final int eventsBackoffMultiplier;
  final int eventsMaxRowsPerPooling;
	public final AtomicInteger failures = new AtomicInteger();
	public final AtomicInteger idles = new AtomicInteger();
  final AtomicInteger backoffCount = new AtomicInteger();

  static final String RESULT_SIZE_HEADER = "RESULT_SIZE_HEADER";

	public EventsPollingRoute(@NonNull EventRepository repo,
                            @NonNull EventsProjector eventsProjectorDao,
                            @NonNull @Named("events.backoff.failure.threshold") Integer eventsBackoffFailureThreshold,
                            @NonNull @Named("events.backoff.idle.threshold") Integer eventsBackoffIdleThreshold,
                            @NonNull @Named("events.backoff.multiplier") Integer eventsBackoffMultiplier,
                            @NonNull @Named("events.max.rows.query") Integer eventsMaxRowsPerPooling) {
		this.repo = repo;
    this.eventsProjectorDao = eventsProjectorDao;
    this.eventsBackoffFailuresThreshold = eventsBackoffFailureThreshold;
    this.eventsBackoffIdlesThreshold = eventsBackoffIdleThreshold;
    this.eventsBackoffMultiplier = eventsBackoffMultiplier;
    this.eventsMaxRowsPerPooling = eventsMaxRowsPerPooling;
  }

	@Override
	public void configure() throws Exception {

    final Predicate hasReachedAnyThreshold = exchange -> failures.get() >= eventsBackoffFailuresThreshold ||
            idles.get() >= eventsBackoffIdlesThreshold;

    final Predicate backoffCountBiggerThanZero = exchange -> backoffCount.get() > 0;

    fromF("direct:pool-events-%s", eventsProjectorDao.getEventsChannelId())
      .routeId("pool-events-" + eventsProjectorDao.getEventsChannelId())
      .log("before -> ${body}")
      .choice()
        .when(hasReachedAnyThreshold)
          .toF("direct:pool-events-open-cb-%s", eventsProjectorDao.getEventsChannelId())
      .end()
      .doTry()
        .toF("direct:pool-events-perform-%s", eventsProjectorDao.getEventsChannelId())
      .doCatch(Throwable.class)
        .setHeader("msg", method(this, "newFailure()"))
        .log("Failure pooling operations incremented to ${header.msg}")
      .end()
      .log("after -> ${body}")
    ;

    fromF("direct:pool-events-open-cb-%s", eventsProjectorDao.getEventsChannelId())
      .routeId("pool-events-open-cb-" + eventsProjectorDao.getEventsChannelId())
      .process(e -> {
        failures.set(0); idles.set(0);
        backoffCount.updateAndGet(operand -> operand + eventsBackoffMultiplier);
      })
    .log("circuit breaker is now open")
    ;

    fromF("direct:pool-events-perform-%s", eventsProjectorDao.getEventsChannelId())
      .routeId("pool-events-perform-" + eventsProjectorDao.getEventsChannelId())
      .errorHandler(noErrorHandler())
      .choice()
        .when(backoffCountBiggerThanZero)
          .setHeader("msg", method(this, "ranBackoff()"))
          .log("backoffCount was decremented to ${header.msg}")
            .choice()
              .when(not(backoffCountBiggerThanZero))
                .log("circuit breaker is now off")
                .stop()
            .otherwise()
                .stop()
            .endChoice()
        .endChoice()
      .end()
      .log("--> will pool")
      .process(e -> {
        final List<ProjectionData> unitsOfWork =
                repo.getAllSince(eventsProjectorDao.getLastUowSeq(), eventsMaxRowsPerPooling);
        e.getOut().setHeader(RESULT_SIZE_HEADER, unitsOfWork.size());
        e.getOut().setBody(unitsOfWork);
      })
      .log("--> ${body}")
      .choice()
        .when(header(RESULT_SIZE_HEADER).isEqualTo(0))
          .setHeader("msg", method(this, "newIdle()"))
          .log("Failure pooling operations incremented to ${header.msg}")
        .otherwise()
          .log("Found ${header.RESULT_SIZE_HEADER} units of work to project")
          .split(body())
          .toF("seda:%s-events", eventsProjectorDao.getEventsChannelId())
      .end()
    ;

  }

  public Integer newIdle() {
	  return idles.incrementAndGet();
  }

  public Integer newFailure() {
    return failures.incrementAndGet();
  }

  public Integer ranBackoff() {
    return backoffCount.decrementAndGet();
  }

}
