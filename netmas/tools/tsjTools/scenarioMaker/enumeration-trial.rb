# シミュレーションパターンを組み合わせで列挙するスクリプト

argument = ARGV[0]
if argument != "-s" && argument != "-d" && argument != "-sd" then
	exit
end 



# 通行不可に関するキーを生成
# ただし、ベストとワーストだけ

#a = [0,1,2,3,4,5,6,7,8,9,10]
BEST_CONDITION  = "00111000000"
WORST_CONDITION = "01000000101"


result_a = [BEST_CONDITION, WORST_CONDITION]

result_a.each do |query_a|
	# 除雪に関しては 11C3 のすべてのパターンを列挙
	b = [11,12,13,14,15,16,17,18,19,20,21]
	result_b = b.combination(3).to_a.map do |array|
		str = ""
		b.each do |i|
			if array.include? i then
				str += "1"	
			else
				str += "0"
			end
		end

		str
	end


	result_b.each do |query_b|

		pattern = query_a + query_b

		command1 = ""
		#result.each do |pattern|
		
		if argument == "-s" then
			command1 = "./start.sh #{pattern}"	
			puts command1
			system command1
		
		elsif argument == "-d" then
			command2 = "./deploy.sh #{pattern}"
			puts command2
			system command2

		elsif argument == "-sd" then
			command1 = "./start.sh #{pattern}"	
			puts command1
			system command1

			command2 = "./deploy.sh #{pattern}"
			puts command2
			system command2

		end
	end
end

puts "************"
puts "************"
puts "************"
puts "************"
puts "************"
#system command1

