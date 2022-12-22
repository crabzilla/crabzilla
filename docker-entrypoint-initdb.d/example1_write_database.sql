CREATE DATABASE ex1_crabzilla OWNER user1;

\connect ex1_crabzilla ;

CREATE TABLE events (
      sequence BIGSERIAL NOT NULL PRIMARY KEY,
      event_type VARCHAR(36) NOT NULL,
      event_payload JSON NOT NULL,
      state_type VARCHAR(36) NOT NULL,
      state_id VARCHAR(36) NOT NULL,
      version INTEGER NOT NULL,
      id CHAR(26) NOT NULL UNIQUE,
      causation_id CHAR(26) NOT NULL REFERENCES events (id),
      correlation_id CHAR(26) NOT NULL REFERENCES events (id),
      inserted_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      UNIQUE (state_id, version)
     )
;

CREATE INDEX sequence_idx ON events using brin (sequence);
CREATE INDEX state_id ON events (state_id);
CREATE INDEX state_name ON events (state_type);
CREATE INDEX event_type ON events (event_type);

CREATE TABLE subscriptions (
   name VARCHAR(100) PRIMARY KEY NOT NULL,
   sequence BIGINT
);

CREATE TABLE commands (
  state_id VARCHAR(36) NOT NULL,
  causation_id CHAR(26) NOT NULL,
  last_causation_id CHAR(26) NOT NULL,
  cmd_payload JSON NOT NULL
);

-- application data

INSERT INTO subscriptions (name, sequence) values ('crabzilla.example1.customer.SimpleProjector', 0);
INSERT INTO subscriptions (name, sequence) values ('crabzilla.example1.customer.BadProjector', 0);
INSERT INTO subscriptions (name, sequence) values ('crabzilla.example1.customer.CustomProjector', 0);

CREATE TABLE customer_summary (
    id UUID NOT NULL PRIMARY KEY,
    name VARCHAR(100),
    is_active BOOLEAN NOT NULL
);
