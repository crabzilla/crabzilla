package crabzilla.stack.vertx.sql;


import com.github.benmanes.caffeine.cache.Cache;
import crabzilla.model.AggregateRoot;
import crabzilla.model.Snapshot;
import crabzilla.stack.EventRepository;
import crabzilla.stack.SnapshotFactory;
import crabzilla.stack.SnapshotMessage;
import crabzilla.stack.SnapshotReaderFn;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

import static crabzilla.stack.SnapshotMessage.LoadedFromEnum;

@Slf4j
public class CaffeinedSnapshotReaderFn<A extends AggregateRoot> implements SnapshotReaderFn<A> {

  private static final Logger logger = LoggerFactory.getLogger(CaffeinedSnapshotReaderFn.class);

  final Cache<String, Snapshot<A>> cache;
  final EventRepository eventRepository;
  final SnapshotFactory<A> snapshotFactory;

  public CaffeinedSnapshotReaderFn(@NonNull Cache<String, Snapshot<A>> cache,
                                   @NonNull EventRepository eventRepository,
                                   @NonNull SnapshotFactory<A> snapshotFactory) {
    this.cache = cache;
    this.eventRepository = eventRepository;
    this.snapshotFactory = snapshotFactory;
  }

  @Override
  public SnapshotMessage<A> getSnapshotMessage(@NonNull final String id) {

    logger.debug("cache.get(id)", id);

    val wasDaoCalled = new AtomicBoolean(false);

    val cachedSnapshot = cache.get(id, s -> {
      logger.debug("cache.getInstance(id) does not contain anything for this id. Will have to search on eventRepository", id);
      val dataFromDb = eventRepository.getAll(id);
      wasDaoCalled.set(true);
      return dataFromDb.map(snapshotFactory::createSnapshot).orElseGet(snapshotFactory::getEmptySnapshot);
    });

    if (wasDaoCalled.get()) { // hopefully all data was loaded from db
      return new SnapshotMessage<>(cachedSnapshot, LoadedFromEnum.FROM_DB);
    }

    if (cachedSnapshot.isEmpty()) {
      return new SnapshotMessage<>(cachedSnapshot, LoadedFromEnum.FROM_CACHE);
    }

    val cachedVersion = cachedSnapshot.getVersion();

    logger.debug("id {} cached lastSnapshotData has version {}. will check if there any version beyond it",
            id, cachedVersion);

    val nonCachedSnapshotData = eventRepository.getAllAfterVersion(id, cachedVersion);

    if (nonCachedSnapshotData.isPresent()) {

      val resultingSnapshotData = nonCachedSnapshotData.get();

      logger.debug("id {} found {} pending events. Last version is now {}",
              id, resultingSnapshotData.getEvents().size(), resultingSnapshotData.getVersion());

      val resultingSnapshot = snapshotFactory.createSnapshot(cachedSnapshot, resultingSnapshotData.getVersion(),
              resultingSnapshotData.getEvents());

      return new SnapshotMessage<>(resultingSnapshot, LoadedFromEnum.FROM_BOTH);

    }

    return new SnapshotMessage<>(cachedSnapshot, LoadedFromEnum.FROM_CACHE);

  }

}
