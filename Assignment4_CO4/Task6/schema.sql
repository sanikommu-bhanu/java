CREATE DATABASE IF NOT EXISTS campus_events;
USE campus_events;
CREATE TABLE IF NOT EXISTS registration (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_reg VARCHAR(9) NOT NULL,
    student_name VARCHAR(60) NOT NULL,
    event_name VARCHAR(40) NOT NULL,
    registered_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_student_event (student_reg, event_name)   -- DB-level guard against double registration
);
INSERT INTO registration (student_reg, student_name, event_name) VALUES
 ('192311001','Arun','Dance'),('192311002','Divya','Music'),('192311003','Kiran','Dance');
