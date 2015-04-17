require 'json'

# デプロイしたシミュレーションデータの結果をソートして並べるユーティリティースクリプト
# 


# デプロイ先ターゲットディレクトリのパス
target_dir_path = "/mnt/exhdd/data/"


# シミュレーションデータを格納する配列
stats = Array.new


# ディレクトリのタイトルを配列に入れる
keys = Dir::entries( target_dir_path )

# カレントディレクトリと親ディレクトリのパスは除く
keys.delete(".")
keys.delete("..")


# すべてのシミュレーションデータの steps と query を配列に格納する
keys.each do |key|
	log_json_path = target_dir_path + key + "/log.json"
	
	log_json_data = File.open( log_json_path ) do |io|
		JSON.load(io)
	end

	steps_string = log_json_data['steps']
	steps_integer = steps_string.to_i
	
	average_time_string = log_json_data['average_time']
	evacuation_time_string = log_json_data['evacuation_time']
	
	stats.push({ 'steps'=>steps_integer, 'query'=>key, 'average_time'=>average_time_string, 'evacuation_time'=>evacuation_time_string })

end
	


# steps で sort
sorted_stats = stats.sort do |a, b|
	a['steps'] <=> b['steps']
end

# ソート済みの配列を表示
sorted_stats.each do |stat|
	puts stat
end

# データの数を表示
puts "number of files: #{sorted_stats.size}" 

