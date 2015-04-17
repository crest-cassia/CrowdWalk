#!/bin/sh

# ################################################################ 
# 
# 本ファイルは CrowdWalk の properties.json のディレクトリに
# シンボリックリンクをはってお使いください。
# 
# 
# 例：~/kanazawa/properties.json
# 
# $ ln -s ~/crowdwalktogis/deploy.sh ~/kanazawa/deploy.sh
# $ cd ~/kanazawa
# $ ./deploy.sh  # デプロイがはじまります。 
# 
# ################################################################


# 環境変数の定義
#EARTH=~/public_html/earth
DATADIR=/mnt/exhdd/data


# 現在のディレクトリを保存
DIR=$(cd $(dirname $0); pwd)

SUB=${PWD##*/}
#echo $SUB


# データ一式を Google Earth へとデプロイ
mkdir -p $DATADIR/  # なければつくる
mkdir -p $DATADIR/$SUB/
rm $DATADIR/$SUB/*
cp ./tmp/* $DATADIR/$SUB/


echo "Generated: $DATADIR/$SUB/log.kml"
echo "Generated: $DATADIR/$SUB/log.json"
echo "Generated: $DATADIR/$SUB/histgram.json"
echo "Generated: $DATADIR/$SUB/evacuation.json"


