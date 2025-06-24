#!/bin/sh
gradle installDist && docker build -t cookbook-app .
