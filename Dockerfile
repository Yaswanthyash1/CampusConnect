# Stage 1: Builder
FROM eclipse-temurin:23-jdk AS builder

# Set working directory
WORKDIR /home/gradle/project

# Copy all files
COPY . .

# Install required packages (MySQL + sudo + bash utilities)
USER root
RUN ls
RUN apt-get update && \
    apt-get install -y sudo mysql-server mysql-client && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Start MySQL service and initialize database using your SQL script
# Make sure sql-workbench file exists and contains valid SQL commands
RUN service mysql start && \
    mysql -u root < /home/gradle/project/frontend/sql-workbench && \
    service mysql stop

# Build all microservices
RUN find . -name "gradlew" -exec chmod +x {} \; && \
    for d in user-service request-service club-service project-service event-service frontend; do \
        echo "Building $d" && \
        (cd "$d" && ./gradlew clean bootJar -x test --no-daemon); \
    done


# Stage 2: Runtime
FROM eclipse-temurin:23-jre

WORKDIR /opt

# Copy built JARs
COPY --from=builder /home/gradle/project/user-service/build/libs/*.jar /opt/user-service.jar
COPY --from=builder /home/gradle/project/request-service/build/libs/*.jar /opt/request-service.jar
COPY --from=builder /home/gradle/project/club-service/build/libs/*.jar /opt/club-service.jar
COPY --from=builder /home/gradle/project/project-service/build/libs/*.jar /opt/project-service.jar
COPY --from=builder /home/gradle/project/event-service/build/libs/*.jar /opt/event-service.jar
COPY --from=builder /home/gradle/project/frontend/build/libs/*.jar /opt/frontend.jar

# Copy startup script
COPY run.sh /opt/run.sh
RUN chmod +x /opt/run.sh

# Expose frontend port
EXPOSE 8080

# Run the script
CMD ["./run.sh"]


