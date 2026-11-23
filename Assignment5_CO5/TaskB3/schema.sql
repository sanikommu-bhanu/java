CREATE DATABASE IF NOT EXISTS campus_test;
USE campus_test;
CREATE TABLE IF NOT EXISTS bus_pass (
    pass_id  VARCHAR(12) PRIMARY KEY,
    reg_no   VARCHAR(9)  NOT NULL,
    name     VARCHAR(60) NOT NULL,
    route_no INT         NOT NULL,
    amount   DOUBLE      NOT NULL CHECK (amount >= 0)
);
