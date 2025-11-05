#!/usr/bin/env bash
set -euo pipefail

# Service list: name:jar_path:port
services=(
  "user-service:/opt/user-service.jar:8081"
  "request-service:/opt/request-service.jar:8083"
  "club-service:/opt/club-service.jar:8082"
  "project-service:/opt/project-service.jar:8084"
  "event-service:/opt/event-service.jar:8085"
  "frontend:/opt/frontend.jar:8080"
)

pids=()

start_service() {
  local name=$1 jar=$2 port=$3
  if [ ! -f "$jar" ]; then
    echo "Warning: $jar not found, skipping $name"
    return
  fi
  echo "Starting $name on port $port..."
  nohup java -jar "$jar" --server.port="$port" > "/opt/${name}.log" 2>&1 &
  pids+=($!)
}

for entry in "${services[@]}"; do
  IFS=':' read -r name jar port <<< "$entry"
  start_service "$name" "$jar" "$port"
  sleep 1
done

cleanup() {
  echo "Stopping services..."
  if [ ${#pids[@]} -gt 0 ]; then
    kill "${pids[@]}" 2>/dev/null || true
    wait "${pids[@]}" 2>/dev/null || true
  fi
  exit 0
}

trap 'cleanup' SIGINT SIGTERM

# Wait for background processes
wait

