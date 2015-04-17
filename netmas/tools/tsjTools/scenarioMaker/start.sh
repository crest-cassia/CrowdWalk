#!/bin/sh

key=$1

# シミュレーションのターゲットとするプロジェクトディレクトリ
TARGET_PROJECT_DIR=$CROWDWALK/sample/kanazawaSnowVersionBase-trial

TEMPORARY_DIR=./tmp/kanazawa-$key


rm -rf $TEMPORARY_DIR
mkdir -p $TEMPORARY_DIR
cp -rf $TARGET_PROJECT_DIR/* $TEMPORARY_DIR

cd $TEMPORARY_DIR

ruby -I ~/crowdwalk-utils/libs/ ~/crowdwalk-utils/mapfile-tagging-2.rb -i map.xml -o map.xml -c ../../config.csv  -k $key

# CrowdWalk の実行
$CROWDWALK/quickstart.sh properties.json --cui

cd ../../

# 圧縮する
#tar cfvz $TEMPORARY_DIR.tar.gz $TEMPORARY_DIR


