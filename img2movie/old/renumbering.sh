#!/bin/sh

count=1
precount=1
maxcount=9999

while [ $precount -lt $maxcount ];
do
    if [ -f img/capture$precount.jpg ]; then
        echo mv img/capture$precount.jpg to img/capture$count.jpg
        mv img/capture$precount.jpg img/capture$count.jpg
        count=`expr $count + 1`
    fi
    precount=`expr $precount + 1`
done
