package crabzilla.vertx;

import java.util.List;
import java.util.function.BiFunction;

public interface VertxBoundedContextComponentsFactory {

 EventProjector eventsProjector() ;

 BiFunction<Long, Integer, List<ProjectionData>> projectionRepository() ;

 // TODO SchedulingRepository;

}
