CREATE DATABASE crabzilla OWNER user1;

\connect crabzilla ;

CREATE TABLE events (
      id CHAR(26) NOT NULL UNIQUE, -- ULID
      sequence BIGINT GENERATED ALWAYS AS IDENTITY UNIQUE,
      event_type VARCHAR(36) NOT NULL,
      event_payload JSON NOT NULL,
      state_type VARCHAR(36) NOT NULL,
      state_id VARCHAR(36) NOT NULL, -- ULID, CHAR or UUID
      version INTEGER NOT NULL,
      causation_id CHAR(26) NOT NULL REFERENCES events (id), -- ULID
      correlation_id CHAR(26) NOT NULL REFERENCES events (id), -- ULID
      UNIQUE (state_id, version)
     )
     -- PARTITION BY HASH (state_type)
     ;

CREATE INDEX state_id ON events USING HASH (state_id);
CREATE INDEX state_type ON events USING HASH (state_type);
-- CREATE INDEX sequence_idx ON events USING BRIN (sequence);
-- CREATE INDEX event_type ON events USING HASH (event_type);

CREATE TABLE subscriptions (
   name VARCHAR(100) PRIMARY KEY NOT NULL,
   sequence BIGINT
);

CREATE TABLE commands (
  state_id VARCHAR(36) NOT NULL, -- ULID, CHAR or UUID
  causation_id CHAR(26) NOT NULL, -- ULID
  last_causation_id CHAR(26) NOT NULL, -- ULID
  cmd_payload JSON NOT NULL
);
