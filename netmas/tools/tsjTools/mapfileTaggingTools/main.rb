# encoding: utf-8

# config.csv で指定したデータにしたがって、
# map.xml のファイル内のタグを追加するスクリプト
#
# 実行例：
# $ ruby -I ./crowdwalk-utils/libs/ mapfile-tagging-2.rb \
#     -i map-before.xml \
#     -o map-affter.xml \
#     -c config.csv
#     -k 10001011101
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
		parser.on('-k VALUE', '--key VALUE', '引数付きオプション(必須)') {|v| args[:key] = v}

		parser.parse!(ARGV)
	end 
	args
end

args = cmdline


# 必須のオプションが指定されていない場合は
# エラーメッセージをはいて強制終了
if args[:input]==nil || args[:output]==nil || args[:key]==nil then
	STDERR.puts "Few arguments error. Read help using [-h] option."
	#STDERR.puts "$ ruby  -vim h"
	exit -1
end

# 必須オプションの処理
input_file_path  = args[:input]
output_file_path = args[:output]
key = args[:key]

# "011100101" => [false, true, true, ...]
using_flag = key.chars.map {|item| (item=="1")}
p using_flag


# 任意オプションの処理
config_file_path   = args[:config]==nil ? "config.csv" : args[:config]



# 入力のXMLファイルを開く
operator = MapfileOperator.new input_file_path



# config ファイルを開く
File.open(config_file_path) do |io|
	puts "Configure File: #{config_file_path}"	
	
	# ファイルのすべての行を読み込み
	io.each do |line|
		#puts line

		columns = line.split(',')

		# '#' はコメント行として読み飛ばす
		if !line.start_with?("#") then
			index    = columns[0].to_i
			tag_name = columns[1].chomp.to_s
			command  = columns[2].chomp.to_s
		
			if using_flag[index] then 
				
				# 3 列目のコマンドで比較
				if command == "ADD" then
					# 4 列目にかかれた
					options = columns[3].chomp.to_s
					puts "Add tag name #{options} to #{tag_name}"
					
					operator.getLinksByTag(tag_name).each do |link|	
						#p link
						link.addTag(options)
					end
					
				elsif command == "DELETE" then
					puts "Delete tag name #{tag_name}"

					operator.getLinksByTag(tag_name).each do |link|	
						#p link
						link.elements.delete_all("tag")
					end

				end

			end

		end

	end

end

# output_file_path にファイルを保存
operator.save output_file_path




