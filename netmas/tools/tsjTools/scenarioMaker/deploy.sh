#!/bin/sh

key=$1


TEMPORARY_DIR=./tmp/kanazawa-$key

#tar xfvz $TEMPORARY_DIR.tar.gz

cd $TEMPORARY_DIR 

ln -s ~/crowdwalktogis/generate-kml.sh ./generate-kml.sh
ln -s ~/crowdwalktogis/deploy.sh ./deploy.sh
./generate-kml.sh
./deploy.sh

cd ../../

#tar cfvz $TEMPORARY_DIR.tar.gz $TEMPORARY_DIR

ruby ./RubyLibs/deploy-mongo.rb $key

# 一時ディレクトリを削除
rm -rf $TEMPORARY_DIR


