CREATE DATABASE example1_write OWNER user1;

\connect example1_write ;

--- APP TABLES

-- projections

CREATE TABLE projections (
   name VARCHAR(36) PRIMARY KEY NOT NULL,
   last_offset BIGINT
);

INSERT INTO projections (name, last_offset) values ('nats-domain-events', 0);
INSERT INTO projections (name, last_offset) values ('nats-integration-events', 0);
INSERT INTO projections (name, last_offset) values ('customers', 0);

--  snapshots table

CREATE TABLE snapshots (
      ar_id UUID NOT NULL,
      version INTEGER,
      json_content JSONB NOT NULL,
      PRIMARY KEY (ar_id)
    )
    PARTITION BY hash(ar_id)
;

-- 3 partitions

CREATE TABLE snapshots_0 PARTITION OF snapshots
    FOR VALUES WITH (MODULUS 3, REMAINDER 0);
CREATE TABLE snapshots_1 PARTITION OF snapshots
    FOR VALUES WITH (MODULUS 3, REMAINDER 1);
CREATE TABLE snapshots_2 PARTITION OF snapshots
    FOR VALUES WITH (MODULUS 3, REMAINDER 2);

-- correlations table

CREATE TYPE CORRELATION_TYPE_ENUM AS ENUM ('Command', 'Event');

CREATE TABLE correlations (
      id BIGSERIAL PRIMARY KEY,
      msg_id BIGINT NOT NULL,
      msg_type CORRELATION_TYPE_ENUM NOT NULL,
      causation_id BIGINT NOT NULL,
      causation_type CORRELATION_TYPE_ENUM NOT NULL,
      correlation_id BIGINT NOT NULL,
      correlation_type CORRELATION_TYPE_ENUM NOT NULL
);

-- commands

-- https://github.com/thenativeweb/commands-events/issues/1#issuecomment-385862281

CREATE TABLE commands (
      id BIGSERIAL NOT NULL PRIMARY KEY,
      cmd_id UUID NOT NULL,
      cmd_payload JSONB NOT NULL,
      correlation_id BIGINT,
      inserted_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    )
 ;

-- indexes

CREATE UNIQUE INDEX cmd_id_idx ON commands (cmd_id);

-- events

-- it must have version so it accepts event patching

CREATE TABLE events (
      sequence BIGSERIAL NOT NULL,
      event_payload JSONB NOT NULL,
      ar_name text NOT NULL,
      ar_id UUID NOT NULL,
      version INTEGER NOT NULL,
      cmd_id BIGINT,
      correlation_id BIGINT,
      inserted_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      UNIQUE (ar_id, version) -- to help lookup by entity id ordered by version
    )
      PARTITION BY hash(ar_id) -- all related events within same partition
    ;

CREATE INDEX sequence_idx ON events using brin (sequence);

-- 3 partitions

CREATE TABLE events_0 PARTITION OF events
    FOR VALUES WITH (MODULUS 3, REMAINDER 0);
CREATE TABLE events_1 PARTITION OF events
    FOR VALUES WITH (MODULUS 3, REMAINDER 1);
CREATE TABLE events_2 PARTITION OF events
    FOR VALUES WITH (MODULUS 3, REMAINDER 2);

-- An implentation avoiding gaps in event sequence: https://dev.to/kspeakman/event-storage-in-postgres-4dk2

---- Append only
--CREATE RULE rule_event_nodelete AS
--ON DELETE TO events DO INSTEAD NOTHING;
--CREATE RULE rule_event_noupdate AS
--ON UPDATE TO events DO INSTEAD NOTHING;
--
---- notification
--CREATE FUNCTION NotifyEvent() RETURNS trigger AS $$
--
--    DECLARE
--        payload text;
--
--    BEGIN
--        -- { position }/{ tenantId }/{ streamId }/{ version }/{ event type }
--        SELECT CONCAT_WS( '/'
--                        , NEW.Position
--                        , NEW.TenantId
--                        , REPLACE(CAST(NEW.StreamId AS text), '-', '')
--                        , NEW.Version
--                        , NEW.Type
--                        )
--          INTO payload
--        ;
--
--        -- using lower case channel name or else LISTEN would require quoted identifier.
--        PERFORM pg_notify('eventrecorded', payload);
--
--        RETURN NULL;
--    END;
--$$ LANGUAGE plpgsql;
--
--CREATE TRIGGER trg_EventRecorded
--    AFTER INSERT ON Event
--    FOR EACH ROW
--    EXECUTE PROCEDURE NotifyEvent()
--;

-- transactional sequence number
--CREATE TABLE IF NOT EXISTS PositionCounter
--(
--    Position bigint NOT NULL
--);
--
--INSERT INTO PositionCounter VALUES (0);
--
---- prevent removal / additional rows
--CREATE RULE rule_positioncounter_noinsert AS
--ON INSERT TO PositionCounter DO INSTEAD NOTHING;
--CREATE RULE rule_positioncounter_nodelete AS
--ON DELETE TO PositionCounter DO INSTEAD NOTHING;
--
---- function to get next sequence number
--CREATE FUNCTION NextPosition() RETURNS bigint AS $$
--    DECLARE
--        nextPos bigint;
--    BEGIN
--        UPDATE PositionCounter
--           SET Position = Position + 1
--        ;
--        SELECT INTO nextPos Position FROM PositionCounter;
--        RETURN nextPos;
--    END;
--$$ LANGUAGE plpgsql;

