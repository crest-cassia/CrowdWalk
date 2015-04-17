#!/bin/sh

# ################################################################ 
# 
# 本ファイルは CrowdWalk の properties.json のディレクトリに
# シンボリックリンクをはってお使いください。
# 
# 
# 例：~/kanazawa/properties.json
# 
# $ ln -s ~/crowdwalktogis/generate-kml.sh ~/kanazawa/generate-kml.sh
# $ cd ~/kanazawa
# $ ./generate-kml.sh  # Kmlの生成処理がはじまります。 
# 
# ################################################################


# 環境変数の定義
CTOG=$CROWDWALK/tools/tsjTools/crowdwalktogis
TEMPLATE_FILE=$CTOG/templates/template.erb




# タイムステップを 60 ステップ毎飛ばしたログデータを作成する
ruby $CTOG/skip-data.rb -i ./log/log_individual_pedestrians.csv -o ./log/log.csv -n 10

mkdir -p tmp
rm ./tmp/*

# KMLに変換し一式のファイルを ./tmp 以下へ保存
ruby -I $CTOG/libs $CTOG/log2kml.rb -i ./log/log.csv -o ./tmp -t $TEMPLATE_FILE -e -s "2014-11-01T12:00:00Z"

echo 'Generated: ./tmp/log.kml'
echo 'Generated: ./tmp/log.json'
echo 'Generated: ./tmp/histgram.json'
echo 'Generated: ./tmp/evacuation.kml'



