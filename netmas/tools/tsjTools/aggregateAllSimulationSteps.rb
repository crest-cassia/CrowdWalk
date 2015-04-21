require 'json'

# デプロイしたシミュレーションデータの結果の統計データを集計して表示するユーティリティースクリプト
# 


# デプロイ先ターゲットディレクトリのパス
target_dir_path = "/mnt/exhdd/data/"


# シミュレーションデータを格納する配列
#stats = Array.new


# ディレクトリのタイトルを配列に入れる
keys_all = Dir::entries( target_dir_path )

# カレントディレクトリと親ディレクトリのパスは除く
keys_all.delete(".")
keys_all.delete("..")


keys_best  = keys_all.select { |key| key[9,11] == "00111000000" }
keys_worst = keys_all.select { |key| key[9,11] == "01000000101" }



if ARGV[0] == "--best" then
	keys_using = keys_best
elsif ARGV[0] == "--worst" then
	keys_using = keys_worst
else
	keys_using = keys_all
end



# すべてのシミュレーションデータの steps と query を stats に格納する
stats = keys_using.map do |key|
	log_json_path = target_dir_path + key + "/log.json"
	
	log_json_data = File.open( log_json_path ) do |io|
		JSON.load(io)
	end

	steps_string = log_json_data['steps']
	steps_integer = steps_string.to_i
	
	average_time_string = log_json_data['average_time']
	evacuation_time_string = log_json_data['evacuation_time']
	
	{ 'steps'=>steps_integer, 'query'=>key, 'average_time'=>average_time_string, 'evacuation_time'=>evacuation_time_string }

end
	


# steps で sort
sorted_stats = stats.sort do |a, b|
	a['steps'] <=> b['steps']
end

=begin
# ソート済みの配列を表示
sorted_stats.each do |stat|
	puts stat
end
=end


# サイズを計算
stats_size = stats.size


# ステップ毎にまとめる
stats_groupby_steps = stats.group_by {|stat| stat['steps'] }

#=begin
stats_groupby_steps.sort.each do |pair|
	steps = pair.first
	item  = pair.last

	count = item.size
	percentage = (count * 100 / stats_size.to_f).round(3)

	typical_stat = item.first

	counting_bar           =  "*" * count
	evacuation_time_string = typical_stat['evacuation_time']
	typical_query          = typical_stat['query']

	typical_query          = typical_query[0..19]+"-"+typical_query[20,typical_query.length-1]

	puts "#{steps} steps (#{evacuation_time_string}):"
	puts "   #{count} times (#{percentage} %)"
	puts "   typicals: { #{typical_query} }"
end
#=end



# データの数を表示
puts "number of files: #{stats_size}" 



