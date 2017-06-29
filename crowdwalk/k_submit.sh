#!/bin/sh -x
#PJM --rsc-list "node=1"
#PJM --rsc-list "elapse=00:10:00"
#PJM --rsc-list "rscgrp=small"
#PJM --stg-transfiles all
#PJM --stgin  "build/libs/crowdwalk.jar ./crowdwalk.jar"
#PJM --stgin-dir  "sample/generatedTown ./input"
#PJM --stgout-dir ". ./%j"
#PJM -s

. /work/system/Env_base
export JAVA_HOME=/opt/klocal/openjdk7u45
export PATH=${JAVA_HOME}/bin:$PATH
export CLASSPATH=.:${JAVA_HOME}/jre/lib:${JAVA_HOME}/lib:${JAVA_HOME}/lib/tools.jar
export _JAVA_OPTIONS="-Xmx500m"

java -Dfile.encoding=UTF-8 -jar crowdwalk.jar -l Warn --cui input/gridTown00.array.prop.json -t out.json

