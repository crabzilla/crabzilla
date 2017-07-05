package crabzilla.vertx.repositories;

import crabzilla.model.CommandScheduling;

import java.time.Instant;
import java.util.List;


// what: sagas monitoring events may emit/schedule new commands to itself/other aggregate roots
// how: events implementing CommandSchedulingEvent. This class will retrieve these scheduled commands

public interface SchedulingRepository {

  void schedule(CommandScheduling scheduling);

  List<CommandScheduling> listAllBefore(Instant instant, int seconds);

}
