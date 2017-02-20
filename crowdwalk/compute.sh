#!/bin/bash
#PJM -L "node=8"
#PJM -L "elapse=00:05:00"
#PJM -j
#PJM -o "comput.stdout"
#PJM --mpi "proc=1"

cd ~/workspace/CrowdWalk/crowdwalk/src/main/java/nodagumi/Itk
export JAVA_HOME=/opt/klocal/openjdk7u45
export PATH=${JAVA_HOME}/bin:$PATH
export CLASSPATH=.:${JAVA_HOME}/jre/lib:${JAVA_HOME}/lib:${JAVA_HOME}/lib/tools.jar

make ./run3gt00a_cui

