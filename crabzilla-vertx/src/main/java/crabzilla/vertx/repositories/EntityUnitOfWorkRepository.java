package crabzilla.vertx.repositories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import crabzilla.model.*;
import crabzilla.stack.DbConcurrencyException;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLRowStream;
import io.vertx.ext.sql.UpdateResult;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static crabzilla.vertx.repositories.VertxSqlHelper.*;
import static io.vertx.core.json.Json.mapper;

@Slf4j
public class EntityUnitOfWorkRepository {

    private static final String UOW_ID = "uow_id";
    private static final String UOW_EVENTS = "uow_events";
    private static final String CMD_DATA = "cmd_data";
    private static final String VERSION = "version";

    private final String aggregateRootName;
    private final JDBCClient client;

    private final TypeReference<List<DomainEvent>> eventsListTpe =  new TypeReference<List<DomainEvent>>() {};

    public EntityUnitOfWorkRepository(@NonNull Class<?> aggregateRootName, @NonNull JDBCClient client) {
      this.aggregateRootName = aggregateRootName.getSimpleName();
      this.client = client;
    }

    public void get(@NonNull final UUID uowId, @NonNull final Future<Optional<EntityUnitOfWork>> getFuture) {

      val SELECT_UOW_BY_ID = "select * from units_of_work where uow_id =? ";
      val params = new JsonArray().add(uowId.toString());

      client.getConnection(getConn -> {

        if (getConn.failed()) {
          getFuture.fail(getConn.cause());
          return;
        }

        val sqlConn = getConn.result();

        Future<ResultSet> resultSetFuture = Future.future();

        queryWithParams(sqlConn, SELECT_UOW_BY_ID, params, resultSetFuture);

        resultSetFuture.setHandler(resultSetAsyncResult -> {
          if (resultSetAsyncResult.failed()) {
            getFuture.fail(resultSetAsyncResult.cause());
            return;
          }

          ResultSet rs = resultSetAsyncResult.result();

          val rows = rs.getRows();

          if (rows.size() == 0 ) {
            getFuture.complete(Optional.empty());
          } else {
            for (JsonObject row : rows) {
              val command = Json.decodeValue(row.getString(CMD_DATA), EntityCommand.class);
              final List<DomainEvent> events = readEvents(row.getString(UOW_EVENTS));
              val uow = new EntityUnitOfWork(UUID.fromString(row.getString(UOW_ID)), command,
                      new Version(row.getLong(VERSION)), events);
              getFuture.complete(Optional.of(uow));
            }
          }

          sqlConn.close(done -> {
            if (done.failed()) {
              throw new RuntimeException(done.cause());
            }
          });

        });

      });

    }

    public void selectAfterVersion(@NonNull final String id, @NonNull final Version version,
                                   @NonNull final Future<SnapshotData> selectAfterVersionFuture) {

      log.info("will load id [{}] after version [{}]", id, version.getValueAsLong());

      val SELECT_AFTER_VERSION = "select uow_events, version from units_of_work " +
              " where ar_id = ? " +
              "   and ar_name = ? " +
              "   and version > ? " +
              " order by version ";

      val params = new JsonArray().add(id).add(aggregateRootName).add(version.getValueAsLong());

      client.getConnection(getConn -> {

        if (getConn.failed()) {
          selectAfterVersionFuture.fail(getConn.cause());
          return;
        }

        val sqlConn = getConn.result();

        Future<SQLRowStream> streamFuture = Future.future();

        queryStreamWithParams(sqlConn, SELECT_AFTER_VERSION, params, streamFuture);

        streamFuture.setHandler(ar -> {

          if (ar.failed()) {
            selectAfterVersionFuture.fail(ar.cause());
            return;
          }

          SQLRowStream stream = ar.result();

          val list = new ArrayList<SnapshotData>() ;

          stream
                  .resultSetClosedHandler(v -> {
                    // will ask to restart the stream with the new result set if any
                    stream.moreResults();

                  })
                  .handler(row -> {

                    val events = readEvents(row.getString(0));
                    val snapshotData = new SnapshotData(new Version(row.getLong(1)), events);
                    list.add(snapshotData);

                  }).endHandler(event -> {

            log.info("found {} units of work for id {} and version > {}",
                    list.size(), id, version.getValueAsLong());

            val finalVersion = list.size() == 0 ? new Version(0) :
                    list.get(list.size() - 1).getVersion();

            final List<DomainEvent> flatMappedToEvents = list.stream()
                    .flatMap(sd -> sd.getEvents().stream()).collect(Collectors.toList());

            selectAfterVersionFuture.complete(new SnapshotData(finalVersion, flatMappedToEvents));

            sqlConn.close(done -> {

              if (done.failed()) {
                throw new RuntimeException(done.cause());
              }

            });

          });

        });

      });
    }

