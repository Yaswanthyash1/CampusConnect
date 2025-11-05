DROP USER IF EXISTS 'clubuser'@'localhost';
DROP USER IF EXISTS 'clubuser'@'%';

DROP DATABASE IF EXISTS clubdb;
CREATE DATABASE clubdb;

CREATE USER 'clubuser'@'localhost' IDENTIFIED BY 'clubpass';
CREATE USER IF NOT EXISTS 'clubuser'@'%' IDENTIFIED BY 'clubpass';
GRANT ALL PRIVILEGES ON clubdb.* TO 'clubuser'@'localhost';
GRANT ALL PRIVILEGES ON clubdb.* TO 'clubuser'@'%';
FLUSH PRIVILEGES;

USE clubdb;
CREATE TABLE IF NOT EXISTS club (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    clubName VARCHAR(255),
    description VARCHAR(255),
    faculty_id VARCHAR(255),
    clubType VARCHAR(255),
    head_srn VARCHAR(255),
    name VARCHAR(255),
    phoneno VARCHAR(255),
    dept VARCHAR(255),
    gender VARCHAR(255),
    sem INT,
    password VARCHAR(255)
);
