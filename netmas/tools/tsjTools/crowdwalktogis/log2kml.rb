#!/usr/bin/ruby
# coding: utf-8


# ライブラリをインクルード
require 'date'
require 'erb'
require 'json'
require 'optparse'

# ユーザーライブラリ用のディレクトリ
#lib_directory_path = $0.rpartition("/")[0] + "/libs/"

# ユーザーライブラリをインクルード
require 'gtoc'


# 出力用のファイル名を定義
LOG_KML_FILENAME         = 'log.kml'
STATS_JSON_FILENAME      = 'stats.json'
EVACUATION_JSON_FILENAME = 'evacuation.json'
HISTGRAM_JSON_FILENAME   = 'histgram.json'


# コマンドライン引数を処理
def cmdline
	args = {}
	OptionParser.new do |parser|
		parser.on('-i VALUE', '--input VALUE', '引数付きオプション(必須)') {|v| args[:input] = v}
		parser.on('-t VALUE', '--template VALUE', '引数付きオプション(必須)') {|v| args[:template] = v}
		parser.on('-o [VALUE]', '--output [VALUE]', '引数付きオプション(任意)') {|v| args[:output] = v}
		parser.on('-s [VALUE]', '--time-starting [VALUE]', '引数付きオプション(任意)') {|v| args[:time_starting] = v}
		parser.on('-e', '--evacuation', '引数付きオプション(任意)') {|v| args[:evacuation] = v}
		parser.parse!(ARGV)
	end 
	args
end

args = cmdline

if args[:input]==nil || args[:template]==nil then
	STDERR.puts "Few arguments error. Read help using [-h] option."
	STDERR.puts "$ ruby log2kml.rb -h"
	exit -1
end

# 必須オプションの処理
input_file_path    = args[:input]
template_file_path = args[:template]

# 任意オプションの処理
output_dir_path        = args[:output]==nil ? "." : args[:output]  
every                  = args[:every]==nil ? 1 : args[:every].to_i 
time_starting_string   = args[:time_starting]==nil ? "2014-05-01T00:00:00Z" : args[:time_starting]
output_evacuation_mode = args[:evacuation]==nil ? false : true

intervals_sec = 1 # 1ステップは 1秒

# output のディレクトリのパス末尾からスラッシュを取り除く
output_dir_path.strip!
last_char = output_dir_path[-1]
if last_char == "/" then 
	output_dir_path.chop!
end

#puts output_dir_path



# 時刻のフォーマットを定義
class DateTime
	def to_s
		self.strftime '%Y-%m-%dT%H:%M:%SZ'
	end
end


# 開始時間を設定
time_starting = DateTime::parse time_starting_string
	



# エージェント名のHashmap 
agent_names = {}


# 時刻の初期化
time = time_starting

datum = {}
agents = []

previousStep = 0
step = 0
File.open(input_file_path, "r") do |io|
	io.each do |line|
		words = line.split(",")	
		
		# 補正をかける
		delX = 0 #346.181553603 - 10.8 + 6  # (-32560) [目標] - (-32906.1815536025700) [実際]
		delY = 0 #-278.351600138 + 14.4 + 4 #  (-2375) [目標] - ( -2096.6483998620565) [実際]	

		agent_name =  words[0].to_s
		x    = -words[2].to_f + delX   # Cartesian.x = -CrowdWalk.y
		y    =  words[1].to_f + delY   # Cartesian.t =  CrowdWalk.x
		step =  words[15].to_i 

		if words[1].to_f !=0.0 && words[2].to_f !=0.0 then
		
			# 平面直角座標系からGIS系へ変換
			gis = Geographic::ctog(x,y,7)
			
			latitude  = gis[0]
			longitude = gis[1]
			altitude  = 0.0   	# 高度は 0 で固定

			if previousStep < step then 
				previousStep = step
			end

			time = time_starting + Rational(intervals_sec*step,24*60*60) 
			
			if !agent_names.key?(agent_name) then
				agent_names[agent_name] = agent_name
				agents.push({:started_time=>time, :name=>agent_name})
				
				datum[agent_name] = []
			end

			datum[agent_name].push({:step=>step, :when=>time.to_s, :agent_name=>agent_name, :longitude=>longitude, :latitude=>latitude, :altitude=>altitude})
					
		end
	end
end


# 個々のエージェントの避難完了時刻を計算する
evacuation = []

num_of_agent = 0
sum_of_evacuation_time = 0

