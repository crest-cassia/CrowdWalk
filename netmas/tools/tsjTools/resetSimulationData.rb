# coding: utf-8
#

require 'mongo'

print "Are you sure to delete all data files and drop mongo? (y/N): "
if gets.chop != "y" then
	exit 0
end

# データベース削除
connection = Mongo::Connection.new
db = connection.db('test')
collection = db.collection('simulations')
collection.drop()
puts "Droped databases."


# ファイル削除
command = "rm -rf /mnt/exhdd/data/kanazawa*"
system command
puts "Deleted deployed data files."


