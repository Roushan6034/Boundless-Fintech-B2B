#!/bin/bash
echo "Brutally killing all Java processes to clear zombies..."
killall -9 java 2>/dev/null
sleep 2
echo "Recompiling and starting fresh..."
mvn clean spring-boot:run
