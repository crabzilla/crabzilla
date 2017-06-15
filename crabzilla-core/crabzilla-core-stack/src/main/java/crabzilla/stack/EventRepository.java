package crabzilla.stack;

import crabzilla.model.Event;
import crabzilla.model.UnitOfWork;
import crabzilla.model.Version;
import lombok.Value;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository {

	Long append(UnitOfWork unitOfWork);

	Optional<UnitOfWork> get(UUID uowId);

  Optional<SnapshotData> getAll(String aggregateRootId);

	Optional<SnapshotData> getAllAfterVersion(String aggregateRootId, Version version);

  class DbConcurrencyException extends RuntimeException {

    public DbConcurrencyException(String s) {
      super(s);
    }

  }

  @Value
  class SnapshotData implements Serializable {

    Version version;
    List<Event> events;

  }

}
