# Multi-stage build for request-service
FROM eclipse-temurin:23-jdk AS builder
WORKDIR /workspace
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
COPY src ./src
RUN chmod +x gradlew && ./gradlew --no-daemon clean bootJar -x test

FROM eclipse-temurin:23-jre
RUN microdnf install -y mariadb-server mariadb && microdnf clean all || (apt-get update && apt-get install -y mariadb-server mariadb-client && rm -rf /var/lib/apt/lists/*)
WORKDIR /app
COPY --from=builder /workspace/build/libs/*-SNAPSHOT.jar app.jar
COPY docker/mysql-init.sql /docker-entrypoint-initdb.d/init.sql
RUN mkdir -p /var/lib/mysql /run/mysqld && chown -R mysql:mysql /var/lib/mysql /run/mysqld || true
EXPOSE 8083 3306
ENV JAVA_OPTS=""
COPY docker/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
