package crabzilla.stack.vertx;


import com.github.benmanes.caffeine.cache.Cache;
import crabzilla.model.AggregateRoot;
import crabzilla.model.Snapshot;
import crabzilla.stack.EventRepository;
import crabzilla.stack.SnapshotFactory;
import crabzilla.stack.SnapshotMessage;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static crabzilla.stack.SnapshotMessage.LoadedFromEnum;

@Slf4j
public class CaffeinedSnapshotReaderFn<A extends AggregateRoot> implements Function<String, SnapshotMessage<A>> {

  private static final Logger logger = LoggerFactory.getLogger(CaffeinedSnapshotReaderFn.class);

  final Cache<String, Snapshot<A>> cache;
  final EventRepository eventRepository;
  final SnapshotFactory<A> snapshotFactory;

  @Inject
  CaffeinedSnapshotReaderFn(@NonNull Cache<String, Snapshot<A>> cache,
                            @NonNull EventRepository eventRepository,
                            @NonNull SnapshotFactory<A> snapshotFactory) {
    this.cache = cache;
    this.eventRepository = eventRepository;
    this.snapshotFactory = snapshotFactory;
  }

  public SnapshotMessage<A> apply(@NonNull final String id) {

    logger.debug("cache.get(id)", id);

    val wasEventRepoCalled = new AtomicBoolean(false);

    val cachedSnapshot = cache.get(id, s -> {
      logger.debug("cache.getInstance(id) does not contain anything for id {}. Will have to search on eventRepository", id);
      val dataFromDb = eventRepository.getAll(id);
      wasEventRepoCalled.set(true);
      return dataFromDb.map(snapshotFactory::createSnapshot).orElseGet(snapshotFactory::getEmptySnapshot);
    });

    if (wasEventRepoCalled.get()) { // hopefully all data was loaded from db
      return new SnapshotMessage<>(cachedSnapshot, LoadedFromEnum.FROM_DB);
    }

    if (cachedSnapshot == null || cachedSnapshot.isEmpty()) {
      return new SnapshotMessage<>(cachedSnapshot, LoadedFromEnum.FROM_CACHE);
    }

    val cachedVersion = cachedSnapshot.getVersion();

    logger.debug("id {} cached lastSnapshotData has version {}. will check if there any version beyond it",
            id, cachedVersion);

    val nonCachedSnapshotData = eventRepository.getAllAfterVersion(id, cachedVersion);

    if (nonCachedSnapshotData.isPresent()) {

      val nonCached = nonCachedSnapshotData.get();

      logger.debug("id {} found {} pending events. Last version is now {}",
              id, nonCached.getEvents().size(), nonCached.getVersion());

      val resultingSnapshot = snapshotFactory.applyNewEventsToSnapshot(cachedSnapshot, nonCached.getVersion(),
              nonCached.getEvents());

      return new SnapshotMessage<>(resultingSnapshot, LoadedFromEnum.FROM_BOTH);

    }

    return new SnapshotMessage<>(cachedSnapshot, LoadedFromEnum.FROM_CACHE);

  }

}