agents.each do |agent|
	agent_name    = agent[:name]
	agent_log     = datum[agent_name]
		
	#io.puts agent_log
	started_time  = agent[:started_time]
	finished_time = DateTime.parse(agent_log.last()[:when])
	
	#io.puts "#{finished_time} #{started_time}"	
	diff = (finished_time - started_time)
		
	hours   = (diff*24).to_i
	minutes = ((diff*24*60).to_i) % 60
	seconds = ((diff*24*3600).to_i) % 60
		
	evacuation_time = (diff*24).to_f #"#{hours} 時間 #{minutes} 分 #{seconds} 秒" 
	num_of_agent += 1
	sum_of_evacuation_time += diff
	
	#io.puts "#{agent_name},#{evacuation_time}"
	evacuation.push({:agent_name=> agent_name, :time=> evacuation_time})

	if minutes < 10 then
		agent[:colorId] = 0
	elsif minutes < 15 then
		agent[:colorId] = 1
	elsif minutes < 20 then
		agent[:colorId] = 2
	else
		agent[:colorId] = 3
	end

	overhead = 100
	tmp_time = time_starting + Rational(intervals_sec*(step+overhead),24*60*60)  
	
	lastLongitude = agent_log[-1][:longitude]
	lastLatitude  = agent_log[-1][:latitude]
	lastAltitude  = agent_log[-1][:altitude]
	
	datum[agent_name].push({:step=>step, :when=>tmp_time.to_s, :agent_name=>agent_name, :longitude=>lastLongitude, :latitude=>lastLatitude, :altitude=>lastAltitude})
end
	



stats = {}
evacuation.each do |obj|
	name = obj[:agent_name]
	value = obj[:time]

	five_minutes = (value.to_f * 12).to_i.to_s
		
	if !stats.key?(five_minutes) then
		stats[five_minutes] = 0
	end 
	stats[five_minutes] += 1
	
end

if output_evacuation_mode then
	File.open(output_dir_path + "/evacuation.json", "w") do |io|
		io.puts JSON.pretty_generate evacuation
	end
end

File.open(output_dir_path+"/histgram.json", "w") do |io|
	sorted = stats.sort

	keys = ["時間"]
	values = [""]
	sorted.each do |tuple|
		key   = (tuple[0].to_i * 5).to_s
		value = tuple[1]

		keys.push key
		values.push value
	end

	data = [keys, values]
	io.puts data.to_json
end



# 避難完了までの時間を計算する
diff = (time - time_starting)

hours   = (diff*24).to_i
minutes = ((diff*24*60).to_i) % 60
seconds = ((diff*24*3600).to_i) % 60

evacuation_time = "#{hours} 時間 #{minutes} 分 #{seconds} 秒" 

diff = sum_of_evacuation_time / num_of_agent
hours   = (diff*24).to_i
minutes = ((diff*24*60).to_i) % 60
seconds = ((diff*24*3600).to_i) % 60
average_evacuation_time = "#{hours} 時間 #{minutes} 分 #{seconds} 秒"



# KML 出力用の変数としてまとめる
placemarks = []
agents.each do |agent|
	agent_name = agent[:name]

	placemark = {
		:name=>agent[:name], 
		:gxTracks=>[],
		:colorId=>agent[:colorId],
	}
	
	logs = datum[agent_name]
	logs.each do |log|
		step = log[:step]
		track = {
			:when      => log[:when],
			:longitude => log[:longitude],
			:latitude  => log[:latitude],
			:altitude  => log[:altitude],
		}		
		placemark[:gxTracks].push track
	end

	placemarks.push placemark
end



# シミュレーションのスタッツのJSONとしてファイル出力
finishedTime = time.to_s

# TODO:とりあえずの変更
# 避難時間とステップだけ60秒ごとじゃなくした
_json_data = File.open( input_file_path+".json" ) do |io|
	JSON.load(io)
end
steps = _json_data['lastSteps'].to_i
hours   = ( (steps/3600).to_i ) % 24
minutes = ( (steps/60).to_i ) % 60
seconds = (steps) % 60

evacuation_time = "#{hours} 時間 #{minutes} 分 #{seconds} 秒" 

puts "*** steps: #{steps}, evacuation_time: #{evacuation_time}"

info = {
	:started_time=>time_starting.to_s, 
	:finished_time=>finishedTime, 
	:evacuation_time=>evacuation_time,
	:average_time=>average_evacuation_time,
	:steps=>steps
}

# データ用のJSONを吐き出す
File.open(output_dir_path + "/log.json", "w") do |io|
	io.puts info.to_json
end



# 最終ステップは全エージェントのデータが必要なのでそれを追加
=begin
placemarks.each do |placemark|
	lastLongitude = placemark[:gxTracks][-1][:longitude]
	lastLatitude  = placemark[:gxTracks][-1][:latitude]
	lastAltitude  = placemark[:gxTracks][-1][:altitude]
	
	placemark[:gxTracks].push({:longitude=>lastLongitude, :latitude=>lastLatitude, :altitude=>lastAltitude, :when=>finishedTime})
	
end
=end


# KMLのビューを調整
#baseLatitude  = 36.61155630151515   # 36.61419947313253   #36.6126113819 
#baseLongitude = 136.60251810346196  # 136.61046211451932  #136.601943720
#baseRange     = 1308.6006439036912  # 2395.425053734429   #3452.25075268
#baseTilt      = 46.753090894196006   # 61.3658824125343    #66.9024150428
#baseRoll      = 0.0016971650917993776  # 0.0019404462580626541  #0.00035363047

        
# テンプレートにしたがってKMLに掃き出す
erb = ERB.new(IO.read(template_file_path))
File.open(output_dir_path + "/log.kml", "w") do |io|
	io.puts erb.result(binding)
end

