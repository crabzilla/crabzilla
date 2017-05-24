package crabzilla.stack;

import crabzilla.model.ProjectionData;
import crabzilla.model.UnitOfWork;
import crabzilla.model.Version;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository {

	void append(UnitOfWork unitOfWork) throws DbConcurrencyException;

	Optional<UnitOfWork> get(UUID uowId);

	List<ProjectionData> getAllSince(long sinceUowSequence, int maxResultSize);

	Optional<SnapshotData> getAll(String aggregateRootId);

	Optional<SnapshotData> getAllAfterVersion(String aggregateRootId, Version version);

  class DbConcurrencyException extends RuntimeException {

    public DbConcurrencyException(String s) {
      super(s);
    }

  }
}
