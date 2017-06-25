package crabzilla.vertx;

import crabzilla.vertx.repositories.VertxEventRepository;

import java.util.List;
import java.util.function.BiFunction;

public interface VertxBoundedContextComponentsFactory {

 VertxEventRepository eventRepository() ;

 EventProjector eventsProjector() ;

 BiFunction<Long, Integer, List<ProjectionData>> projectionRepository() ;

 // TODO SchedulingRepository;

}
