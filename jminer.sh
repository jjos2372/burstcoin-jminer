#!/bin/sh
cd `dirname $0`
if [ -n "$JAVA_HOME" ]; then
  $JAVA_HOME/bin/java -jar ./jminer.jar $*
else
  java -jar ./jminer.jar $*
fi
cd $OLDPWD
