CREATE DATABASE example1_write OWNER user1;

\connect example1_write ;

-- write model (in production, it could be a different database instance)

-- commands

CREATE TABLE commands (
      cmd_id UUID NOT NULL PRIMARY KEY,
      ar_id UUID NOT NULL,
      causation_id UUID NOT NULL,
      correlation_id UUID NOT NULL,
      cmd_payload JSONB NOT NULL,
      inserted_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    )
 ;

-- indexes

CREATE INDEX idx_ar_id ON commands (ar_id);

-- events

CREATE TABLE events (
      event_id BIGSERIAL NOT NULL,
      event_payload JSONB NOT NULL,
      ar_name VARCHAR(16) NOT NULL,
      ar_id UUID NOT NULL,
      version INTEGER NOT NULL,
      cmd_id UUID,
      inserted_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (ar_id, event_id)
    )
      PARTITION BY hash(ar_id) -- all related events within same partition
    ;

-- indexes

CREATE INDEX idx_cmd_id ON events (cmd_id);
CREATE INDEX idx_ar ON events (ar_name, ar_id);

-- 3 partitions

CREATE TABLE events_0 PARTITION OF events
    FOR VALUES WITH (MODULUS 3, REMAINDER 0);
CREATE TABLE events_1 PARTITION OF events
    FOR VALUES WITH (MODULUS 3, REMAINDER 1);
CREATE TABLE events_2 PARTITION OF events
    FOR VALUES WITH (MODULUS 3, REMAINDER 2);

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
