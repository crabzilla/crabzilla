package crabzilla.vertx;

import crabzilla.vertx.repositories.VertxUnitOfWorkRepository;

import java.util.List;
import java.util.function.BiFunction;

public interface VertxBoundedContextComponentsFactory {

 VertxUnitOfWorkRepository eventRepository() ;

 EventProjector eventsProjector() ;

 BiFunction<Long, Integer, List<ProjectionData>> projectionRepository() ;

 // TODO SchedulingRepository;

}
