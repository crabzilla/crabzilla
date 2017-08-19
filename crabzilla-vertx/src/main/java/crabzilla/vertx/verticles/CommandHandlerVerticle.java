package crabzilla.vertx.verticles;

import crabzilla.model.*;
import crabzilla.stack.CommandExecution;
import crabzilla.stack.DbConcurrencyException;
import crabzilla.stack.UnknownCommandException;
import crabzilla.vertx.repositories.EntityUnitOfWorkRepository;
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

import static crabzilla.stack.CommandExecution.*;
import static crabzilla.stack.StringHelper.commandHandlerId;
import static java.util.Collections.singletonList;

@Slf4j
public class CommandHandlerVerticle<A extends Aggregate> extends AbstractVerticle {

  final Class<A> aggregateRootClass;
  final A seedValue;
  final BiFunction<EntityCommand, Snapshot<A>, CommandHandlerResult> cmdHandler;
  final Function<EntityCommand, List<String>> validatorFn;
  final ExpiringMap<String, Snapshot<A>> cache;
  final SnapshotPromoter<A> snapshotPromoter;

  final EntityUnitOfWorkRepository eventRepository;
  final CircuitBreaker circuitBreaker;

  public CommandHandlerVerticle(@NonNull final Class<A> aggregateRootClass,
                                @NonNull final A seedValue,
                                @NonNull final BiFunction<EntityCommand, Snapshot<A>, CommandHandlerResult> cmdHandler,
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
      .<CommandExecution>execute(cmdHandler(command))
      .setHandler(resultHandler(msg));
    };
  }

  Handler<Future<CommandExecution>> cmdHandler(final EntityCommand command) {

    return future1 -> {
      val targetId = command.getTargetId().stringValue();

      log.debug("cache.get(id)", targetId);
      val snapshotFromCache = cache.get(targetId);
      val emptySnapshot = new Snapshot<A>(seedValue, new Version(0));
      val cachedSnapshot = snapshotFromCache == null ? emptySnapshot : snapshotFromCache;

      log.info("id {} cached lastSnapshotData has version {}. Will check if there any version beyond it",
              targetId, cachedSnapshot);
      Future<SnapshotData> selectAfterVersionFuture = Future.future();
      eventRepository.selectAfterVersion(targetId, cachedSnapshot.getVersion(), selectAfterVersionFuture);

      selectAfterVersionFuture.setHandler(snapshotDataAsyncResult -> {
        if (snapshotDataAsyncResult.failed()) {
          future1.fail(snapshotDataAsyncResult.cause());
          return;
        }
        val nonCached = snapshotDataAsyncResult.result();
        val totalOfNonCachedEvents = nonCached.getEvents().size();
        log.debug("id {} found {} pending events. Last version is now {}", targetId, totalOfNonCachedEvents,
                nonCached.getVersion());
        val resultingSnapshot = totalOfNonCachedEvents > 0 ?
                snapshotPromoter.promote(cachedSnapshot, nonCached.getVersion(), nonCached.getEvents())
                : cachedSnapshot;
        if (totalOfNonCachedEvents > 0) {
          cache.put(targetId, resultingSnapshot);
        }
        // cmd handler _may_ be blocking. Otherwise, aggregate root would need to use reactive API to call
        // external services
        vertx.<CommandExecution>executeBlocking(future2 -> {

          val result = cmdHandler.apply(command, resultingSnapshot);

          result.inCaseOfSuccess(uow -> {
            log.info("CommandExecution: {}", uow);
            Future<Long> appendFuture = Future.future();
            eventRepository.append(uow, appendFuture);
            appendFuture.setHandler(appendAsyncResult -> {
              if (appendAsyncResult.failed()) {
                val error =  appendAsyncResult.cause();
                log.error("When appending uow of command {} -> {}", command.getCommandId(), error.getMessage());
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
              log.info("uowSequence: {}", uowSequence);
              future2.complete(SUCCESS(uow, uowSequence));
            });
          });

          result.inCaseOfError(error -> {
            log.error("CommandExecution: {}", error.getMessage());
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
            res.cause().printStackTrace();
            future1.fail(res.cause());
          }
        });
      });
    };
  }


  Handler<AsyncResult<CommandExecution>> resultHandler(final Message<EntityCommand> msg) {

    return (AsyncResult<CommandExecution> resultHandler) -> {
      if (resultHandler.succeeded()) {
        val resp = resultHandler.result();
        log.info("** success: {}", resp);
        val options = new DeliveryOptions().setCodecName("CommandExecution");
        msg.reply(resp, options, new Handler<AsyncResult<Message<CommandExecution>>>() {
          @Override
          public void handle(AsyncResult<Message<CommandExecution>> event) {
            log.info("succeeded ?: "+ event.succeeded());
          }
        });
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

