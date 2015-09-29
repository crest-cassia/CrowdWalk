# 二元配置の分散分析



# 交互作用


filename = "testdata1.csv"
if ARGV.size >= 1 then
	filename = ARGV[0]
end


# 交互作用
order_of_interactions = 2
if ARGV.size >= 2 then
	order_of_interactions = ARGV[1].to_i
end



# データのラベル
data_label = "data"
if ARGV.size >= 3 then
	data_label = ARGV[2]
end
data_label_index = -1



# debug mode 
debug = false
if ARGV.size >= 4 then
	if ARGV[3] == "debug" then
		debug = true
	end
end




# 因子名と水準名
factor_names = Array.new
levels_of = {}

File.open(filename) do |io|
	firstline = io.gets

	# 因子名の取得
	#firstline = csv_string.lines.first.chomp
	columns = firstline.split(",")

	columns.size.times do |index|
		factor_name = columns[index].chomp.strip

		if factor_name != data_label then
			factor_names.push factor_name
		else
			data_label_index = index					
		end
	end

	if debug then
		puts "#factors:"
		p factor_names
		puts ""
	end


	# 水準名の取得
	#csv_string_lines = csv_string.lines
	#csv_string_lines.shift
	#csv_string_lines.each do |line|
	io.each do |line|

		columns = line.chomp.split(',')

		factor_names.size.times do |index|
			factor_name = factor_names[index]
			column = columns[index].strip

			if levels_of.has_key? factor_name then 
				if !(levels_of[factor_name].include?(column)) then
					levels_of[factor_name].push column
				end
			else 
				levels_of.store(factor_name, [column])
			end
		end
	end

	if debug then
		puts "#levels:"
		p levels_of
		puts ""
	end
end


=begin
	levels_of = { 
		"A" => ["1", "2", "3"],
		"B" => ["1", "2"]
	}
=end




# データの保持
data = Array.new
File.open(filename) do |io|

	io.gets  # 一行目を取得

	#csv_string_lines.each do |line|
	io.each do |line|
		columns = line.chomp.split(',')
		
		obj = {}
		factor_names.size.times do |index|
			column = columns[index].strip

			obj.store(factor_names[index], column)

		end

		data_value = columns[data_label_index].to_f 
		obj.store(data_label, data_value)

		data.push obj
	end

=begin
	data = [
		{ "A" => "1", "B" => "1", data_label => 64 },
		{ "A" => "1", "B" => "1", data_label => 58 },
		{ "A" => "2", "B" => "1", data_label => 61 },
		{ "A" => "2", "B" => "1", data_label => 69 },
		{ "A" => "3", "B" => "1", data_label => 63 },
		{ "A" => "3", "B" => "1", data_label => 57 },
		{ "A" => "1", "B" => "2", data_label => 27 },
		{ "A" => "1", "B" => "2", data_label => 31 },
		{ "A" => "2", "B" => "2", data_label => 56 },
		{ "A" => "2", "B" => "2", data_label => 50 },
		{ "A" => "3", "B" => "2", data_label => 51 },
		{ "A" => "3", "B" => "2", data_label => 49 }
	]
=end

	#p data
end





# すべての因子をまとめる
stored_factors = {}



def sum array
	array.inject(0) do |result, i|
		result + i
	end 	
end



# 因子
class Factor
	def initialize name, freedom
		@name    = name
		@freedom = freedom
	end

	def result error_term=nil
		var = self.variance
		f   = self.f_value error_term

		"#{@name}, #{@sum_of_squares}, #{@freedom}, #{var}, #{f}"
	end

	def f_value error_term
		if error_term != nil
			@f_value  = self.variance / error_term.variance.to_f
		else
			@f_value  = "-"
		end		
	end

	def variance
		@sum_of_squares / @freedom.to_f
	end

	attr_reader :name, :freedom
	attr_accessor :sum_of_squares

end








# 平均変動
all_data = []
data.each do |h|
	all_data.push h[data_label]
end

correction_term	= Factor.new "M", 1
correction_term.sum_of_squares = sum(all_data) ** 2 / all_data.size

if debug then
	puts "# correction_term: done."
end





# 主効果
factor_names.each do |factor_name|

	# a による類別
	data_a_hash = {}
	data.each do |h| 
		key = "#{h[factor_name]}"
		if data_a_hash.has_key? key then
			data_a_hash[key].push h[data_label]
		else
			data_a_hash.store(key, [h[data_label]])
		end
	end
	data_a = data_a_hash.values



	a_term 			= Factor.new factor_name, data_a.size-1
	a_term.sum_of_squares = (data_a.inject(0) {|s, cls| s + sum(cls)**2 / cls.size.to_f} ) - correction_term.sum_of_squares

	stored_factors.store(factor_name, a_term)

	if debug then
		puts "# #{factor_name}: done."
	end

end





# 交互作用
#=begin
if order_of_interactions >= 2 then
	factor_names.combination(2) do |factor_name_a, factor_name_b|

		factor_name = "#{factor_name_a}x#{factor_name_b}"

		# axb による類別
		data_axb_hash = {}
		data.each do |h| 
			key = "#{h[factor_name_a]}#{h[factor_name_b]}"
			if data_axb_hash.has_key? key then
				data_axb_hash[key].push h[data_label]
			else
				data_axb_hash.store(key, [h[data_label]])
			end
		end
		data_axb = data_axb_hash.values


		a_term = stored_factors[factor_name_a]
		b_term = stored_factors[factor_name_b]

		axb_term        = Factor.new factor_name, a_term.freedom*b_term.freedom

		ab_sum_of_squares = (data_axb.inject(0) {|s, cls| s + sum(cls)**2 / cls.size.to_f} ) - correction_term.sum_of_squares
		axb_term.sum_of_squares = ab_sum_of_squares - (a_term.sum_of_squares + b_term.sum_of_squares) 


		stored_factors.store(factor_name, axb_term)

		if debug then
			puts "# #{factor_name}: done."
		end

	end
end
#=end



# 全データ
sum_term	= Factor.new "SUM", all_data.size - 1
sum_term.sum_of_squares = sum(all_data.map {|i| i*i}) - correction_term.sum_of_squares



# 残差項の計算
error_term 		= Factor.new "e", sum_term.freedom - stored_factors.values.inject(0) {|sum, i| sum + i.freedom }
error_term.sum_of_squares = sum_term.sum_of_squares - stored_factors.values.inject(0) {|sum, i| sum + i.sum_of_squares }

if debug then
	puts "# error_term: done."
end






# 分散分析表を出力
if debug then
	puts ""
	puts "#VA tables:"
	puts "factor, SS, f, V, F"
	puts "----------------------------"
end

puts correction_term.result
stored_factors.values.each do |factor|
	puts factor.result(error_term)
end
puts error_term.result

if debug then
	puts "----------------------------"
	puts "#{sum_term.name}, #{sum_term.sum_of_squares}, #{sum_term.freedom}, -, -"
end

