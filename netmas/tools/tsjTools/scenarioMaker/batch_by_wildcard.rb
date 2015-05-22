#!/usr/bin/env ruby
# coding: utf-8

require 'benchmark'
require 'parallel'


BIN      = "#{ENV["CROWDWALK"]}/tools/tsjTools/scenarioMaker/start.sh"

bit_mask        = ARGV[0] # ex. "0333333111133333331111"
num_of_parallel = ARGV[1].to_i # ex. 100



@patternList = Array.new

def patternListUp(prefix, postfix)
	if postfix.size == 0 then
		@patternList.push prefix

	else
		char = postfix[0]
		nextPostfix = postfix[1..-1]

		if char == "0" || char == "1" then
			patternListUp(prefix + char, nextPostfix)
		else 
			patternListUp(prefix + "0", nextPostfix)
			patternListUp(prefix + "1", nextPostfix)
		end
	end

end

# すべてのビットパターンをリストアップ
patternListUp("", bit_mask)

# debugging 
#p @patternList



#=begin
# すべてのクエリーパターンを実行
result = Benchmark.realtime do

	# プロセスに分割
	Parallel.map(@patternList, :in_processes=>num_of_parallel) do |pattern|
	#@patternList.each do |pattern|

		cmd = "#{BIN} #{pattern}"
		puts cmd
		system(cmd)
	end

end


puts "execution_time: #{result}s"

#=end

