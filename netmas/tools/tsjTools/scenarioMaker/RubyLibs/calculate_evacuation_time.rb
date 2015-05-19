# coding: utf-8

require 'json'


# 避難時間の配列
evacuation_steps_array = Array.new


filename = "./log/agent_movement_history.csv"
File.open(filename) do |io|
	
	# 一行目は無視
	io.gets
	
	io.each do |line|
		column = line.split(",")

		evacuation_steps = column[7].to_i
		
		evacuation_steps_array.push evacuation_steps
	end
end


# 平均避難時間（average_steps） の計算
average_steps = evacuation_steps_array.inject(0.0) { |sum, i| sum += i.to_f } / evacuation_steps_array.size

# 避難完了時間（evacuation_steps） の計算
evacuation_steps = evacuation_steps_array.max



output = { "average_steps" => average_steps, "evacuation_steps" => evacuation_steps }


puts JSON.generate(output)

