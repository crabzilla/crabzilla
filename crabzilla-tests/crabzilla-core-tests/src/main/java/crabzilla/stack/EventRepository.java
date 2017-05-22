package crabzilla.stack;

import crabzilla.UnitOfWork;
import crabzilla.Version;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository {

	void append(UnitOfWork unitOfWork) throws DbConcurrencyException;

	Optional<UnitOfWork> get(UUID uowId);

	List<ProjectionData> getAllSince(long sinceUowSequence, int maxResultSize);

	SnapshotData getAll(String aggregateRootId);

	SnapshotData getAllAfterVersion(String aggregateRootId, Version version);

  class DbConcurrencyException extends RuntimeException {

    public DbConcurrencyException(String s) {
      super(s);
    }

  }
}
