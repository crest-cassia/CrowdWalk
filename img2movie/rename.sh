#!/bin/sh

count=1

find . -maxdepth 2 -name '*.jpg' | while read jpg; do
    echo mv $jpg to $count.jpg
    mv "$jpg" img/capture$count.jpg
    count=`expr $count + 1`
done
