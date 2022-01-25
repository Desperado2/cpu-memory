#!/bin/sh
kill -9 $(netstat -nlp | grep :19999 | awk '{print $7}' | awk -F"/" '{ print $1 }')
