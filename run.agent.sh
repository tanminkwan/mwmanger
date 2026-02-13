#!/bin/bash
export JAVA_HOME=/home/hennry/projects/mwmanger/temp_jdk/jdk8u482-b08
export PATH=$JAVA_HOME/bin:$PATH
export USER=`whoami`
export HOSTNAME=`hostname`
export WORK_DIR=`pwd`
export WEBTOBDIR=/home/hennry

ps -ef | grep mwagent.$USER | grep -v grep | awk '{print $2}' | xargs kill -9
nohup $JAVA_HOME/bin/java -cp $WORK_DIR/lib/*:$WORK_DIR/release/mwmanger-0.9.14.jar -Dname=mwagent.$USER -Xms128m -Xmx128m mwmanger.MwAgent > nohup.out 2>&1 &
