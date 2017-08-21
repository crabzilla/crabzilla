
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

CREATE INDEX idx_ar_id ON units_of_work (ar_id);

DROP TABLE if exists events_channels ;

CREATE TABLE events_channels (
    channel_name VARCHAR(36) NOT NULL,
    uow_last_seq BIGINT ,
    PRIMARY KEY (channel_name)
    )
    ;