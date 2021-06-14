CREATE DATABASE example1_write OWNER user1;

\connect example1_write ;

--- APP TABLES

CREATE TYPE AGGREGATES_ENUM AS ENUM ('Customer');

-- projections

CREATE TABLE projections (
   name VARCHAR(36) PRIMARY KEY NOT NULL,
   last_offset BIGINT
);

INSERT INTO projections (name, last_offset) values ('nats-domain-events', 0);
INSERT INTO projections (name, last_offset) values ('nats-integration-events', 0);
INSERT INTO projections (name, last_offset) values ('customers', 0);

--  snapshots tables

CREATE TABLE customer_snapshots (
      ar_id UUID NOT NULL,
      version INTEGER,
      json_content JSONB NOT NULL,
      PRIMARY KEY (ar_id)
    );

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
      correlation_id BIGINT REFERENCES correlations(id),
      inserted_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    )
 ;

-- indexes

CREATE UNIQUE INDEX cmd_id_idx ON commands (cmd_id);

-- events

-- it must have version so it accepts event patching

CREATE TABLE events (
      sequence BIGINT NOT NULL,
      event_payload JSONB NOT NULL,
      ar_name AGGREGATES_ENUM NOT NULL,
      ar_id UUID NOT NULL,
      version INTEGER NOT NULL,
      cmd_id BIGINT,
      correlation_id BIGINT,
      inserted_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (sequence, ar_id)
--      ,
--      FOREIGN KEY (cmd_id) REFERENCES commands(id),
--      FOREIGN key (correlation_id) REFERENCES correlations(id)
    )
      PARTITION BY hash(ar_id) -- all related events within same partition
    ;

-- indexes

CREATE INDEX sequence_idx ON events (sequence); -- to help lookup by entity id
CREATE UNIQUE INDEX ar_version_idx ON events (ar_name, ar_id, version); -- to help lookup by entity id

-- 3 partitions

CREATE TABLE events_0 PARTITION OF events
    FOR VALUES WITH (MODULUS 3, REMAINDER 0);
CREATE TABLE events_1 PARTITION OF events
    FOR VALUES WITH (MODULUS 3, REMAINDER 1);
CREATE TABLE events_2 PARTITION OF events
    FOR VALUES WITH (MODULUS 3, REMAINDER 2);

-- experimenting a lock on sequence of event id

CREATE TABLE events_sequence (
  onerow_id bool PRIMARY KEY DEFAULT TRUE,
  sequence BIGINT NOT NULL,
  CONSTRAINT onerow_uni CHECK (onerow_id)
);

INSERT INTO events_sequence (sequence) values (1);

CREATE SEQUENCE events_sequence_sequence OWNED by events_sequence.sequence
