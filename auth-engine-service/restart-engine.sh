#!/bin/bash
echo "Killing any stuck auth-engine-service..."
lsof -i :8082 | grep LISTEN | awk '{print $2}' | xargs kill -9 2>/dev/null
sleep 2
echo "Recompiling and starting the Auth Engine..."
mvn clean spring-boot:run
