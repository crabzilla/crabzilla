package io.github.crabzilla.vertx.entity;

import io.github.crabzilla.core.entity.*;
import io.github.crabzilla.core.exceptions.DbConcurrencyException;
import io.github.crabzilla.core.exceptions.UnknownCommandException;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.jodah.expiringmap.ExpiringMap;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.github.crabzilla.vertx.entity.EntityCommandExecution.*;
import static io.github.crabzilla.vertx.helpers.StringHelper.commandHandlerId;
import static java.util.Collections.singletonList;

@Slf4j
public class EntityCommandHandlerVerticle<A extends Entity> extends AbstractVerticle {

  private final Class<A> aggregateRootClass;
  private final A seedValue;
  private final BiFunction<EntityCommand, Snapshot<A>, EntityCommandResult> cmdHandler;
  private final Function<EntityCommand, List<String>> validatorFn;
  private final ExpiringMap<String, Snapshot<A>> cache;
  private final SnapshotPromoter<A> snapshotPromoter;

  private final EntityUnitOfWorkRepository eventRepository;
  private final CircuitBreaker circuitBreaker;

  public EntityCommandHandlerVerticle(@NonNull final Class<A> aggregateRootClass,
                                      @NonNull final A seedValue,
                                      @NonNull final BiFunction<EntityCommand, Snapshot<A>, EntityCommandResult> cmdHandler,
                                      @NonNull final Function<EntityCommand, List<String>> validatorFn,
                                      @NonNull final SnapshotPromoter<A> snapshotPromoter,
                                      @NonNull final EntityUnitOfWorkRepository eventRepository,
                                      @NonNull final ExpiringMap<String, Snapshot<A>> cache,
                                      @NonNull final CircuitBreaker circuitBreaker) {
    this.aggregateRootClass = aggregateRootClass;
    this.seedValue = seedValue;
    this.cmdHandler = cmdHandler;
    this.validatorFn = validatorFn;
    this.snapshotPromoter = snapshotPromoter;
    this.eventRepository = eventRepository;
    this.cache = cache;
    this.circuitBreaker = circuitBreaker;
  }

  @Override
  public void start() throws Exception {
    vertx.eventBus().consumer(commandHandlerId(aggregateRootClass), msgHandler());
  }

  Handler<Message<EntityCommand>> msgHandler() {
    return (Message<EntityCommand> msg) -> {
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

  Handler<Future<EntityCommandExecution>> cmdHandler(final EntityCommand command) {

    return future1 -> {

      val targetId = command.getTargetId().stringValue();

      // get from cache _may_ be blocking if you plug an EntryLoader (from ExpiringMap)
      vertx.<Snapshot<A>>executeBlocking(fromCacheFuture -> {

        log.debug("loading {} from cache", targetId);

        fromCacheFuture.complete(cache.get(targetId));

        }, false, fromCacheResult -> {

        if (fromCacheResult.failed()) {
          future1.fail(fromCacheResult.cause());
          return;
        }

        val snapshotFromCache = fromCacheResult.result();
        val emptySnapshot = new Snapshot<A>(seedValue, new Version(0));
        val cachedSnapshot = snapshotFromCache == null ? emptySnapshot : snapshotFromCache;

        log.debug("id {} cached lastSnapshotData has version {}. Will check if there any version beyond it",
                targetId, cachedSnapshot);

        Future<SnapshotData> selectAfterVersionFuture = Future.future();

        // command handler function _may_ be blocking if your aggregate are using blocking internal services
        eventRepository.selectAfterVersion(targetId, cachedSnapshot.getVersion(), selectAfterVersionFuture);

        selectAfterVersionFuture.setHandler(fromEventRepoResult -> {

          if (fromEventRepoResult.failed()) {
            future1.fail(fromEventRepoResult.cause());
            return;
          }

          val nonCached = fromEventRepoResult.result();
          val totalOfNonCachedEvents = nonCached.getEvents().size();
          log.debug("id {} found {} pending events. Last version is now {}", targetId, totalOfNonCachedEvents,
                  nonCached.getVersion());

          // cmd handler _may_ be blocking. Otherwise, aggregate root would need to use reactive API to call
          // external services
          vertx.<EntityCommandExecution>executeBlocking(future2 -> {

            val resultingSnapshot = totalOfNonCachedEvents > 0 ?
                    snapshotPromoter.promote(cachedSnapshot, nonCached.getVersion(), nonCached.getEvents())
                    : cachedSnapshot;

            if (totalOfNonCachedEvents > 0) {
              cache.put(targetId, resultingSnapshot);
            }

            val result = cmdHandler.apply(command, resultingSnapshot);

            result.inCaseOfSuccess(uow -> {
              Future<Long> appendFuture = Future.future();
              eventRepository.append(uow, appendFuture);
              appendFuture.setHandler(appendAsyncResult -> {
                if (appendAsyncResult.failed()) {
                  val error =  appendAsyncResult.cause();
                  log.error("appendUnitOfWork for command " + command.getCommandId(), error.getMessage());
                  if (error instanceof DbConcurrencyException) {
                    future2.complete(CONCURRENCY_ERROR(command.getCommandId(), error.getMessage()));
                  } else {
                    future2.fail(appendAsyncResult.cause());
                  }
                  return ;
                }
                val finalSnapshot = snapshotPromoter.promote(resultingSnapshot, uow.getVersion(), uow.getEvents());
                cache.put(targetId, finalSnapshot);
                val uowSequence = appendAsyncResult.result();
                log.debug("uowSequence: {}", uowSequence);
                future2.complete(SUCCESS(uow, uowSequence));
              });
            });

            result.inCaseOfError(error -> {
              log.error("commandExecution", error.getMessage());
              if (error instanceof UnknownCommandException) {
                future2.complete(UNKNOWN_COMMAND(command.getCommandId()));
              } else {
                future2.complete(HANDLING_ERROR(command.getCommandId(), error.getMessage()));
              }
            });

          }, res -> {
            if (res.succeeded()) {
              future1.complete(res.result());
            } else {
              log.error("selectAfterVersion ", res.cause());
              future1.fail(res.cause());
            }
          });
        });

      });

    };
  }

  Handler<AsyncResult<EntityCommandExecution>> resultHandler(final Message<EntityCommand> msg) {

    return (AsyncResult<EntityCommandExecution> resultHandler) -> {
      if (!resultHandler.succeeded()) {
        log.error("resultHandler", resultHandler.cause());
        // TODO customize
        msg.fail(400, resultHandler.cause().getMessage());
        return;
      }
      val resp = resultHandler.result();
      val options = new DeliveryOptions().setCodecName("EntityCommandExecution");
      msg.reply(resp, options);
//      msg.reply(resp, options, (Handler<AsyncResult<Message<EntityCommandExecution>>>)
//              event -> {
//        if (!event.succeeded()) {
//          log.error("msg.reply ", event.cause());
//        }
//      });
    };
  }

}

