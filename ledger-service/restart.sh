#!/bin/bash
lsof -i :8083 | grep LISTEN | awk '{print $2}' | xargs kill -9 2>/dev/null
sleep 2
mvn clean spring-boot:run
