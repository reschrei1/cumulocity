#!/bin/sh
(
while true
do
  echo Running the Cumulocity Linux Agent...
  java -cp 'cfg/*:lib/*' c8y.lx.agent.Agent
  sleep 1
done
) 2>&1 | logger
