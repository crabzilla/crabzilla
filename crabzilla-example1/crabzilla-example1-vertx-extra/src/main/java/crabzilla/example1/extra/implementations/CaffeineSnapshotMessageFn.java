package crabzilla.example1.extra.implementations;


import com.github.benmanes.caffeine.cache.Cache;
import crabzilla.model.AggregateRoot;
import crabzilla.model.Snapshot;
import crabzilla.model.Version;
import crabzilla.stack.EventRepository;
import crabzilla.stack.model.SnapshotFactory;
import crabzilla.stack.model.SnapshotMessage;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static crabzilla.stack.model.SnapshotMessage.LoadedFromEnum;

@Slf4j
public class CaffeineSnapshotMessageFn<A extends AggregateRoot> implements Function<String, SnapshotMessage<A>> {

  final Cache<String, Snapshot<A>> cache;
  final EventRepository eventRepository;
  final SnapshotFactory<A> snapshotFactory;

  @Inject
  public CaffeineSnapshotMessageFn(@NonNull Cache<String, Snapshot<A>> cache,
                                   @NonNull EventRepository eventRepository,
                                   @NonNull SnapshotFactory<A> snapshotFactory) {
    this.cache = cache;
    this.eventRepository = eventRepository;
    this.snapshotFactory = snapshotFactory;
  }

  public SnapshotMessage<A> apply(@NonNull final String id) {

    log.debug("cache.get(id)", id);

    val wasEventRepoCalled = new AtomicBoolean(false);

    val snapshotFromCache = cache.get(id, s-> {
      log.debug("cache.getInstance(id) does not contain anything for id {}. Will have to search on eventRepository", id);
      val dataFromDb = eventRepository.getAll(id);
      wasEventRepoCalled.set(true);
      return dataFromDb.map(snapshotFactory::createSnapshot).orElseGet(snapshotFactory::getEmptySnapshot);
    });

    val cachedVersion = snapshotFromCache == null ? new Version(0) : snapshotFromCache.getVersion();

    log.debug("id {} cached lastSnapshotData has version {}. will check if there any version beyond it",
            id, cachedVersion);

    if (wasEventRepoCalled.get()) { // hopefully all data was loaded from db
      return new SnapshotMessage<>(snapshotFromCache, LoadedFromEnum.FROM_DB);
    }

    val nonCachedSnapshotData = eventRepository.getAllAfterVersion(id, cachedVersion);

    if (nonCachedSnapshotData.isPresent()) {

      val nonCached = nonCachedSnapshotData.get();

      log.debug("id {} found {} pending events. Last version is now {}",
              id, nonCached.getEvents().size(), nonCached.getVersion());

      val resultingSnapshot = snapshotFactory.applyNewEventsToSnapshot(snapshotFromCache, nonCached.getVersion(),
              nonCached.getEvents());

      return new SnapshotMessage<>(resultingSnapshot, LoadedFromEnum.FROM_BOTH);

    }

    return new SnapshotMessage<>(snapshotFromCache, LoadedFromEnum.FROM_CACHE);

  }

}
