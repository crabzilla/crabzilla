package crabzilla.stack;

import crabzilla.model.ProjectionData;

import java.util.List;
import java.util.function.BiFunction;

public interface StackComponentsFactory {

 EventRepository eventRepository() ;

 EventProjector eventsProjector() ;

 BiFunction<Long, Integer, List<ProjectionData>> projectionRepository() ;

 // TODO SchedulingRepository;

}
