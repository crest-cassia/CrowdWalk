#!/bin/sh

# export CROWDWALK=/path/to/CrowdWalk/netmas
if test "$CROWDWALK" = "" ; then
	CROWDWALK='.'
fi

JAVA='java'
JAVAOPT='-Dfile.encoding=UTF-8'
DYLD=$CROWDWALK/libs
JAR=$CROWDWALK/build/libs/netmas.jar

OS=`uname -a`
case "$OS" in
    *"Darwin"*)
        echo " > Load Mac OS X libraries..."
        DYLD=$CROWDWALK/libs/macosx
        ;;
    *"CYGWIN"*"64"*)
        echo " > Load Windows amd64 libraries..."
        DYLD=$CROWDWALK/libs/windows/amd64
        ;;
    *"CYGWIN"*"i686"*)
        echo " > Load Windows i386 libraries..."
        DYLD=$CROWDWALK/libs/windows/i386
        ;;
    *"Linux"*"x86_64"*)
        echo " > Load linux amd64 libraries..."
        DYLD=$CROWDWALK/libs/linux/amd64
        ;;
    *"Linux"*"x86_32"*)
        echo " > Load linux i386 libraries..."
        DYLD=$CROWDWALK/libs/linux/i386
        ;;
    *)
        echo " > Current OS may not be supported..."
        echo " > Please check the architecture and libraries."
        exit 0
        ;;
esac

echo "$JAVA $JAVAOPT -Djava.library.path=$DYLD -jar $JAR $*"
$JAVA $JAVAOPT -Djava.library.path=$DYLD -jar $JAR $*
