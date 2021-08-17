#!/bin/sh

# export CROWDWALK=/path/to/CrowdWalk/crowdwalk
if test "$CROWDWALK" = "" ; then
    #	CROWDWALK='.'
    	CROWDWALK=`dirname $0`
fi

# カレントディレクトリの CrowdWalk を優先
if test "$(dirname $0)" = "." ; then
	DIR='.'
else
        DIR=$CROWDWALK
fi

JAVA='java'
JAVAOPT="-Dfile.encoding=UTF-8 $JAVA_OPTS"
JAR=$DIR/build/libs/crowdwalk.jar

echo "$JAVA $JAVAOPT -Djdk.gtk.version=2 -jar $JAR $*"
$JAVA $JAVAOPT -Djdk.gtk.version=2 -jar $JAR $*
