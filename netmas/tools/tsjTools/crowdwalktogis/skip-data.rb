#!/usr/bin/ruby
# coding: utf-8
# 
# プリプロセスコード
# 入力されたファイルの
# 1 行目のデータを抜いて
# あるステップおきにデータを出力する
#
#

require 'date'


# 入力を処理
require 'optparse'

def cmdline
	args = {}
	OptionParser.new do |parser|
		parser.on('-i VALUE', '--input VALUE', '引数付きオプション(必須)') {|v| args[:input] = v}
		parser.on('-o VALUE', '--output VALUE', '引数付きオプション(必須)') {|v| args[:output] = v}
		parser.on('-m [VALUE]', '--every-m-id [VALUE]', '引数付きオプション(任意)') {|v| args[:ids] = v}
		parser.on('-n [VALUE]', '--every-n-steps [VALUE]', '引数付きオプション(任意)') {|v| args[:steps] = v}
		parser.parse!(ARGV)
	end 
	args
end

args = cmdline

if args[:input]==nil || args[:output]==nil then
	STDERR.puts "Few arguments error. Read help using [-h] option."
	STDERR.puts "$ ruby skip-data.rb -h"
	exit -1
end

# 必須オプションの処理
input_file_path    = args[:input]
output_file_path   = args[:output]
steps              = args[:steps]==nil ? 1 : args[:steps].to_i 
ids                = args[:ids]==nil ? 1 : args[:ids].to_i 


lineIndex = 0

lastSteps = 0

File.open(output_file_path, "w") do |o|

	File.open(input_file_path, "r") do |i|
		i.each do |line|
			words = line.split(",")	
			
			id   = words[0].to_i
			
			step = words[15].to_i
			lastSteps = step

			if lineIndex > 0 then
				
				if id%ids==0 && step%steps==0 then
					o.puts line
				#	puts line
				end

			end

			lineIndex += 1
		end
	end
end


File.open(output_file_path+".json", "w") do |o|
	o.puts "{ \"lastSteps\": #{lastSteps} }"
end

