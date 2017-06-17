package crabzilla.stack.vertx;


import crabzilla.model.AggregateRoot;
import crabzilla.stack.EventRepository;
import crabzilla.stack.model.SnapshotFactory;
import crabzilla.stack.model.SnapshotMessage;
import io.vertx.core.shareddata.LocalMap;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import java.util.function.Function;

import static crabzilla.stack.model.SnapshotMessage.LoadedFromEnum;

@Slf4j
public class VertxSnapshotMessageFn<A extends AggregateRoot> implements Function<String, SnapshotMessage<A>> {

  final LocalMap<String, ShareableSnapshot<A>> cache;
  final EventRepository eventRepository;
  final SnapshotFactory<A> snapshotFactory;

  @Inject
  public VertxSnapshotMessageFn(@NonNull LocalMap<String, ShareableSnapshot<A>> cache,
                                @NonNull EventRepository eventRepository,
                                @NonNull SnapshotFactory<A> snapshotFactory) {
    this.cache = cache;
    this.eventRepository = eventRepository;
    this.snapshotFactory = snapshotFactory;
  }

  public SnapshotMessage<A> apply(@NonNull final String id) {

    log.debug("cache.get(id)", id);

    val snapshotFromCache = cache.get(id);

    if (snapshotFromCache == null) {
      log.debug("cache.getInstance(id) does not contain anything for id {}. Will have to search on eventRepository", id);
      val dataFromDb = eventRepository.getAll(id);
      val snapshotFromDb = dataFromDb.map(snapshotFactory::createSnapshot).orElseGet(snapshotFactory::getEmptySnapshot);
      return new SnapshotMessage<>(snapshotFromDb, LoadedFromEnum.FROM_DB);
    }

    val cachedVersion = snapshotFromCache.getVersion();

    log.debug("id {} cached lastSnapshotData has version {}. will check if there any version beyond it",
            id, cachedVersion);

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
