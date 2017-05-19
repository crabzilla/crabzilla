package crabzilla.model;

import java.util.Optional;
import java.util.function.Function;

public interface SchedulingMonitoringFn extends Function<Event, Optional<Command>> {

  Optional<Command> apply(Event event);

}

