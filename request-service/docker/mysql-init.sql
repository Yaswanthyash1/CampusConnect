DROP USER IF EXISTS 'clubuser'@'localhost';
DROP USER IF EXISTS 'clubuser'@'%';

DROP DATABASE IF EXISTS requestdb;
CREATE DATABASE requestdb;

CREATE USER 'clubuser'@'localhost' IDENTIFIED BY 'clubpass';
CREATE USER IF NOT EXISTS 'clubuser'@'%' IDENTIFIED BY 'clubpass';
GRANT ALL PRIVILEGES ON requestdb.* TO 'clubuser'@'localhost';
GRANT ALL PRIVILEGES ON requestdb.* TO 'clubuser'@'%';
FLUSH PRIVILEGES;

USE requestdb;
CREATE TABLE IF NOT EXISTS request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id VARCHAR(255),
    type VARCHAR(255),
    description VARCHAR(255),
    file_path VARCHAR(255),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(255),
    clubName VARCHAR(255),
    is_completed BIT(1)
);