    public void append(@NonNull final EntityUnitOfWork unitOfWork, Future<Long> appendFuture) {

      val SELECT_CURRENT_VERSION =
              "select max(version) as last_version from units_of_work where ar_id = ? and ar_name = ? ";

      val INSERT_UOW = "insert into units_of_work " +
              "(uow_id, uow_events, cmd_id, cmd_data, ar_id, ar_name, version) " +
              "values (?, ?, ?, ?, ?, ?, ?)";


      client.getConnection(conn -> {

        if (conn.failed()) {
          appendFuture.fail(conn.cause());
        }

        val sqlConn = conn.result();

        Future<Void> startTxFuture = Future.future();

        // start a transaction
        startTx(sqlConn, startTxFuture);

        startTxFuture.setHandler(startTxAsyncResult -> {
          if (startTxAsyncResult.failed()) {
            appendFuture.fail(startTxAsyncResult.cause());
            return;
          }

          // check current version  // TODO also check if command was not already processed given the commandId
          val params1 = new JsonArray()
                  .add(unitOfWork.targetId().getStringValue())
                  .add(aggregateRootName);

          Future<ResultSet> resultSetFuture = Future.future();

          queryWithParams(sqlConn, SELECT_CURRENT_VERSION, params1, resultSetFuture);

          resultSetFuture.setHandler(asyncResultResultSet -> {

            if (asyncResultResultSet.failed()) {
              appendFuture.fail(asyncResultResultSet.cause());
              return;
            }

            ResultSet rs = asyncResultResultSet.result();
            Long currentVersion = rs.getRows().get(0).getLong("last_version");

            currentVersion = currentVersion == null ? 0L : currentVersion;

            log.info("Found version  {}", currentVersion);

            // apply optimistic locking
            if (currentVersion != unitOfWork.getVersion().getValueAsLong() - 1) {

              val error = new DbConcurrencyException (
                      String.format("ar_id = [%s], current_version = %d, new_version = %d",
                              unitOfWork.targetId().getStringValue(),
                              currentVersion, unitOfWork.getVersion().getValueAsLong())) ;

              appendFuture.fail(error);

              // and close the connection
              sqlConn.close(done -> {
                if (done.failed()) {
                  throw new RuntimeException(done.cause());
                }
              });

              return ;
            }

            // if version is OK, then insert
            final String cmdAsJson = commandToJson(unitOfWork.getCommand());
            final String eventsListAsJson = listOfEventsToJson(unitOfWork.getEvents());

            val params2 = new JsonArray()
                    .add(unitOfWork.getUnitOfWorkId().toString())
                    .add(eventsListAsJson)
                    .add(unitOfWork.getCommand().getCommandId().toString())
                    .add(cmdAsJson)
                    .add(unitOfWork.targetId().getStringValue())
                    .add(aggregateRootName)
                    .add(unitOfWork.getVersion().getValueAsLong());

            Future<UpdateResult> updateResultFuture = Future.future();

            updateWithParams(sqlConn, INSERT_UOW, params2, updateResultFuture);

            updateResultFuture.setHandler(asyncResultUpdateResult -> {

              if (asyncResultUpdateResult.failed()) {
                appendFuture.fail(asyncResultUpdateResult.cause());
                return;
              }

              UpdateResult updateResult = asyncResultUpdateResult.result();

              Future<Void> commitFuture = Future.future();

              // commit data
              commitTx(sqlConn, commitFuture);

              commitFuture.setHandler(commitAsyncResult -> {

                if (commitAsyncResult.failed()) {
                  appendFuture.fail(commitAsyncResult.cause());
                  return;
                }

                appendFuture.complete(updateResult.getKeys().getLong(0));

                // and close the connection
                sqlConn.close(done -> {
                  if (done.failed()) {
                    throw new RuntimeException(done.cause());
                  }
                });
              });

            });

          });

        });

      });

    }

    String commandToJson(Command command) {
      try {
        val cmdAsJson = mapper.writerFor(Command.class).writeValueAsString(command);
        log.info("commandToJson {}", cmdAsJson);
        return cmdAsJson;
      } catch (JsonProcessingException e) {
        throw new RuntimeException("When writing commandToJson", e);
      }
    }

    String listOfEventsToJson(List<DomainEvent> events) {
      try {
        val cmdAsJson = mapper.writerFor(eventsListTpe).writeValueAsString(events);
        log.info("listOfEventsToJson {}", cmdAsJson);
        return cmdAsJson;
      } catch (JsonProcessingException e) {
        throw new RuntimeException("When writing listOfEventsToJson", e);
      }
    }

    List<DomainEvent> readEvents(String eventsAsJson) {
      try {
        log.info("eventsAsJson {}", eventsAsJson);
        return mapper.readerFor(eventsListTpe).readValue(eventsAsJson);
      } catch (IOException e) {
        throw new RuntimeException("When reading events list from JSON", e);
      }
    }

  }