#!/bin/sh

key=$1

# シミュレーションのターゲットとするプロジェクトディレクトリ
TARGET_PROJECT_DIR=$CROWDWALK/sample/kanazawaSnowVersionBase-trial

# CrowdWalkの結果を一時的に保存するディレクトリ
# デプロイする際に破棄される
TEMPORARY_DIR=./tmp/kanazawa-$key


# Maptagging 系のツール
MAPFILE_TAGGING_TOOLS=$CROWDWALK/tools/tsjTools/mapfileTaggingTools


rm -rf $TEMPORARY_DIR
mkdir -p $TEMPORARY_DIR
cp -rf $TARGET_PROJECT_DIR/* $TEMPORARY_DIR

cd $TEMPORARY_DIR

ruby -I $MAPFILE_TAGGING_TOOLS/libs/ $MAPFILE_TAGGING_TOOLS/main.rb -i map.xml -o map.xml -c ../../config.csv  -k $key

# CrowdWalk の実行
sh $CROWDWALK/quickstart.sh properties.json --cui

cd ../../

# 圧縮する
#tar cfvz $TEMPORARY_DIR.tar.gz $TEMPORARY_DIR


