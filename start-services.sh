#!/bin/bash

echo "Starting club-service..."
cd club-service
./gradlew bootRun > club-service.log 2>&1 &
CLUB_PID=$!
cd ..

echo "Waiting for club-service to start..."
sleep 10

echo "Starting user-service..."
cd user-service
./gradlew bootRun > user-service.log 2>&1 &
USER_PID=$!
cd ..

echo "Services started. PIDs: club-service=$CLUB_PID, user-service=$USER_PID"
echo "Logs are in club-service.log and user-service.log"

# To view logs in real-time, use:
# tail -f club-service.log user-service.log
