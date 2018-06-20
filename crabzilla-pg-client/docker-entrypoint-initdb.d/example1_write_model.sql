
CREATE DATABASE example1_write OWNER user1;

\connect example1_write ;

CREATE TABLE units_of_work (
      uow_seq_number BIGSERIAL,
	    uow_id UUID NOT NULL,
      uow_events JSON NOT NULL,
      cmd_id UUID NOT NULL,
      cmd_data JSON NOT NULL,
      ar_name VARCHAR(36) NOT NULL,
      ar_id VARCHAR(36) NOT NULL,
      version NUMERIC,
      inserted_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (uow_seq_number, ar_name),
      UNIQUE (uow_id, ar_name),
      UNIQUE (cmd_id, ar_name)
    )
--    PARTITION BY hash(ar_name)
    ;

CREATE INDEX idx_cmd_id ON units_of_work (cmd_id);
CREATE INDEX idx_uow_id ON units_of_work (uow_id);
CREATE INDEX idx_ar_id ON units_of_work (ar_id);
