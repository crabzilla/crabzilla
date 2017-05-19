package crabzilla.stack1;


import crabzilla.Version;
import crabzilla.model.AggregateRoot;
import crabzilla.model.AggregateRootId;
import crabzilla.model.Event;
import crabzilla.stack.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

@Slf4j
public class Stack1SnapshotReader<ID extends AggregateRootId, A extends AggregateRoot>
				implements SnapshotReader<ID, A> {

	private static Version lastVersion(final SnapshotData unitOfWorks) {
		return unitOfWorks.getEvents().isEmpty() ? Version.create(0L) : unitOfWorks.getVersion();
	}

	private static final Logger logger = LoggerFactory.getLogger(Stack1SnapshotReader.class);

	final Cache<ID, SnapshotData> cache;
	final EventRepository dao;
  final Supplier<A> supplier;
  final Function<A, A> dependencyInjectionFn;
  final BiFunction<Event, A, A> stateTransitionFn;

	public Stack1SnapshotReader(@NonNull Cache<ID, SnapshotData> cache,
															@NonNull EventRepository dao,
															@NonNull Supplier<A> supplier,
															@NonNull Function<A, A> dependencyInjectionFn,
															@NonNull BiFunction<Event, A, A> stateTransitionFn) {
		this.cache = cache;
		this.dao = dao;
		this.supplier = supplier;
		this.dependencyInjectionFn = dependencyInjectionFn;
		this.stateTransitionFn = stateTransitionFn;
	}

	@Override
	public Snapshot<A> getSnapshot(ID id) {

		requireNonNull(id);

		logger.debug("id {} cache.getInstance(id)", id);

    final StateTransitionsTracker<A> tracker = new StateTransitionsTracker<>(supplier.get(),
            stateTransitionFn, dependencyInjectionFn);

		final AtomicBoolean wasDaoCalled = new AtomicBoolean(false);

		final SnapshotData cachedUowList = cache.get(id, s -> {
			logger.debug("id {} cache.getInstance(id) does not contain anything for this id. Will have to search on dao",
							id);
			wasDaoCalled.set(true);
			return dao.getAll(id.getStringValue());
		});

		logger.debug("id {} wasDaoCalled ? {}", id, wasDaoCalled.get());

		if (wasDaoCalled.get()) {
			return new Snapshot<A>(tracker.applyEvents(cachedUowList.getEvents()).currentState(), cachedUowList.getVersion());
		}

		final Version lastVersion = lastVersion(cachedUowList);

		logger.debug("id {} cached lastSnapshotData has version {}. will check if there any version beyond it",
						id, lastVersion);

		final SnapshotData nonCachedUowList =
						dao.getAllAfterVersion(id.getStringValue(), lastVersion);

		logger.debug("id {} found {} pending transactions. Last version is now {}",
						id, nonCachedUowList.getEvents().size(), lastVersion);

		final List<Event> cachedPlusNonCachedList =
						javaslang.collection.List.ofAll(cachedUowList.getEvents())
						.appendAll(nonCachedUowList.getEvents()).toJavaList();

		final Version finalVersion = nonCachedUowList.getVersion();

		cache.put(id, new SnapshotData(finalVersion, cachedPlusNonCachedList));

		return new Snapshot<A>(tracker.applyEvents(cachedPlusNonCachedList).currentState(), finalVersion);

	}

}
