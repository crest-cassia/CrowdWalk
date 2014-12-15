# coding: utf-8

require 'rexml/document'
#require 'active_support/core_ext'
#require 'json'
#puts JSON.pretty_generate(Hash.from_xml(open(ARGV[0])))

ITEMS = [
  {type: 'text',    commentable: false, default: '',        name: 'comment'},
  {type: 'bool',    commentable: false, default: false,     name: 'debug'},
  {type: 'text',    commentable: false, default: 'none',    name: 'io_handler_type'},
  {type: '空行'},
  {type: 'text',    commentable: true,  default: 'map.xml',         name: 'map_file'},
  {type: 'text',    commentable: true,  default: 'generation.csv',  name: 'generation_file'},
  {type: 'text',    commentable: true,  default: 'scenario.csv',    name: 'scenario_file'},
  {type: 'text',    commentable: true,  default: 'pollution.csv',   name: 'pollution_file'},
  {type: 'text',    commentable: true,  default: 'camera.csv',      name: 'camera_file'},
  {type: '空行'},
  {type: 'numeric', commentable: true,  default: 1,         name: 'interval'},
  {type: 'numeric', commentable: true,  default: 0,         name: 'randseed'},
  {type: 'bool',    commentable: false, default: false,     name: 'random_navigation'},
  {type: 'text',    commentable: true,  default: 'density', name: 'speed_model'},
  {type: 'numeric', commentable: true,  default: 1,         name: 'loop_count'},
  {type: 'numeric', commentable: true,  default: 0,         name: 'exit_count'},
  {type: 'bool',    commentable: true,  default: true,      name: 'all_agent_speed_zero_break'},
  {type: '空行'},
  {type: 'bool',    commentable: false, default: false,     name: 'timer_enable'},
  {type: 'text',    commentable: false, default: 'log/timer.log',   name: 'timer_file'},
  {type: '空行'},
  {type: 'bool',    commentable: false, default: false,     name: 'time_series_log'},
  {type: 'text',    commentable: false, default: 'log',     name: 'time_series_log_path'},
  {type: 'numeric', commentable: false, default: 1,         name: 'time_series_log_interval'},
  {type: '空行'},
  {type: 'bool',    commentable: false, default: false,     name: 'damage_speed_zero_log'},
  {type: 'text',    commentable: false, default: 'log/damage_speed_zero.csv', name: 'damage_speed_zero_log_path'},
  {type: '空行'},
  {type: 'text',    commentable: true,  default: 'log/agent_movement_history.csv', name: 'agent_movement_history_file'},
  {type: 'text',    commentable: true,  default: 'log',     name: 'individual_pedestrians_log_dir'},
  {type: '空行'},
  {type: 'bool',    commentable: false, default: false,     name: 'record_simulation_screen'},
  {type: 'text',    commentable: true,  default: 'screenshots', name: 'screenshot_dir'},
  {type: 'bool',    commentable: true,  default: true,      name: 'clear_screenshot_dir'},
  {type: 'text',    commentable: false, default: 'png',     name: 'screenshot_image_type'},
  {type: '空行'},
  {type: 'numeric', commentable: true,  default: 0,         name: 'weight'},
  {type: 'numeric', commentable: true,  default: 2.0,       name: 'vertical_scale'},
  {type: 'numeric', commentable: true,  default: 1.0,       name: 'agent_size'},
  {type: 'numeric', commentable: true,  default: 1.0,       name: 'zoom'},
  {type: 'bool',    commentable: false, default: false,     name: 'hide_links'},
  {type: 'bool',    commentable: false, default: false,     name: 'density_mode'},
  {type: 'bool',    commentable: false, default: true,      name: 'change_agent_color_depending_on_speed'},
  {type: 'text',    commentable: false, default: 'none',    name: 'show_status'},
  {type: 'bool',    commentable: false, default: false,     name: 'show_logo'},
  {type: 'bool',    commentable: false, default: true,      name: 'show_3D_polygon'},
  {type: '空行'},
  {type: 'bool',    commentable: false, default: false,     name: 'simulation_window_open'},
  {type: 'bool',    commentable: false, default: false,     name: 'auto_simulation_start'},
]

if ARGV.size != 1
  puts "property_xml2json: XML形式のプロパティファイルをJSON形式に変換する。"
  puts "使い方: ruby property_xml2json.rb <変換元のプロパティファイル>"
  puts "        変換元のプロパティファイルと同じディレクトリに、変換結果のJSONファイルが作成される。"
  exit(1)
end
src_path = ARGV[0]
dst_path = src_path.sub(/\.xml$/i, '.json') 
if ! src_path.downcase.end_with?('.xml')
  puts "Error: XML形式のファイルを指定してください。"
  exit(1)
elsif ! File.exists?(src_path)
  puts "Error: '#{src_path}' ファイルが存在しません。"
  exit(1)
elsif File.exists?(dst_path)
  puts "Error: '#{dst_path}' ファイルがすでに存在しています。"
  exit(1)
end

doc = REXML::Document.new(open(src_path))
dirname = File.dirname(src_path)

properties = {}
properties['comment'] = doc.elements['properties/comment'].text if doc.elements['properties/comment']
doc.elements.each('properties/entry') {|entry| properties[entry.attributes['key']] = entry.text }

File.open(dst_path, 'w') {|file|
  file.puts '{'
  ITEMS.each do |item|
    if item[:type] == '空行'
      file.puts
      next
    end
    quote = (item[:type] == 'text' ? "'" : '')
    value = properties[item[:name]]
    if value
      if ['map_file', 'generation_file', 'scenario_file', 'pollution_file', 'camera_file'].include?(item[:name])
        value = File.basename(value) if File.dirname(value) == dirname
      end
      file.puts "    #{item[:name]}: #{quote}#{value}#{quote},"
    else
      file.puts "#{item[:commentable] ? '//' : ''}    #{item[:name]}: #{quote}#{item[:default]}#{quote},"
    end
  end
  file.puts '}'
}
