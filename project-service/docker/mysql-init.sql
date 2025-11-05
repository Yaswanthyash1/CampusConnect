DROP USER IF EXISTS 'clubuser'@'localhost';
DROP USER IF EXISTS 'clubuser'@'%';

DROP DATABASE IF EXISTS projectdb;
CREATE DATABASE projectdb;

CREATE USER 'clubuser'@'localhost' IDENTIFIED BY 'clubpass';
CREATE USER IF NOT EXISTS 'clubuser'@'%' IDENTIFIED BY 'clubpass';
GRANT ALL PRIVILEGES ON projectdb.* TO 'clubuser'@'localhost';
GRANT ALL PRIVILEGES ON projectdb.* TO 'clubuser'@'%';
FLUSH PRIVILEGES;

USE projectdb;
CREATE TABLE IF NOT EXISTS project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attachments VARCHAR(255),
    budget DOUBLE,
    category VARCHAR(255),
    clubname VARCHAR(255),
    created_at DATETIME(6),
    deliverables TEXT,
    description TEXT,
    enddate DATE,
    mentor VARCHAR(255),
    objectives TEXT,
    priority VARCHAR(255),
    projectname VARCHAR(255),
    startdate DATE,
    status VARCHAR(255),
    teamsize INT,
    technologies TEXT,
    updated_at DATETIME(6)
);
