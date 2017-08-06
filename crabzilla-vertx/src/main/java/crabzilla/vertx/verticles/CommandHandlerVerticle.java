package crabzilla.vertx.verticles;

import crabzilla.model.*;
import crabzilla.vertx.CommandExecution;
import crabzilla.vertx.repositories.VertxUnitOfWorkRepository;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.jodah.expiringmap.ExpiringMap;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static crabzilla.vertx.CommandExecution.*;
import static crabzilla.vertx.util.StringHelper.commandHandlerId;
import static java.util.Collections.singletonList;

@Slf4j
public class CommandHandlerVerticle<A extends AggregateRoot> extends AbstractVerticle {

  final Class<A> aggregateRootClass;
  final BiFunction<Command, Snapshot<A>, Either<Throwable, Optional<UnitOfWork>>> cmdHandler;
  final Function<Command, List<String>> validatorFn;
  final ExpiringMap<String, Snapshot<A>> cache;
  final Snapshotter<A> snapshotter;

  final VertxUnitOfWorkRepository eventRepository;
  final Vertx vertx;
  final CircuitBreaker circuitBreaker;

  public CommandHandlerVerticle(@NonNull final Class<A> aggregateRootClass,
                                @NonNull final BiFunction<Command, Snapshot<A>, Either<Throwable, Optional<UnitOfWork>>> cmdHandler,
                                @NonNull final Function<Command, List<String>> validatorFn,
                                @NonNull final Snapshotter<A> snapshotter,
                                @NonNull final VertxUnitOfWorkRepository eventRepository,
                                @NonNull final ExpiringMap<String, Snapshot<A>> cache,
                                @NonNull final Vertx vertx,
                                @NonNull final CircuitBreaker circuitBreaker) {
    this.aggregateRootClass = aggregateRootClass;
    this.cmdHandler = cmdHandler;
    this.validatorFn = validatorFn;
    this.snapshotter = snapshotter;
    this.eventRepository = eventRepository;
    this.cache = cache;
    this.vertx = vertx;
    this.circuitBreaker = circuitBreaker;
  }

  @Override
  public void start() throws Exception {

    vertx.eventBus().consumer(commandHandlerId(aggregateRootClass), msgHandler());

  }

  Handler<Message<Command>> msgHandler() {

    return (Message<Command> msg) -> {

      val command = msg.body();

      if (command==null) {
        msg.reply(VALIDATION_ERROR(singletonList("Command cannot be null. Check if JSON payload is valid.")));
        return;
      }

      log.info("received a command {}", command);

      val constraints = validatorFn.apply(command);

      if (!constraints.isEmpty()) {
        msg.reply(VALIDATION_ERROR(command.getCommandId(), constraints));
        return;
      }

      circuitBreaker.fallback(throwable -> {

        log.error("Fallback for command " + command.getCommandId(), throwable);

        return FALLBACK(command.getCommandId());

      })

      .execute(cmdHandler(command))

      .setHandler(resultHandler(msg));

    };
  }

  Handler<Future<CommandExecution>> cmdHandler(final Command command) {

    return future1 -> {

      val targetId = command.getTargetId().getStringValue();

      log.debug("cache.get(id)", targetId);

      val snapshotFromCache = cache.get(targetId);

      val cachedSnapshot = snapshotFromCache == null ? snapshotter.getEmptySnapshot() : snapshotFromCache;

      log.debug("id {} cached lastSnapshotData has version {}. Will check if there any version beyond it",
              targetId, cachedSnapshot.getVersion());

      Future<SnapshotData> selectAfterVersionFuture = Future.future();

      eventRepository.selectAfterVersion(targetId, cachedSnapshot.getVersion(), selectAfterVersionFuture);

      selectAfterVersionFuture.setHandler(snapshotDataAsyncResult -> {
        if (snapshotDataAsyncResult.failed()) {
          future1.fail(snapshotDataAsyncResult.cause());
          return;
        }

        SnapshotData nonCached = snapshotDataAsyncResult.result();
        val totalOfNonCachedEvents = nonCached.getEvents().size();

        log.debug("id {} found {} pending events. Last version is now {}", targetId, totalOfNonCachedEvents,
                nonCached.getVersion());

        val resultingSnapshot = totalOfNonCachedEvents > 0 ?
                snapshotter.applyNewEventsToSnapshot(cachedSnapshot, nonCached.getVersion(), nonCached.getEvents())
                : cachedSnapshot;

        if (totalOfNonCachedEvents > 0) {
          cache.put(targetId, resultingSnapshot);
        }

        // cmd handler _may_ be blocking. Otherwise, aggregate root would need to use reactive API to call
        // external services
        vertx.executeBlocking(blockingCmdHandler(command, resultingSnapshot), false, event -> {

          if (event.succeeded()) {
            future1.complete(event.result());
          } else {
            future1.fail(event.cause());
          }

        });

      });

    };

  }

  Handler<Future<CommandExecution>> blockingCmdHandler(Command command, Snapshot<A> resultingSnapshot) {

    return future2 ->

      cmdHandler.apply(command, resultingSnapshot).match(cmdHandlerError -> {

        log.error("Command handling error for command {} message {}", command.getCommandId(), cmdHandlerError.getMessage());
        future2.complete(HANDLING_ERROR(command.getCommandId()));
        return null;

      }, (Function<Optional<UnitOfWork>, Void>) unitOfWork -> {

        if (unitOfWork.isPresent()) {

          Future<Either<Throwable, Long>> appendFuture = Future.future();

          eventRepository.append(unitOfWork.get(), appendFuture);

          appendFuture.setHandler(appendAsyncResult -> {
            if (appendAsyncResult.failed()) {
              future2.fail(appendAsyncResult.cause());
              return;
            }

            Either<Throwable, Long> appendResult = appendAsyncResult.result();
            appendResult.match(cmdAppendError -> {

              log.error("Exception for command {} message {}", command.getCommandId(), cmdAppendError.getMessage());
              future2.complete(CONCURRENCY_ERROR(command.getCommandId(), cmdAppendError.getMessage()));
              return null;

            }, uowSequence -> {

              future2.complete(SUCCESS(unitOfWork.get(), uowSequence));
              return null;

            });
          });

        } else {

            future2.complete(UNKNOWN_COMMAND(command.getCommandId()));

        }

        return null;

      });
  }

  Handler<AsyncResult<CommandExecution>> resultHandler(final Message<Command> msg) {

    return (AsyncResult<CommandExecution> resultHandler) -> {

      if (resultHandler.succeeded()) {

        val resp = resultHandler.result();
        log.info("success: {}", resp);
        msg.reply(resp);

      } else {

        log.error("error cause: {}", resultHandler.cause());
        log.error("error message: {}", resultHandler.cause().getMessage());
        resultHandler.cause().printStackTrace();
        // TODO customize conform commandResult
        msg.fail(400, resultHandler.cause().getMessage());
      }

    };
  }

}

