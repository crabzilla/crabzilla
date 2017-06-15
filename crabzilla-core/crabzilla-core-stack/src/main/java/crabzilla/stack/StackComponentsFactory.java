package crabzilla.stack;

public interface StackComponentsFactory {

 EventRepository eventRepository() ;

 EventsProjector eventsProjector() ;

 ProjectionRepository projectionRepository() ;

 // TODO SchedulingRepository;

}
