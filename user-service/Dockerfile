# Multi-stage build for user-service
FROM eclipse-temurin:23-jdk AS builder
WORKDIR /workspace
# Copy Gradle wrapper and config first to leverage Docker layer caching
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
COPY src ./src
# Ensure wrapper is executable and build the boot jar
RUN chmod +x gradlew && ./gradlew --no-daemon clean bootJar -x test

# Runtime image (with MariaDB)
FROM eclipse-temurin:23-jre

# Install MariaDB server and client
RUN microdnf install -y mariadb-server mariadb && microdnf clean all || (apt-get update && apt-get install -y mariadb-server mariadb-client && rm -rf /var/lib/apt/lists/*)

WORKDIR /app
# Copy built jar
COPY --from=builder /workspace/build/libs/*-SNAPSHOT.jar app.jar
# Copy init SQL and entrypoint
COPY docker/mysql-init.sql /docker-entrypoint-initdb.d/init.sql
COPY docker/entrypoint.sh /usr/local/bin/entrypoint.sh

# Create data dir
RUN mkdir -p /var/lib/mysql /run/mysqld && chown -R mysql:mysql /var/lib/mysql /run/mysqld || true

# Expose service port and MySQL port
EXPOSE 8081 3306
ENV JAVA_OPTS=""

# Simple entrypoint: start MariaDB, init DB, then start app
RUN chmod +x /usr/local/bin/entrypoint.sh

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
