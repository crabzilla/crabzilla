
DROP TABLE if exists customer_summary ;

CREATE TABLE customer_summary (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(36) NOT NULL,
    is_active BOOLEAN NOT NULL,
    PRIMARY KEY (id)
    )