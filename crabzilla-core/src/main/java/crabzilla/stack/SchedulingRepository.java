package crabzilla.stack;

import crabzilla.model.CommandScheduling;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;


// what: sagas monitoring events may emit/schedule new commands to  other aggregate roots
// how: events implementing CommandScheduling. This class will retrieve scheduled commands

public interface SchedulingRepository<TX> {

  void schedule(Supplier<TX> transactionSupplier, CommandScheduling scheduling);

  List<CommandScheduling> listAllBefore(Instant instant, int seconds);

}
