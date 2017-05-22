package crabzilla.stack;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

// events implementing CommandScheduling
// sagas monitoring events and scheduling commands as needed
public interface SchedulingRepository<TX> {

  void schedule(Supplier<TX> transactionSupplier, CommandScheduling scheduling);

  List<CommandScheduling> listAllBefore(Instant instant, int seconds);

}
