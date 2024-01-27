CREATE DATABASE crabzilla OWNER user1;

\connect crabzilla ;

-- TODO WIP

-- CREATE TABLE streams (
--     id VARCHAR(100) NOT NULL,
--     state_type VARCHAR(100) NOT NULL,
--     state_id VARCHAR(100) NOT NULL, --
--     UNIQUE (id)
-- );
--
-- CREATE INDEX state_id ON streams USING HASH (state_id);
-- CREATE INDEX state_type ON streams USING HASH (state_type);

CREATE TABLE events (
      id UUID NOT NULL UNIQUE, --
      sequence BIGINT GENERATED ALWAYS AS IDENTITY UNIQUE,
      event_type VARCHAR(100) NOT NULL,
      event_payload JSON NOT NULL,
--       stream_id VARCHAR(100) NOT NULL REFERENCES streams (id),
     state_type VARCHAR(36) NOT NULL,
     state_id VARCHAR(36) NOT NULL, --
      version INTEGER NOT NULL,
      causation_id UUID NOT NULL REFERENCES events (id), --
      correlation_id UUID NOT NULL REFERENCES events (id), --
      UNIQUE (state_id, version)
     )
     -- PARTITION BY HASH (state_type)
     ;

CREATE INDEX state_id ON events USING HASH (state_id);
CREATE INDEX state_type ON events USING HASH (state_type);
-- CREATE INDEX sequence_idx ON events USING BRIN (sequence);
-- CREATE INDEX event_type ON events USING HASH (event_type);

CREATE TABLE commands (
  state_id VARCHAR(36) NOT NULL, -- should be command_id
  causation_id UUID NULL REFERENCES events (id), --
  correlation_id UUID NULL REFERENCES events (id), --
  cmd_payload JSON NOT NULL
);

CREATE TABLE subscriptions (
                             name VARCHAR(100) PRIMARY KEY NOT NULL,
                             sequence BIGINT
);
