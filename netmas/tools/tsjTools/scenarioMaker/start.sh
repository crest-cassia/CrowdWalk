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


# $key にしたがって Map ファイルにタグ付けする
ruby -I $MAPFILE_TAGGING_TOOLS/libs/ $MAPFILE_TAGGING_TOOLS/main.rb -i map.xml -o map.xml -c ../../config.csv  -k $key


# ログファイルのディレクトリがなければ作成
mkdir -p log

# CrowdWalk の実行
sh $CROWDWALK/quickstart.sh properties.json --cui # --log-level Error --tick tickfile.txt

# 避難時間の集計情報を _output.json へと出力
ruby $CROWDWALK/tools/tsjTools/scenarioMaker/RubyLibs/calculate_evacuation_time.rb > ./log/_output.json


cd ../../


mv $TEMPORARY_DIR/log/_output.json ./tmp/_output-$key.json
rm -rf $TEMPORARY_DIR

# 圧縮する
#tar cfvz $TEMPORARY_DIR.tar.gz $TEMPORARY_DIR



