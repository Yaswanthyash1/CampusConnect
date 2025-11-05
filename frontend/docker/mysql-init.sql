DROP USER IF EXISTS 'clubuser'@'localhost';
DROP USER IF EXISTS 'clubuser'@'%';

DROP DATABASE IF EXISTS clubdb;
CREATE DATABASE clubdb;

CREATE USER 'clubuser'@'localhost' IDENTIFIED BY 'clubpass';
CREATE USER IF NOT EXISTS 'clubuser'@'%' IDENTIFIED BY 'clubpass';
GRANT ALL PRIVILEGES ON clubdb.* TO 'clubuser'@'localhost';
GRANT ALL PRIVILEGES ON clubdb.* TO 'clubuser'@'%';
FLUSH PRIVILEGES;
