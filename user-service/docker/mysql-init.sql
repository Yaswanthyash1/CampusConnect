DROP USER IF EXISTS 'clubuser'@'localhost';
DROP USER IF EXISTS 'clubuser'@'%';

DROP DATABASE IF EXISTS userdb;
CREATE DATABASE userdb;

CREATE USER 'clubuser'@'localhost' IDENTIFIED BY 'clubpass';
CREATE USER IF NOT EXISTS 'clubuser'@'%' IDENTIFIED BY 'clubpass';
GRANT ALL PRIVILEGES ON userdb.* TO 'clubuser'@'localhost';
GRANT ALL PRIVILEGES ON userdb.* TO 'clubuser'@'%';
FLUSH PRIVILEGES;

USE userdb;
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    srn VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(255) UNIQUE,
    name VARCHAR(255),
    email VARCHAR(255),
    password VARCHAR(255),
    role VARCHAR(255),
    domain VARCHAR(255),
    sem INT,
    dept VARCHAR(255),
    phoneno VARCHAR(255),
    gender VARCHAR(255),
    club VARCHAR(255)
);
