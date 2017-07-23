package crabzilla.vertx.repositories;

import com.fasterxml.jackson.core.type.TypeReference;
import crabzilla.model.Event;
import crabzilla.vertx.ProjectionData;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.jdbc.JDBCClient;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

@Slf4j
public class VertxProjectionRepository implements BiFunction<Long, Integer, List<ProjectionData>> {

  private final JDBCClient client;

  private final TypeReference<List<Event>> eventsListTpe =  new TypeReference<List<Event>>() {};

  public VertxProjectionRepository(@NonNull JDBCClient client) {
    this.client = client;
  }

  @Override
  public List<ProjectionData> apply(Long sinceUowSequence, Integer maxResultSize) {

    log.info("will load a maximum of {} units unitOfWork work since sequence {}", maxResultSize, sinceUowSequence);

    val SELECT_SINCE_UOW_SEQ = "select uow_id, uow_seq_number, ar_id, uow_events " +
            "from units_of_work where uow_seq_number > ? order by uow_seq_number limit %d";
    val result = new ArrayList<ProjectionData>();
    val params = new JsonArray().add(sinceUowSequence);

    // TODO check
    // https://github.com/vert-x3/vertx-examples/blob/master/jdbc-examples/src/main/java/io/vertx/example/jdbc/streaming/JDBCExample.java

    client.getConnection(getSqlConn -> {
      if (getSqlConn.succeeded()) {
        val sqlConnection = getSqlConn.result();
        val sql = String.format(SELECT_SINCE_UOW_SEQ, maxResultSize);
        sqlConnection.queryStreamWithParams(sql, params, stream -> {
          if (stream.succeeded()) {
            stream.result().handler(row -> {
              // uow_id, uow_seq_number, ar_id, uow_events
              val events = readEvents(row.getString(3));
              val projectionData = new ProjectionData(UUID.fromString(row.getString(0)), row.getLong(1),
                      row.getString(2), events);
              result.add(projectionData);
            });
          }
        }).setAutoCommit(false, setCommit -> {
          if (setCommit.succeeded()) {
            // OK!
          } else {
            // Failed!
          }
        }).close();
      } else {
        log.error("Decide what to do"); // TODO
        // Failed to get connection - deal with it
      }
    });

    log.info("Found {} units of work since sequence {}", result.size(), sinceUowSequence);
    return result;

  }

  private List<Event> readEvents(String eventsAsJson) {
    try {
      return Json.mapper.readerFor(eventsListTpe).readValue(eventsAsJson);
    } catch (IOException e) {
      throw new RuntimeException("When reading events list from JSON", e);
    }
  }

}