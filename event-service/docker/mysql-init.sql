DROP USER IF EXISTS 'clubuser'@'localhost';
DROP USER IF EXISTS 'clubuser'@'%';

DROP DATABASE IF EXISTS eventdb;
CREATE DATABASE eventdb;

CREATE USER 'clubuser'@'localhost' IDENTIFIED BY 'clubpass';
CREATE USER IF NOT EXISTS 'clubuser'@'%' IDENTIFIED BY 'clubpass';
GRANT ALL PRIVILEGES ON eventdb.* TO 'clubuser'@'localhost';
GRANT ALL PRIVILEGES ON eventdb.* TO 'clubuser'@'%';
FLUSH PRIVILEGES;

USE eventdb;
CREATE TABLE IF NOT EXISTS event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    clubName VARCHAR(255),
    eventName VARCHAR(255),
    description VARCHAR(255),
    location VARCHAR(255),
    type VARCHAR(255),
    timestamp DATETIME,
    budget DOUBLE,
    registrationLink VARCHAR(255),
    banner LONGBLOB,
    venue VARCHAR(255)
);
