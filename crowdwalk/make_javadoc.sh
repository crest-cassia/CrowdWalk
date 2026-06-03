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

not_work_LINK_EXTPACKAGES="
  -linkoffline http://jsonic.osdn.jp/1.0/as3/api http://jsonic.osdn.jp/1.0/as3/api
  -linkoffline http://docs.geotools.org/latest/javadocs http://docs.geotools.org/latest/javadocs
  -linkoffline http://www.atetric.com/atetric/javadoc/io.jeo/proj4j/0.1.1 http://www.atetric.com/atetric/javadoc/io.jeo/proj4j/0.1.1
"

"$JAVA_HOME/bin/javadoc" -d ./doc/javadoc -sourcepath ./src/main/java -encoding utf-8 -charset UTF-8 -subpackages nodagumi -classpath build/libs/crowdwalk.jar -notimestamp -windowtitle "crowdwalk API" -quiet -linksource -overview ./src/main/java/nodagumi/ananPJ/overview.html

# gradle が FAILURE exception を出してしまうのを回避する
exit 0
