#!/bin/sh
AGENT_PATH=/usr/local/src/cpu-test
kill -9 $(netstat -nlp | grep :19999 | awk '{print $7}' | awk -F"/" '{ print $1 }')
nohup java -jar -Dspring.config.location=$AGENT_PATH/application.yml -Xms128m -Xmx8092m  $AGENT_PATH/cpu-test-0.0.1-SNAPSHOT.jar >out.log 2>&1 &
