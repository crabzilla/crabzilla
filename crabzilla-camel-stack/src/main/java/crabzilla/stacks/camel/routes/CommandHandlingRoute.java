package crabzilla.stacks.camel.routes;

import com.google.gson.Gson;
import crabzilla.model.util.Either;
import crabzilla.model.util.Eithers;
import crabzilla.stack.EventRepository;
import crabzilla.stack.SnapshotMessage;
import crabzilla.stack.SnapshotReaderFn;
import crabzilla.stack.util.HeadersConstants;
import crabzilla.stack.util.StringHelper;
import lombok.NonNull;
import lombok.val;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.IdempotentRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class CommandHandlingRoute<A extends AggregateRoot> extends RouteBuilder {

  // TODO desacoplar do http

  static final String RESULT = "result";
  static final String IS_ERROR = "IS_ERROR";

  final Snapshot<A> EMPTY_SNAPSHOT ;
  final Class<A> aggregateRootClass;
  final SnapshotReaderFn<A> snapshotReaderFn;
  final CommandHandlerFn<A> handler;
  final Supplier<A> supplier;
  final EventRepository writeModelRepo;
  final Gson gson ;
  final IdempotentRepository<String> idempotentRepo;

  public CommandHandlingRoute(Snapshot<A> empty_snapshot, @NonNull Class<A> aggregateRootClass,
                              @NonNull SnapshotReaderFn<A> snapshotReaderFn,
                              @NonNull CommandHandlerFn<A> handler,
                              @NonNull Supplier<A> supplier,
                              @NonNull EventRepository writeModelRepo,
                              @NonNull Gson gson,
                              @NonNull IdempotentRepository<String> idempotentRepo) {
    EMPTY_SNAPSHOT = empty_snapshot;
    this.aggregateRootClass = aggregateRootClass;
    this.snapshotReaderFn = snapshotReaderFn;
    this.handler = handler;
    this.supplier = supplier;
    this.writeModelRepo = writeModelRepo;
    this.gson = gson;
    this.idempotentRepo = idempotentRepo;
  }

  @Override
  public void configure() throws Exception {

    fromF("direct:handle-cmd-%s", StringHelper.aggregateRootId(aggregateRootClass))
      .routeId("handle-cmd-" + StringHelper.aggregateRootId(aggregateRootClass))
      .log("received command as json: ${body}")
      .doTry()
        .process(e -> {
          final String asJson = e.getIn().getBody(String.class);
          final Command instance = gson.fromJson(asJson, Command.class);
          e.getOut().setBody(instance, Command.class);
          e.getOut().setHeaders(e.getIn().getHeaders());
        })
        .log("command as java: ${body}")
      .doCatch(Exception.class)
        .log(LoggingLevel.ERROR, "error ")
        .setBody(constant(Arrays.asList("gson serialization error")))
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
        .process(e -> {
          final List<String> instance = e.getIn().getBody(List.class);
          final String asJson = gson.toJson(instance, List.class);
          e.getOut().setBody(asJson, String.class);
        })
        .log(LoggingLevel.ERROR, "error as json: ${body}")
        .stop()
      .end()
      .hystrix()
        .hystrixConfiguration()
          .groupKey("handle-cmd-" + StringHelper.aggregateRootId(aggregateRootClass))
          .executionTimeoutInMilliseconds(5000).circuitBreakerSleepWindowInMilliseconds(10000)
        .end()
        .process(new CommandProcessor())
        .toF("direct:process-results-%s", StringHelper.aggregateRootId(aggregateRootClass))
      .onFallback()
        .setBody(constant(Arrays.asList("fallback - circuit breaker seems to be open")))
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(503))
        .process(e -> {
          final List instance = e.getIn().getBody(List.class);
          final String asJson = gson.toJson(instance, List.class);
          e.getOut().setBody(asJson, String.class);
          e.getOut().setHeaders(e.getIn().getHeaders());
        })
      .end()
      .log("result as json: ${body}")
    ;

    fromF("direct:process-results-%s", StringHelper.aggregateRootId(aggregateRootClass))
      .routeId("process-results-" + StringHelper.aggregateRootId(aggregateRootClass))
      .choice()
        .when(header(IS_ERROR).isEqualTo(false))
          .toF("direct:save-events-%s", StringHelper.aggregateRootId(aggregateRootClass))
          .process(e -> {
            final Optional<UnitOfWork> result = e.getIn().getHeader(RESULT, Optional.class);
            if (result.isPresent()) {
              val asJson = gson.toJson(result.get());
              e.getOut().setBody(asJson, String.class);
              e.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 201);
            } else {
              e.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
            }
          })
        .otherwise()
          .process(e -> {
            final Optional<UnitOfWork> result = e.getIn().getHeader(RESULT, Optional.class);
            //val asJson = gson.toJson(Arrays.asList(result.get().getMessage()), List.class);
            // TODO e.getOut().setBody(asJson);
            e.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
          })
      .end()
    ;

    fromF("direct:save-events-%s", StringHelper.aggregateRootId(aggregateRootClass))
      .routeId("save-events-" + StringHelper.aggregateRootId(aggregateRootClass))
      .log(LoggingLevel.DEBUG, "${header.command_id}")
      .idempotentConsumer(header(HeadersConstants.COMMAND_ID)).messageIdRepository(idempotentRepo)
      .process(new SaveEventsProcessor())
      ;
  }

  final class CommandProcessor implements Processor {

    @Override
    public void process(Exchange e) throws Exception {

      final Command command = e.getIn().getBody(Command.class);
      final SnapshotMessage<A> snapshotMsg = snapshotReaderFn.getSnapshotMessage(command.getTargetId().getStringValue());
      Either<Exception,UnitOfWork> result ;
      try {
        result = handler.handle(command, snapshotMsg.getSnapshot());
        // TODO check if snapshotDataMsg is new and add it to cache
      } catch (Exception ex) {
        result = Eithers.left(ex);
      }
      e.getOut().setBody(command, Command.class);
      e.getOut().setHeader(IS_ERROR, result.match(err-> true, uow -> false));
      e.getOut().setHeader(HeadersConstants.COMMAND_ID, command.getCommandId());
      e.getOut().setHeader(RESULT, result);
    }
  }

  final class SaveEventsProcessor implements Processor {

    @Override
    public void process(Exchange e) throws Exception {

      final Optional<UnitOfWork> result = e.getIn().getHeader(RESULT, Optional.class);

      result.ifPresent(writeModelRepo::append);

    }

  }

}
