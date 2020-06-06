CREATE DATABASE example1_read OWNER user1;

\connect example1_read ;

-- read model

CREATE TABLE crabz_projections (
    ar_name VARCHAR(36) NOT NULL,
    stream_name VARCHAR(36) NOT NULL,
    last_uow INTEGER ,
    PRIMARY KEY (ar_name, stream_name)
);

CREATE INDEX idx_stream ON crabz_projections (ar_name, stream_name);

CREATE TABLE customer_summary (
    id INTEGER NOT NULL,
    name VARCHAR(36) NOT NULL,
    is_active BOOLEAN NOT NULL,
    PRIMARY KEY (id)
);

CREATE DATABASE example1_write OWNER user1;

\connect example1_write ;

-- write model (in production, it could be a different database instance)

CREATE TABLE crabz_units_of_work (
      uow_id BIGSERIAL,
      uow_events JSONB NOT NULL,
      cmd_id UUID NOT NULL,
      cmd_data JSONB NOT NULL,
      ar_name VARCHAR(36) NOT NULL,
      ar_id INTEGER NOT NULL,
      version INTEGER NOT NULL,
      inserted_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (ar_id, ar_name, version),
      UNIQUE (ar_id, uow_id),
      UNIQUE (ar_id, cmd_id)
    )
      PARTITION BY hash(ar_id) -- all related events within same partition
    ;

-- indexes

CREATE INDEX idx_cmd_id ON crabz_units_of_work (cmd_id);
CREATE INDEX idx_uow_id ON crabz_units_of_work (uow_id);
CREATE INDEX idx_ar ON crabz_units_of_work (ar_id, ar_name);

-- 3 partitions

CREATE TABLE crabz_units_of_work_0 PARTITION OF crabz_units_of_work
    FOR VALUES WITH (MODULUS 3, REMAINDER 0);
CREATE TABLE crabz_units_of_work_1 PARTITION OF crabz_units_of_work
    FOR VALUES WITH (MODULUS 3, REMAINDER 1);
CREATE TABLE crabz_units_of_work_2 PARTITION OF crabz_units_of_work
    FOR VALUES WITH (MODULUS 3, REMAINDER 2);

--  snapshots tables

CREATE TABLE crabz_customer_snapshots (
      ar_id INTEGER NOT NULL,
      version INTEGER,
      json_content JSONB NOT NULL,
      PRIMARY KEY (ar_id)
    );
