#!/bin/sh

CP=build/libs/GisToCrowdwalk.jar:.:gis/geotools-2.7-M3/gt-shapefile-2.7-M3.jar:gis/geotools-2.7-M3/gt-main-2.7-M3.jar:gis/geotools-2.7-M3/gt-swing-2.7-M3.jar:lib/jruby.jar
#CP=build/libs/ImportFromGIS.jar:.
OS=`uname -a`
case "$OS" in
    *"CYGWIN"*)
        CP=build/libs/GisToCrowdwalk.jar\;.\;gis/geotools-2.7-M3/gt-shapefile-2.7-M3.jar\;gis/geotools-2.7-M3/gt-main-2.7-M3.jar\;gis/geotools-2.7-M3/gt-swing-2.7-M3.jar\;lib/jruby.jar
        ;;
esac
echo "java -cp $CP nodagumi.ananPJ.Editor.ImportGis"
java -Dfile.encoding=UTF-8 -cp $CP nodagumi.ananPJ.Editor.ImportGis
