CREATE DATABASE ex1_crabzilla OWNER user1;

\connect ex1_crabzilla ;

CREATE TABLE projections (
   name TEXT PRIMARY KEY NOT NULL,
   sequence BIGINT
);

--  snapshots table

CREATE TABLE snapshots (
      state_id UUID NOT NULL,
      state_type text NOT NULL,
      version INTEGER,
      json_content JSON NOT NULL,
      PRIMARY KEY (state_id, state_type)
    )
    PARTITION BY hash(state_id, state_type)
;

-- 3 partitions

CREATE TABLE snapshots_0 PARTITION OF snapshots
    FOR VALUES WITH (MODULUS 3, REMAINDER 0);
CREATE TABLE snapshots_1 PARTITION OF snapshots
    FOR VALUES WITH (MODULUS 3, REMAINDER 1);
CREATE TABLE snapshots_2 PARTITION OF snapshots
    FOR VALUES WITH (MODULUS 3, REMAINDER 2);

-- commands

-- https://github.com/thenativeweb/commands-events/issues/1#issuecomment-385862281

CREATE TABLE commands (
      cmd_id UUID NOT NULL PRIMARY KEY,
      cmd_payload JSON NOT NULL,
      inserted_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    )
 ;

-- events

CREATE TABLE events (
      sequence BIGSERIAL NOT NULL,
      event_type TEXT NOT NULL,
      event_payload JSON NOT NULL,
      state_type text NOT NULL,
      state_id UUID NOT NULL,
      version INTEGER NOT NULL,
      id UUID NOT NULL,
      causation_id UUID NOT NULL,
      correlation_id UUID NOT NULL,
      inserted_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      UNIQUE (state_id, version) -- to help lookup by entity id ordered by version
    )
      PARTITION BY hash(state_id) -- all related events within same partition
    ;

CREATE INDEX sequence_idx ON events using brin (sequence);
CREATE INDEX state_name ON events (state_type);
--CREATE INDEX event_type ON events (event_type);

-- 3 partitions

CREATE TABLE events_0 PARTITION OF events
    FOR VALUES WITH (MODULUS 3, REMAINDER 0);
CREATE TABLE events_1 PARTITION OF events
    FOR VALUES WITH (MODULUS 3, REMAINDER 1);
CREATE TABLE events_2 PARTITION OF events
    FOR VALUES WITH (MODULUS 3, REMAINDER 2);

-- application data

CREATE TABLE customer_summary (
    id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO publications (name, sequence) values ('customers', 0);
INSERT INTO projections (name, sequence) values ('crabzilla.example1.customer.CustomersEventsProjector', 0);
INSERT INTO projections (name, sequence) values ('crabzilla.example1.customer.CustomersSlowEventsProjector', 0);
INSERT INTO projections (name, sequence) values ('crabzilla.example1.customer.FilteredEventsProjector', 0);
