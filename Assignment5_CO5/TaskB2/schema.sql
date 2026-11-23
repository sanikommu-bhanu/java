CREATE DATABASE IF NOT EXISTS campus_test;
USE campus_test;
CREATE TABLE IF NOT EXISTS users (id VARCHAR(30) PRIMARY KEY, pwd VARCHAR(60) NOT NULL);
INSERT INTO users VALUES ('arun','secret123'),('admin','Adm!n#2026');
