#!/bin/sh

CP=build/libs/ImportFromGIS.jar:.:gis/geotools-2.7-M3/gt-shapefile-2.7-M3.jar:gis/geotools-2.7-M3/gt-main-2.7-M3.jar:gis/geotools-2.7-M3/gt-swing-2.7-M3.jar
#CP=build/libs/ImportFromGIS.jar:.
echo "java -cp $CP nodagumi.ananPJ.Editor.ImportGis"
java -Xms1024M -Xmx1024M -cp $CP nodagumi.ananPJ.Editor.ImportGis

