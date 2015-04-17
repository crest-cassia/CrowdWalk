# encoding: utf-8
#
# CrowdWalk のログデータを解析し、シミュレーションステップなどの情報を表示するツール


# 引数がない場合は異常終了
if ARGV.size == 0 then
	exit -1
end

# 第一引数にファイル名を解析対象のログファイル名を入力する 
filename = ARGV[0]

# シミュレーションステップの列番号
COLUMN_INDEX_SIMULATIONS_STEP_COLUMN = 15
# エージェント ID の列番号
COLUMN_INDEX_AGENT_ID = 0


# エージェント ID を格納するハッシュ
agent_ids = {}


# ログファイルをすべての行解析し、もっとも大きなシミュレーションステップを最終ステップとする
last_simulation_step = -1
File.open(filename, "r") do |io|
   io.each do |line|
      columns = line.split(",")

      current_step = columns[COLUMN_INDEX_SIMULATIONS_STEP_COLUMN].to_i
      last_simulation_step = [current_step, last_simulation_step].max

      current_agent_id = columns[COLUMN_INDEX_AGENT_ID].to_i
      

      if agent_ids.has_key?(current_agent_id) then
         agent_ids[current_agent_id] += 1
      else 
         agent_ids[current_agent_id] = 0
      end

   end
end


# エージェント総数
agent_number = agent_ids.size



# 最終ステップ数からシミュレーション時間を計算
simulation_time_seconds = last_simulation_step % 60
simulation_time_minutes = (last_simulation_step / 60) % 60
simulation_time_hours   = (last_simulation_step / 3600)


# エージェント数、シミュレーションステップ、シミュレーション時間を計算
puts "log file name: #{filename}"
puts "agent number: #{agent_number}"
puts "last simulation step: #{last_simulation_step}"
puts "last simulation time: #{simulation_time_hours}:#{simulation_time_minutes}:#{simulation_time_seconds}"


