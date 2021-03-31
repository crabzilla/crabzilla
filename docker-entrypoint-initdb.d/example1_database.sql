CREATE DATABASE example1_read OWNER user1;

CREATE DATABASE example1_write OWNER user1;

\connect example1_write ;

-- write model (in production, it could be a different database instance)

-- commands

CREATE TABLE commands (
      cmd_id BIGSERIAL NOT NULL,
      ar_id INTEGER NOT NULL,
      external_cmd_id UUID NOT NULL,
      correlation_id UUID NOT NULL,
      causation_id UUID NOT NULL,
      cmd_payload JSONB NOT NULL,
      inserted_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      UNIQUE (external_cmd_id),
      PRIMARY KEY (cmd_id)
    )
 ;

-- indexes

CREATE INDEX idx_ext_cmd_id ON commands (external_cmd_id);

-- events

CREATE TABLE events (
      event_id BIGSERIAL NOT NULL,
      event_payload JSONB NOT NULL,
      ar_name VARCHAR(36) NOT NULL,
      ar_id INTEGER NOT NULL,
      version INTEGER NOT NULL,
      cmd_id BIGINT,
      -- TODO correlation and causation ids
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

CREATE TABLE events_offset (
   id bool PRIMARY KEY DEFAULT TRUE,
   last_offset BIGINT,
   CONSTRAINT id CHECK (id)
);

INSERT INTO events_offset (last_offset) VALUES (0);

--  snapshots tables

CREATE TABLE customer_snapshots (
      ar_id INTEGER NOT NULL,
      version INTEGER,
      json_content JSONB NOT NULL,
      PRIMARY KEY (ar_id)
    );

\connect example1_read ;

-- read model

CREATE TABLE customer_summary (
    id INTEGER NOT NULL,
    name VARCHAR(36) NOT NULL,
    is_active BOOLEAN NOT NULL,
    PRIMARY KEY (id)
);

