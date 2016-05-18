#!/bin/sh

JAR=./build/libs/GisToCrowdwalk-all.jar

echo "java -Dfile.encoding=UTF-8 -jar $JAR $*"
java -Dfile.encoding=UTF-8 -jar $JAR $*
