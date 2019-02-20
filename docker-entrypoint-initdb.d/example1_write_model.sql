
CREATE DATABASE example1_write OWNER user1;

\connect example1_write ;

CREATE TABLE units_of_work (
      uow_seq_number SERIAL,
	    uow_id UUID NOT NULL,
      uow_events JSON NOT NULL,
      cmd_id UUID NOT NULL,
      cmd_data JSON NOT NULL,
      ar_name VARCHAR(36) NOT NULL,
      ar_id INTEGER NOT NULL,
      version INTEGER,
      inserted_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (ar_id, uow_seq_number),
      UNIQUE (ar_id, uow_id),
      UNIQUE (ar_id, cmd_id)
    )
      PARTITION BY hash(ar_id) -- all related events within same partition
    ;

-- 3 partitions

CREATE TABLE units_of_work_0 PARTITION OF units_of_work
    FOR VALUES WITH (MODULUS 3, REMAINDER 0);
CREATE TABLE units_of_work_1 PARTITION OF units_of_work
    FOR VALUES WITH (MODULUS 3, REMAINDER 1);
CREATE TABLE units_of_work_2 PARTITION OF units_of_work
    FOR VALUES WITH (MODULUS 3, REMAINDER 2);

CREATE INDEX idx_cmd_id ON units_of_work (cmd_id);
CREATE INDEX idx_uow_id ON units_of_work (uow_id);
CREATE INDEX idx_ar ON units_of_work (ar_id, ar_name);
