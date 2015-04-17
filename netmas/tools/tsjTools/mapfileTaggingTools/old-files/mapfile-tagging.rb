# encoding: utf-8

# config.csv で指定したデータにしたがって、
# map.xml のファイル内のタグを追加するスクリプト
#
# 実行例：
# $ ruby -I ./crowdwalk-utils/libs/ mapfile-tagging.rb \
#     -i map-before.xml \
#     -o map-affter.xml \
#     -c config.csv
#

require 'MapfileOperator'
require 'optparse'


# コマンドライン引数を処理
def cmdline
	args = {}
	OptionParser.new do |parser|
		parser.on('-i VALUE', '--input VALUE', '引数付きオプション(必須)') {|v| args[:input] = v}
		parser.on('-o VALUE', '--output VALUE', '引数付きオプション(必須)') {|v| args[:output] = v}
		parser.on('-c [VALUE]', '--configure [VALUE]', '引数付きオプション(任意)') {|v| args[:config] = v}
		parser.parse!(ARGV)
	end 
	args
end

args = cmdline


# 必須のオプションが指定されていない場合は
# エラーメッセージをはいて強制終了
if args[:input]==nil || args[:output]==nil then
	STDERR.puts "Few arguments error. Read help using [-h] option."
	#STDERR.puts "$ ruby  -h"
	exit -1
end

# 必須オプションの処理
input_file_path  = args[:input]
output_file_path = args[:output]

# 任意オプションの処理
config_file_path   = args[:config]==nil ? "config.csv" : args[:config]



# 入力のXMLファイルを開く
operator = MapfileOperator.new input_file_path



# config ファイルを開く
File.open(config_file_path) do |io|
	puts "Configure File: #{config_file_path}"	
	
	# ファイルのすべての行を読み込み
	io.each do |line|
		puts line

		columns = line.split(',')

		# '#' はコメント行として読み飛ばす
		if !line.start_with?("#") then
			search_tag_name = columns[0]
			add_tag_name    = columns[1]
			percentage      = columns[2].to_i
		
			# search_tag_name タグの付いたリンクを取得し、ランダムで add_tag_name を追加
			operator.getLinksByTag(search_tag_name).each do |link|
		
				# 1 から 100 までの数をランダムで取得し r に保存
				r = rand(1..100)
				
				# percentage にしたがって
				if r <= percentage then 
	
					link.elements.delete_all("tag")
					#puts link

					link.addTag add_tag_name
					#puts link
				end

			end

		end

	end

end

# output_file_path にファイルを保存
operator.save output_file_path


# すべてのリンクの個数をカウントして出力
#count = operator.getAllLinks().count
#puts count


