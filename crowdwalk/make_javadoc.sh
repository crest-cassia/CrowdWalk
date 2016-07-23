#!/bin/bash

# quickstart.sh のタイムスタンプを commit された日時にする
TIME=`git log --pretty=format:%ci -n1`
STAMP=`date -d "$TIME" +"%y%m%d%H%M.%S"`
touch -t $STAMP quickstart.sh

JAVADOC_FILE="doc/javadoc/nodagumi/ananPJ/package-summary.html"

if [[ -f $JAVADOC_FILE ]]; then
    if [[ $JAVADOC_FILE -nt quickstart.sh ]]; then
        echo "Javadoc is up to date."
        exit 0
    fi
fi

"$JAVA_HOME/bin/javadoc" -d ./doc/javadoc -sourcepath ./src/main/java -encoding utf-8 -charset UTF-8 -subpackages nodagumi -notimestamp -windowtitle "crowdwalk API"

# gradle が FAILURE exception を出してしまうのを回避する
exit 0
