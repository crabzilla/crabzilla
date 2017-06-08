package crabzilla.stack.vertx;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectWriter;
import crabzilla.model.Command;
import crabzilla.model.Event;
import crabzilla.model.UnitOfWork;
import crabzilla.model.Version;
import crabzilla.stack.EventRepository;
import crabzilla.stack.ProjectionData;
import crabzilla.stack.SnapshotData;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.TransactionIsolation;
import lombok.NonNull;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class VertxEventRepository implements EventRepository {

  static final Logger logger = LoggerFactory.getLogger(VertxEventRepository.class);

  static final String UOW_ID = "uow_id";
  static final String UOW_EVENTS = "uow_events";
  static final String CMD_DATA = "cmd_data";
  static final String VERSION = "version";

  private final String aggregateRootName;
  private final JDBCClient client;

  private final TypeReference<List<Event>> eventsListTpe =  new TypeReference<List<Event>>() {};

  public VertxEventRepository(@NonNull String aggregateRootName,@NonNull JDBCClient client) {
    this.aggregateRootName = aggregateRootName;
    this.client = client;
  }

  @Override
  public Optional<UnitOfWork> get(@NonNull UUID uowId) {

    val result = new AtomicReference<Optional<UnitOfWork>>(Optional.empty());
    val SELECT_UOW_BY_ID = "select * from units_of_work where uow_id =? ";
    val params = new JsonArray().add(uowId.toString());

    client.getConnection(res -> {
      if (res.succeeded()) {
        val connection = res.result();
        connection.queryWithParams(SELECT_UOW_BY_ID, params, res2 -> {
          if (res2.succeeded()) {
            val rs = res2.result();
            val rows = rs.getRows();
            for (JsonObject row : rows) {
              val command = Json.decodeValue(row.getString(CMD_DATA), Command.class);
              final List<Event> events = readEvents(row.getString(UOW_EVENTS));
              val uow = new UnitOfWork(UUID.fromString(row.getString(UOW_ID)), command,
                      new Version(row.getLong(VERSION)), events);
              result.set(Optional.of(uow));
            }
          }
        });
      } else {
        logger.error("Decide what to do"); // TODO
        // Failed to get connection - deal with it
      }
    });

    return result.get();

  }

  @Override
  public List<ProjectionData> getAllSince(long sinceUowSequence, int maxResultSize) {

    logger.info("will load a maximum of {} units of work since sequence {}", maxResultSize, sinceUowSequence);

    val SELECT_SINCE_UOW_SEQ = "select uow_id, uow_seq_number, ar_id, uow_events " +
            "from units_of_work where uow_seq_number > ? order by uow_seq_number limit %d";
    val result = new ArrayList<ProjectionData>();
    val params = new JsonArray().add(sinceUowSequence);

    client.getConnection(res -> {
      if (res.succeeded()) {
        val connection = res.result();
        val sql = String.format(SELECT_SINCE_UOW_SEQ, maxResultSize);
        connection.queryStreamWithParams(sql, params, stream -> {
          if (stream.succeeded()) {
            stream.result().handler(row -> {
              // uow_id, uow_seq_number, ar_id, uow_events
              final List<Event> events = readEvents(row.getString(3));
              val projectionData = new ProjectionData(row.getString(0), row.getLong(1),
                      row.getString(2), events);
              result.add(projectionData);
            });
          }
        });
      } else {
        logger.error("Decide what to do"); // TODO
        // Failed to get connection - deal with it
      }
    });

    logger.info("Found {} units of work since sequence {}", result.size(), sinceUowSequence);
    return result;

  }

  @Override
  public Optional<SnapshotData> getAll(@NonNull final String id) {
    return getAllAfterVersion(id, new Version(0L));
  }

  @Override
  public Optional<SnapshotData> getAllAfterVersion(@NonNull final String id, @NonNull final Version version) {

    logger.info("will load {}", id);

    val SELECT_AFTER_VERSION = "select version, uow_events from units_of_work " +
            " where ar_id = ? and ar_name = ? " +
            "   and version > ? " +
            " order by version";

    val list = new ArrayList<SnapshotData>();
    val params = new JsonArray().add(id).add(aggregateRootName).add(version.getValueAsLong());

    client.getConnection(res -> {
      if (res.succeeded()) {
        val connection = res.result();
        connection.queryStreamWithParams(SELECT_AFTER_VERSION, params, stream -> {
          if (stream.succeeded()) {
            stream.result().handler(row -> {
              final List<Event> events = readEvents(row.getString(0));
              val snapshotData = new SnapshotData(new Version(row.getLong(1)), events);
              list.add(snapshotData);
            });
          }
        });
      } else {
        logger.error("Decide what to do"); // TODO
        // Failed to get connection - deal with it
      }
    });

    logger.info("found {} units of work for id {} and version > {}",
            list.size(), id, version.getValueAsLong());

    if (list.isEmpty()) {
      return Optional.empty();
    }

    val finalVersion = list.get(list.size() - 1).getVersion();

    final List<Event> flatMappedToEvents = list.stream()
            .flatMap(sd -> sd.getEvents().stream()).collect(Collectors.toList());

    val result = new SnapshotData(finalVersion, flatMappedToEvents);

    return Optional.of(result);

  }

  @Override
  public Long append(@NonNull final UnitOfWork unitOfWork) {

    final AtomicReference<Long> uowSequence = new AtomicReference<>(0L);

    val SELECT_CURRENT_VERSION =
            "select max(version) from aggregate_roots where ar_id = ? and ar_name = ? group by ar_id";

    val INSERT_UOW = "insert into units_of_work " +
            "(uow_id, uow_events, cmd_id, cmd_data, ar_id, ar_name, version, inserted_on) " +
            "values (:uow_id, :uow_events, :cmd_id, :cmd_data, :ar_id, :ar_name, :version, :inserted_on)";

    client.getConnection(res1 -> {

      if (res1.succeeded()) {

        val connection = res1.result();

        connection.setAutoCommit(false, res2 -> {

          if (res2.succeeded()) {

            connection.setTransactionIsolation(TransactionIsolation.SERIALIZABLE, res3 -> {

              if (res3.succeeded()) {

                final AtomicReference<Optional<Long>> currentVersion =
                        new AtomicReference<>(Optional.empty());

                val params1 = new JsonArray()
                        .add(unitOfWork.getUnitOfWorkId().toString())
                        .add(aggregateRootName);

                connection.queryWithParams(SELECT_CURRENT_VERSION, params1, res4 -> {

                  if (res4.succeeded()) {

                    val rs = res4.result();
                    val rows = rs.getRows();

                    for (JsonObject row : rows) {
                      currentVersion.set(Optional.of(row.getLong(VERSION)));
                    }

                    newVersionIsCurrentVersionPlus1(unitOfWork, currentVersion.get().orElse(0L));

                    val cmdAsJson = writeValueAsString(Json.mapper.writerFor(Command.class), unitOfWork.getCommand());
                    val eventsAsJson = writeValueAsString(Json.mapper.writerFor(eventsListTpe), unitOfWork.getEvents());

                    val params2 = new JsonArray()
                            .add(unitOfWork.getUnitOfWorkId().toString())
                            .add(eventsAsJson)
                            .add(unitOfWork.getCommand().getCommandId().toString())
                            .add(cmdAsJson)
                            .add(unitOfWork.targetId().getStringValue())
                            .add(aggregateRootName)
                            .add(unitOfWork.getVersion().getValueAsLong());

                    connection.updateWithParams(INSERT_UOW, params2, res5 -> {

                      if (res5.succeeded()) {

                        val result = res5.result();
                        System.out.println("Updated no. of rows: " + result.getUpdated());
                        System.out.println("Generated keys: " + result.getKeys());

                        connection.commit(res6 -> {

                          if (res6.succeeded()) {
                            uowSequence.set(result.getKeys().getLong(0));
                          } else {
                            logger.error("Error 6", res6.cause());
                          }

                        });

                      } else {

                        // Failed!

                        throw new DbConcurrencyException(
                                String.format("id = [%s], current_version = %d, new_version = %d",
                                        unitOfWork.targetId().getStringValue(),
                                        currentVersion.get().orElse(0L),
                                        unitOfWork.getVersion().getValueAsLong()));
                      }
                    });
                  }
                });
              }
            });
          } else {
            // Failed!
          }
        });
      } else {
        logger.error("Decide what to do"); // TODO
        // Failed to get connection - deal with it
      }
    });

    // TODO decide about to save scheduled commands here
    ////              uow.collectEvents().stream()
    ////            .filter(event -> event instanceof CommandSchedulingEvent) // TODO idempotency
    ////            .map(event -> (CommandSchedulingEvent) e)
    ////            .forEachOrdered(cs -> commandScheduler.schedule(commandId, cs));

    return uowSequence.get();

  }

  private void newVersionIsCurrentVersionPlus1(UnitOfWork unitOfWork, Long currentVersion) throws DbConcurrencyException {
    if ((currentVersion == null ? 0 : currentVersion) != unitOfWork.getVersion().getValueAsLong() - 1) {
      throw new DbConcurrencyException(
              String.format("ar_id = [%s], current_version = %d, new_version = %d",
                      unitOfWork.targetId().getStringValue(),
                      currentVersion, unitOfWork.getVersion().getValueAsLong()));
    }

  }

  String writeValueAsString(ObjectWriter writer, Object obj) {
    try {
      return writer.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("When writing to JSON", e);
    }
  }

  List<Event> readEvents(String eventsAsJson) {
    try {
      return Json.mapper.readerFor(eventsListTpe).readValue(eventsAsJson);
    } catch (IOException e) {
      throw new RuntimeException("When reading events list from JSON", e);
    }
  }

}