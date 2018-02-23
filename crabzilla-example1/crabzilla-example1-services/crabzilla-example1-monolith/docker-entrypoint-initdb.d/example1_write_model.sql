create database example1_write ;

use example1_write ;

DROP TABLE if exists units_of_work ;

CREATE TABLE units_of_work (
      uow_seq_number BIGINT AUTO_INCREMENT,
	    uow_id VARCHAR(36) NOT NULL,
      uow_events JSON NOT NULL,
      cmd_id VARCHAR(36) NOT NULL,
      cmd_data JSON NOT NULL,
      ar_name VARCHAR(36) NOT NULL,
      ar_id VARCHAR(36) NOT NULL,
      version NUMERIC,
      inserted_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (uow_seq_number, ar_name),
      UNIQUE (uow_id, ar_name),
      UNIQUE (cmd_id, ar_name)
    )
    PARTITION BY KEY(ar_name)
    ;

CREATE INDEX idx_cmd_id ON units_of_work (cmd_id);
CREATE INDEX idx_uow_id ON units_of_work (uow_id);
CREATE INDEX idx_ar_id ON units_of_work (ar_id);
