#!/bin/sh

"$JAVA_HOME/bin/javadoc" -d ./doc/javadoc -sourcepath ./src/main/java -encoding utf-8 -charset UTF-8 -subpackages nodagumi -notimestamp -windowtitle "crowdwalk API"

# gradle が FAILURE exception を発生するのを回避させるため
exit 0
