#!/bin/sh

#CP=build/libs/netmas.jar:build/libs/netmas-pathing.jar
CP=build/libs/netmas.jar
DYLD=libs
#JAVAOPT='-Xms1024M -Xmx1024M'
JAVAOPT='-Xms2048M -Xmx2048M -Dfile.encoding=UTF-8'
JAVA='java'
EXECUTABLE=main
GISEXECUTABLE='nodagumi.ananPJ.Editor.ImportGis'
#EXECUTABLE=nodagumi.ananPJ.NetworkMapEditor

OS=`uname -a`
case "$OS" in
    *"Darwin"*)
        echo " > Load Mac OS X libraries..."
        DYLD=libs/macosx
        CP=$CP:build/libs/netmas-pathing.jar
        ;;
    *"CYGWIN"*"64"*)
        echo " > Load Windows amd64 libraries..."
        DYLD=libs/windows/amd64
        CP=$CP;build/libs/netmas-pathing.jar
        ;;
    *"CYGWIN"*"i686"*)
        echo " > Load Windows i386 libraries..."
        DYLD=libs/windows/i386
        CP=$CP;build/libs/netmas-pathing.jar
        ;;
    *"Linux"*"x86_64"*)
        echo " > Load linux amd64 libraries..."
        DYLD=libs/linux/amd64
        CP=$CP:build/libs/netmas-pathing.jar
        ;;
    *"Linux"*"x86_32"*)
        echo " > Load linux i386 libraries..."
        DYLD=libs/linux/i386
        CP=$CP:build/libs/netmas-pathing.jar
        ;;
    *)
        echo " > Current OS may not be supported..."
        echo " > Please check the architecture and libraries."
        exit 0
        ;;
esac

if [ $# -eq 2 ]; then
    if [ "$2" = "cui" ]; then
        echo "$JAVA $JAVAOPT -Djava.library.path=$DYLD -cp $CP $EXECUTABLE cui $1"
        $JAVA $JAVAOPT -Djava.library.path=$DYLD -cp $CP $EXECUTABLE cui $1
    else
        echo "$JAVA $JAVAOPT -Djava.library.path=$DYLD -cp $CP $EXECUTABLE $1"
        $JAVA $JAVAOPT -Djava.library.path=$DYLD -cp $CP $EXECUTABLE $1
    fi
elif [ $# -eq 1 ]; then
    if [ "$1" = "gis" ]; then
        echo "$JAVA $JAVAOPT -Djava.library.path=$DYLD -cp $CP $GISEXECUTABLE"
        $JAVA $JAVAOPT -Djava.library.path=$DYLD -cp $CP $GISEXECUTABLE
    else
        echo "$JAVA $JAVAOPT -Djava.library.path=$DYLD -cp $CP $EXECUTABLE $1"
        $JAVA $JAVAOPT -Djava.library.path=$DYLD -cp $CP $EXECUTABLE $1
    fi
elif [ $# -eq 0 ]; then
    echo "$JAVA $JAVAOPT -Djava.library.path=$DYLD -cp $CP $EXECUTABLE"
    $JAVA $JAVAOPT -Djava.library.path=$DYLD -cp $CP $EXECUTABLE
else
    echo " >"
    echo " > usage:"
    echo " >  sh quickstart PROPERTIES_FILE [gui|cui]"
    echo " >"
fi
