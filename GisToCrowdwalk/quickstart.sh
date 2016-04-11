#!/bin/sh

CP=.:build/libs/*
OS=`uname -a`
case "$OS" in
    *"CYGWIN"*)
        CP=".;build\libs\*"
        ;;
esac
echo "java -Dfile.encoding=UTF-8 -cp $CP nodagumi.ananPJ.ImportGis $*"
java -Dfile.encoding=UTF-8 -cp $CP nodagumi.ananPJ.ImportGis $*
