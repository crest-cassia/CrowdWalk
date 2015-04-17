#!/bin/sh

# ################################################################ 
# 
# 本ファイルは CrowdWalk の properties.json のディレクトリに
# シンボリックリンクをはってお使いください。
# 
# 
# 例：~/kanazawa/properties.json
# 
# $ ln -s ~/crowdwalktogis/quickstart.sh ~/kanazawa/simulate.sh
# $ cd ~/kanazawa
# $ ./simulate.sh  # シミュレーションがはじまります。 
# 
# ################################################################


# 環境変数の定義
#CROWDWALK=~/CrowdWalk/netmas
CTOG=$CROWDWALK/tools/tsjTools/crowdwalktogis


# 現在のディレクトリを保存
DIR=$(cd $(dirname $0); pwd)

SUB=${PWD##*/}
#echo $SUB

mkdir -p $CROWDWALK/log/$SUB/

# CrowdWalkのシミュレーション実行
cd $CROWDWALK
sh quickstart.sh $DIR/properties.json --cui
cd $DIR

# 一時ディレクトリの作成
mkdir -p log
rm ./log/*

# CrowdWalkのデータを移動
cp $CROWDWALK/log/$SUB/log_individual_pedestrians.csv ./log/

echo "Generated: ./log/log_individual_pedestrians.csv"


