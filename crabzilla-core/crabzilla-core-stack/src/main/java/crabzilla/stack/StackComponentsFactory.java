package crabzilla.stack;

public interface StackComponentsFactory {

 EventRepository eventRepository() ;

 EventProjector eventsProjector() ;

 ProjectionRepository projectionRepository() ;

 // TODO SchedulingRepository;

}
