package crabzilla.stack.vertx;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import crabzilla.model.*;
import crabzilla.stack.EventRepository;
import crabzilla.stack.ProjectionData;
import crabzilla.stack.SnapshotData;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.skife.jdbi.v2.util.LongColumnMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class JdbiJacksonEventRepository implements EventRepository {

  static final Logger logger = LoggerFactory.getLogger(JdbiJacksonEventRepository.class);

  static final String SQL_SELECT_UOW = "select * from units_of_work where uow_id = :uow_id ";

  static final String SQL_SELECT_AFTER_VERSION = "select version, uow_events " +
          "from units_of_work where ar_id = :ar_id and ar_name = :ar_name " +
          " and version > :version " +
          "order by version";

  static final String SQL_SELECT_CURRENT_VERSION =
          "select max(version) from units_of_work where ar_id = :ar_id and ar_name = :ar_name group by ar_id, ar_name";

  static final String SQL_INSERT_UOW = "insert into units_of_work " +
          "(uow_id, uow_events, cmd_id, cmd_data, ar_id, ar_name, version) " +
          "values (:uow_id, :uow_events, :cmd_id, :cmd_data, :ar_id, :ar_name, :version)";

  static final String UOW_ID = "uow_id";
  static final String UOW_EVENTS = "uow_events";
  static final String CMD_ID = "cmd_id";
  static final String CMD_DATA = "cmd_data";
  static final String AR_ID = "ar_id";
  static final String AR_NAME = "ar_name";
  static final String VERSION = "version";
  static final String UOW_SEQ_NUMBER = "uow_seq_number";

  private final String aggregateRootName;
  private final ObjectMapper mapper;
  private final DBI dbi;

  private final TypeReference<List<Event>> eventsListTpe =  new TypeReference<List<Event>>() {};

  public JdbiJacksonEventRepository(@NonNull final String aggregateRootName, @NonNull final ObjectMapper mapper,
                                    @NonNull final DBI dbi) {
    this.aggregateRootName = aggregateRootName;
    this.mapper = mapper;
    this.dbi = dbi;
  }

  @Override
  public Optional<UnitOfWork> get(@NonNull final UUID uowId) {

    final UnitOfWork uow = dbi
            .withHandle(h -> h.createQuery(SQL_SELECT_UOW)
                    .bind(UOW_ID, uowId.toString())
                    .map(new UnitOfWorkMapper()).first()
            );

    return Optional.ofNullable(uow);

  }

  @Override
  public List<ProjectionData> getAllSince(long sinceUowSequence, int maxResultSize) {

    logger.info("will load a maximum of {} units of work since sequence {}", maxResultSize, sinceUowSequence);

    final List<ProjectionData> projectionDataList = dbi
      .withHandle(new HandleCallback<List<ProjectionData>>() {

        final String sql = String.format("select uow_id, uow_seq_number, ar_id, uow_events " +
                        "from units_of_work where uow_seq_number > %d order by uow_seq_number limit %d",
                sinceUowSequence, maxResultSize);

        public List<ProjectionData> withHandle(Handle h) {
          return h.createQuery(sql)
                  .bind(UOW_SEQ_NUMBER, sinceUowSequence)
                  .map(new ProjectionDataMapper())
                  .list();
        }
      }
    );

    logger.info("Found {} units of work since sequence {}", projectionDataList.size(), sinceUowSequence);
    return projectionDataList;

  }

  @Override
  public Optional<SnapshotData> getAll(@NonNull final String id) {

    return getAllAfterVersion(id, new Version(0L));

  }

  @Override
  public Optional<SnapshotData> getAllAfterVersion(@NonNull final String id, @NonNull final Version version) {

    logger.info("will load {}", id);

    final List<SnapshotData> snapshotDataList = dbi
            .withHandle(h -> h.createQuery(SQL_SELECT_AFTER_VERSION)
                    .bind(AR_ID, id)
                    .bind(AR_NAME, aggregateRootName)
                    .bind(VERSION, version.getValueAsLong())
                    .map(new SnapshotDataMapper()).list()
            );

    logger.info("found {} units of work for id {} and version > {}",
            snapshotDataList.size(), id, version.getValueAsLong());

    if (snapshotDataList.isEmpty()) {
      return Optional.empty();
    }

    val finalVersion = snapshotDataList.get(snapshotDataList.size() - 1).getVersion();

    final List<Event> flatMappedToEvents = snapshotDataList.stream()
            .flatMap(sd -> sd.getEvents().stream()).collect(Collectors.toList());

    val result = new SnapshotData(finalVersion, flatMappedToEvents);

    return Optional.of(result);

  }

  @Override
  public Long append(@NonNull final UnitOfWork unitOfWork) {

    val uowSequence = new AtomicReference<Long>(0L);

    logger.info("appending uow to units_of_work with id {}", unitOfWork.targetId());

    dbi.inTransaction(TransactionIsolationLevel.SERIALIZABLE, (conn, status) -> {

      // TODO check if the command was not handled already

      Optional<Long> currentVersion = Optional.ofNullable(conn.createQuery(SQL_SELECT_CURRENT_VERSION)
              .bind(AR_ID, unitOfWork.targetId().getStringValue())
              .bind(AR_NAME, aggregateRootName)
              .map(LongColumnMapper.WRAPPER).first());

      newVersionIsCurrentVersionPlus1(unitOfWork, currentVersion.orElse(0L));

      val cmdAsJson = mapper.writerFor(Command.class).writeValueAsString(unitOfWork.getCommand());
      val eventsAsJson = mapper.writerFor(eventsListTpe).writeValueAsString(unitOfWork.getEvents());

      final Map<String, Object> keysMap = conn.createStatement(SQL_INSERT_UOW)
              .bind(UOW_ID, unitOfWork.getUnitOfWorkId().toString())
              .bind(UOW_EVENTS, eventsAsJson)
              .bind(CMD_ID, unitOfWork.getCommand().getCommandId().toString())
              .bind(CMD_DATA, cmdAsJson)
              .bind(AR_ID, unitOfWork.targetId().getStringValue())
              .bind(AR_NAME, aggregateRootName)
              .bind(VERSION, unitOfWork.getVersion().getValueAsLong())
              .executeAndReturnGeneratedKeys().first();

      // TODO decide about to also save scheduled commands

//              uow.collectEvents().stream()
//            .filter(event -> event instanceof CommandScheduling) // TODO idempotency here
//            .map(event -> (CommandScheduling) e)
//            .forEachOrdered(cs -> commandScheduler.schedule(commandId, cs));

      if (keysMap.size()==1) {
        uowSequence.set(((Number) keysMap.values().stream().findFirst().get()).longValue());
        return true;
      }

      throw new DbConcurrencyException(
              String.format("id = [%s], current_version = %d, new_version = %d",
                      unitOfWork.targetId().getStringValue(),
                      currentVersion, unitOfWork.getVersion().getValueAsLong()));

      }

    );

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


  class SnapshotDataMapper implements ResultSetMapper<SnapshotData> {
    @Override
    @SneakyThrows
    public SnapshotData map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
      final List<Event> events = mapper.readerFor(eventsListTpe).readValue(resultSet.getString(UOW_EVENTS));
      return new SnapshotData(new Version(resultSet.getLong(VERSION)), events);
    }
  }

  class ProjectionDataMapper implements ResultSetMapper<ProjectionData> {
    @Override
    @SneakyThrows
    public ProjectionData map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
      final List<Event> events = mapper.readerFor(eventsListTpe).readValue(resultSet.getString(UOW_EVENTS));
      return new ProjectionData(resultSet.getString(UOW_ID),
              resultSet.getLong(UOW_SEQ_NUMBER),
              resultSet.getString(AR_ID),
              events
      );
    }
  }

  class UnitOfWorkMapper implements ResultSetMapper<UnitOfWork> {
    @Override
    @SneakyThrows
    public UnitOfWork map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
      final Command command = mapper.readerFor(Command.class).readValue(resultSet.getString(CMD_DATA));
      final List<Event> events = mapper.readerFor(eventsListTpe).readValue(resultSet.getString(UOW_EVENTS));
      return new UnitOfWork(UUID.fromString(resultSet.getString(UOW_ID)), command,
              new Version(resultSet.getLong(VERSION)),
              events);

    }
  }

}