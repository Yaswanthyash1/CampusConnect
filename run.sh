#!/bin/bash

if [ "$1" == 'clean' ]; then
    echo "Cleaning up..."
    cd request-service && ./gradlew clean
    cd ../user-service && ./gradlew clean
    cd ../frontend && ./gradlew clean
    cd ../club-service && ./gradlew clean
    cd ../project-service && ./gradlew clean
    exit 0
fi


# Start each microservice in the background and store their PIDs
(cd user-service && ./gradlew bootRun) &
(cd request-service && ./gradlew bootRun) &
(cd club-service && ./gradlew bootRun) &
(cd project-service && ./gradlew bootRun) &


# Function to kill all microservices
cleanup() {
    echo "Killing all microservices..."
    pkill -f "java"
    exit
}

cd frontend && ./gradlew bootRun

cleanup