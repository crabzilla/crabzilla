package crabzilla.stacks.sql;


import com.github.benmanes.caffeine.cache.Cache;
import crabzilla.Version;
import crabzilla.model.AggregateRoot;
import crabzilla.model.Event;
import crabzilla.stack.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static crabzilla.stack.SnapshotMessage.LoadedFromEnum;

@Slf4j
public class CaffeinedSnapshotReaderFn<A extends AggregateRoot> implements SnapshotReaderFn<A> {

	private static final Logger logger = LoggerFactory.getLogger(CaffeinedSnapshotReaderFn.class);

	final Snapshot<A> EMPTY_SNAPSHOT ;

  final Cache<String, Snapshot<A>> cache;
	final EventRepository dao;
  final Supplier<A> supplier;
  final Function<A, A> dependencyInjectionFn;
  final BiFunction<Event, A, A> stateTransitionFn;

	public CaffeinedSnapshotReaderFn(@NonNull Cache<String, Snapshot<A>> cache,
                                   @NonNull EventRepository dao,
                                   @NonNull Supplier<A> supplier,
                                   @NonNull Function<A, A> dependencyInjectionFn,
                                   @NonNull BiFunction<Event, A, A> stateTransitionFn) {
		this.EMPTY_SNAPSHOT = new Snapshot<>(supplier.get(), new Version(0L));
	  this.cache = cache;
		this.dao = dao;
		this.supplier = supplier;
		this.dependencyInjectionFn = dependencyInjectionFn;
		this.stateTransitionFn = stateTransitionFn;
	}

	@Override
	public SnapshotMessage<A> getSnapshotMessage(@NonNull final String id) {

		logger.debug("cache.get(id)", id);

    val wasDaoCalled = new AtomicBoolean(false);

    val cachedSnapshot = cache.get(id, s -> {
			logger.debug("cache.getInstance(id) does not contain anything for this id. Will have to search on dao", id);
			val dataFromDb = dao.getAll(id);
      return dataFromDb.map(snapshotData ->
              createSnapshot(EMPTY_SNAPSHOT, snapshotData.getVersion(), snapshotData.getEvents()))
              .orElse(EMPTY_SNAPSHOT);
		});

    if (wasDaoCalled.get()) { // so hopefully all data from db was loaded
      return new SnapshotMessage<>(cachedSnapshot, LoadedFromEnum.FROM_DB);
    }

    if (cachedSnapshot.isEmpty()) {
      return new SnapshotMessage<>(cachedSnapshot, LoadedFromEnum.FROM_CACHE);
    }

    val cachedVersion = cachedSnapshot.getVersion();

		logger.debug("id {} cached lastSnapshotData has version {}. will check if there any version beyond it",
						id, cachedVersion);

		val nonCachedSnapshotData = dao.getAllAfterVersion(id, cachedVersion);

		if (nonCachedSnapshotData.isPresent()) {

		  val resultingSnapshotData = nonCachedSnapshotData.get();

      logger.debug("id {} found {} pending transactions. Last version is now {}",
              id, resultingSnapshotData.getEvents().size(), resultingSnapshotData.getVersion());

      val resultingSnapshot = createSnapshot(cachedSnapshot, resultingSnapshotData.getVersion(),
                                              resultingSnapshotData.getEvents());

      return new SnapshotMessage<>(resultingSnapshot, LoadedFromEnum.FROM_BOTH);

    }

    return new SnapshotMessage<>(cachedSnapshot, LoadedFromEnum.FROM_CACHE);

	}

	private Snapshot<A> createSnapshot(Snapshot<A> originalSnapshot, Version newVersion, List<Event> newEvents) {

    final StateTransitionsTracker<A> tracker = new StateTransitionsTracker<>(originalSnapshot.getInstance(),
            stateTransitionFn, dependencyInjectionFn);

    return new Snapshot<>(tracker.applyEvents(newEvents).currentState(), newVersion);
  }

}
