#!/usr/bin/env ruby
# -*- coding: utf-8 -*-

$LOAD_PATH.unshift File.expand_path(File.dirname(__FILE__) + '/../lib')

require 'optparse'
require 'gtoc'
require 'cblxy'
require 'geo_utils'

# constants
MAX_ID = 1024 * 1024 * 1024

# variables
max_height, min_height = 5.0, -5.0
start_tag_number = 1
map_number = 9
default_latitude, default_longitude = nil, nil
map_type = "world"
meshcode_type = "undefined"

# default args
debug, config, input_directory, map_file, output_map_file,
  output_pollution_file, crowdwalk_coordinate, meshcode_tag = false, nil, nil, nil, nil, false, false

# parse command line options.
opts = OptionParser.new do |o|
  #o.on("-d", "--debug", "debug option") {|v| d = v}
  o.on("-c CONFIG_FILE", "--config=CONFIG_FILE",
       "specify the configuration files.") {|v| config = v}
  # o.on("-i INPUT_DIRECTORY", "--input=INPUT_DIRECTORY",
       # "specify the directory that includes input files.") {|v| input_directory = v}
  # o.on("-m MAP_FILE", "--map=MAP_FILE",
       # "specify the crowdwalk map file to add the flood area..") {|v| map_file = v}
  # o.on("-o OUTPUT_MAP_FILE", "--output_map=OUTPUT_MAP_FILE",
       # "specify a output map file name.") {|v| output_map_file = v}
  # o.on("-p POLLUTION_FILE", "--pollution=POLLUTION_FILE",
       # "specify a output pollution file name.") {|v| output_pollution_file = v}
  begin
    o.parse!
  rescue
    STDERR.puts "E: Invalid option!"
    puts "#{o}"
    exit
  end
end

if config.nil?
  STDERR.puts "E: no configuration!"
  exit
else
  require 'json'
  fjson = nil
  File.open(config, "r") { |f|
    fjson = JSON.parse(f.read)
  }
  fjson.each_key do |key|
    case key
    when "max_height" then max_height = fjson[key].to_f
    when "min_height" then min_height = fjson[key].to_f
    when "start_tag_number" then start_tag_number = fjson[key].to_i
    when "map_number" then map_number = fjson[key].to_i
    when "default_latitude" then default_latitude = fjson[key].to_f
    when "default_longitude" then default_longitude = fjson[key].to_f
    when "map_type" then map_type = fjson[key]
    when "debug" then debug = fjson[key] == "true" ? true : false
    when "input_directory" then input_directory = fjson[key]
    when "map_file" then map_file = fjson[key]
    when "output_map_file" then output_map_file = fjson[key]
    when "output_pollution_file" then output_pollution_file = fjson[key]
    when "meshcode_type" then meshcode_type = fjson[key]
    when "crowdwalk_coordinate" then crowdwalk_coordinate = fjson[key] == "true" ? true : false
    when "meshcode_tag" then meshcode_tag = fjson[key] == "true" ? true : false
    else STDERR.puts "E: invalid key in config file. #{key}"
    end
    p "D: key: #{key}, value: #{fjson[key]}"
  end
end
if input_directory.nil?
  STDERR.puts "E: invalid input directory!"
  STDERR.puts "#{opts}"
  exit
end
if map_file.nil?
  STDERR.puts "E: invalid map file!"
  STDERR.puts "#{opts}"
  exit
end
if output_map_file.nil?
  STDERR.puts "E: invalid output map file!"
  STDERR.puts "#{opts}"
  exit
end
if output_pollution_file.nil?
  STDERR.puts "E: invalid output pollution file!"
  STDERR.puts "#{opts}"
  exit
end
if meshcode_type == "undefined"
  STDERR.puts "E: meshcode_type is undefined."
  STDERR.puts "#{opts}"
  exit
end

WIDTH4 = 1.0
WIDTH6 = WIDTH4 / 8
WIDTH8 = WIDTH6 / 10
WIDTH9 = WIDTH8 / 2
WIDTH10 = WIDTH9 / 2
WIDTH11 = WIDTH10 / 2
HEIGHT4 = 40.0 / 60
HEIGHT6 = HEIGHT4 / 8
HEIGHT8 = HEIGHT6 / 10
HEIGHT9 = HEIGHT8 / 2
HEIGHT10 = HEIGHT9 / 2
HEIGHT11 = HEIGHT10 / 2

def parse4(code, latitude, longitude)
  latitude += code[0..1].to_f / 1.5
  longitude += code[2..3].to_f
  return latitude, longitude
end

def parse6(code, latitude, longitude)
  latitude, longitude = parse4(code, latitude, longitude)
  latitude += HEIGHT6 * code[4].to_f
  longitude += WIDTH6 * code[5].to_f
  return latitude, longitude
end

def parse8(code, latitude, longitude)
  latitude, longitude = parse6(code, latitude, longitude)
  latitude += HEIGHT8 * code[6].to_f
  longitude += WIDTH8 * code[7].to_f
  return latitude, longitude
end

def parse9(code, latitude, longitude)
  latitude, longitude = parse8(code, latitude, longitude)
  case code[8]
  when "1"
  when "2" then longitude += WIDTH9
  when "3" then latitude += HEIGHT9
  when "4"
    latitude += HEIGHT9
    longitude += WIDTH9
  else
    STDERR.puts "E: code[8] includes invalid String #{code[8]}. This must be " +
                "1~4."
    return nil, nil
  end
  return latitude, longitude
end

def parse10(code, latitude, longitude)
  latitude, longitude = parse9(code, latitude, longitude)
  case code[9]
  when "1"
  when "2" then longitude += WIDTH10
  when "3" then latitude += HEIGHT10
  when "4"
    latitude += HEIGHT10
    longitude += WIDTH10
  else
    STDERR.puts "E: code[9] includes invalid String #{code[9]}. This must be " +
                "1~4."
    return nil, nil
  end
  return latitude, longitude
end

def parse11(code, latitude, longitude)
  latitude, longitude = parse10(code, latitude, longitude)
  case code[10]
  when "1"
  when "2" then longitude += WIDTH11
  when "3" then latitude += HEIGHT11
  when "4"
    latitude += HEIGHT11
    longitude += WIDTH11
  else
    STDERR.puts "E: code[10] includes invalid String #{code[10]}. This must " +
                "be 1~4."
    return nil, nil
  end
  return latitude, longitude
end

#returned_value :: the coordinate of the left bottom corner.
def meshcode2coordinate(code)
  unless code.instance_of?(String)
    STDERR.puts "E: argument must be String."
    return nil
  end
  latitude = 0.0
  longitude = 100.0
  case code.length
  when 4
    latitude, longitude = parse4(code, latitude, longitude)
    return {latitude: latitude, longitude: longitude, width: WIDTH4,
            height: HEIGHT4}
  when 6
    latitude, longitude = parse6(code, latitude, longitude)
    return {latitude: latitude, longitude: longitude, width: WIDTH6,
            height: HEIGHT6}
  when 8
    latitude, longitude = parse8(code, latitude, longitude)
    return {latitude: latitude, longitude: longitude, width: WIDTH8,
            height: HEIGHT8}
  when 9
    latitude, longitude = parse9(code, latitude, longitude)
    return {latitude: latitude, longitude: longitude, width: WIDTH9,
            height: HEIGHT9}
  when 10
    latitude, longitude = parse10(code, latitude, longitude)
    return {latitude: latitude, longitude: longitude, width: WIDTH10,
            height: HEIGHT10}
  when 11
    latitude, longitude = parse11(code, latitude, longitude)
    return {latitude: latitude, longitude: longitude, width: WIDTH11,
            height: HEIGHT11}
  else
    STDERR.puts "E: invalid length of code: #{code.length}"
    return {latitude: nil, longitude: nil, width: nil, height: nil}
  end
end

def parsemap(e, ids)
  ids << e.attribute("id")
  e.elements.each("Node") { |i|
    ids << i.attributes["id"].to_i
  }
  e.elements.each("Link") { |i|
    ids << i.attributes["id"].to_i
  }
  e.elements.each("Group") { |i|
    parsemap(i, ids)
  }
end

def generate_uniq_id(ids, max_id)
  id = rand(max_id) while ids.include?(id)
  return id
end

def decimal2degree(decimal)
  degree = decimal.to_i
  minute = ((decimal - degree) * 60.0).to_i
  second = ((decimal - degree) * 60.0 - minute) * 60.0
  degree * 10000.0 + minute * 100.0 + second
end

#
# main
#

files = []
# for files in input_directory
Dir.entries(input_directory).each do |f|
  if /ht([0-9]{5}).dat/ =~ f
    files << {
      time: $1.to_i,
      path: File.join(input_directory, f)
    }
  end
end

# for each directories, picks up all area.
allfiles = []
require 'pathname'
path = Pathname.new(input_directory)
Dir.entries(path = Pathname.new(input_directory).parent).each do |dict|
  next if dict == "." || dict == ".."
  next unless File.directory?(File.join(path, dict))
  Dir.entries(File.join(path, dict)).each do |f|
    if /ht([0-9]{5}).dat/ =~ f
      allfiles << {
        time: $1.to_i,
        path: File.join(path, dict, f)
      }
    end
  end
end

# read map file and generate used IDs
require 'rexml/document'
doc = REXML::Document.new(File.open(map_file, "r"))
ids = []
parsemap(doc, ids)
ids = ids.uniq
p "D: ids size: #{ids.size}"

# data structure of alldata
# [{time: $SECONDS, flood: [{code: $CODESTRING, depth: $DEPTH}, ...]},
#  {time:$SECONDS, ...},
#  ...]
alldata = [{time: 0, flood:[]}]

valid, samearea = 0, 0
files.each do |f|
  p "D: time: #{f[:time]}, path: #{f[:path]}"
  if (match = alldata.select { |i| i[:time] == f[:time] }).length != 1
    alldata << {time: f[:time], flood: []}
  end
  index = alldata.index { |i| i[:time] == f[:time] }
  open(f[:path]) {|file|
    while l = file.gets
      sl = l.chomp.split(" ")
      if (mi = alldata[index][:flood].index { |i| i[:code] == sl[0] }).nil?
        alldata[index][:flood] << {code: sl[0], depth: sl[1].to_f}
        valid += 1
      else
        STDERR.puts "E: same area codes exist in same time!"
        STDERR.puts "E: #{alldata[index][:flood]}, code: #{sl[0]}"
        if alldata[index][:flood][mi][:depth] != sl[1].to_f
          STDERR.puts "E: #{alldata[index][:time]}, #{f[:time]}, " +
            "#{alldata[index][:flood][mi][:code]}, #{sl[0]}, " +
            "#{alldata[index][:flood][mi][:depth]}, #{sl[1].to_f}"
        end
        samearea += 1
        next
      end
    end
  }
end

alldata.sort { |x, y| x[:time] <=> y[:time] }
p "D: valid data size: #{valid}, same time & area data size: #{samearea}"

alltagcodes = []
tag_number = start_tag_number
allfiles.each do |f|
  open(f[:path]) do |file|
    while l = file.gets
      sl = l.chomp.split(" ")
      unless alltagcodes.map { |tc| tc[:code] }.include?(sl[0])
        alltagcodes << {tag: tag_number, code: sl[0]}
        tag_number += 1
      end
    end
  end
end


# create map file
File.open(output_map_file, "w") do |file|
  tail_lines = String.new
  tail_line = false
  File.open(map_file, "r") do |mfile|
    while l = mfile.gets
      tail_line = true if l.include?("</Group>")
      if tail_line
        tail_lines << l
      else
        file.puts l
      end
    end
  end

  alltagcodes.each do |tagcode|
    c = meshcode2coordinate(tagcode[:code])
    ne, sw = nil, nil
    if map_type == "world"
      if meshcode_type == "world"
        x, y = blxy(decimal2degree(c[:latitude]), decimal2degree(c[:longitude]), map_number)
        sw = {x: x, y: y}
        x, y = blxy(decimal2degree(c[:latitude] + c[:height]), decimal2degree(c[:longitude] + c[:width]), map_number)
        ne = {x: x, y: y}
      else  # "japan"
        world_x, world_y, world_z = convert_japan_to_world(c[:longitude], c[:latitude], 0.0)
        x, y = blxy(decimal2degree(world_y), decimal2degree(world_x), map_number)
        sw = {x: x, y: y}
        world_x, world_y, world_z = convert_japan_to_world(c[:longitude] + c[:width], c[:latitude] + c[:height], 0.0)
        x, y = blxy(decimal2degree(world_y), decimal2degree(world_x), map_number)
        ne = {x: x, y: y}
      end
    else
      if !default_latitude.nil? && !default_longitude.nil?
        sw = Geographic::gtoc(
          #latitude: c[:latitude] + c[:height],
          latitude: c[:latitude],
          longitude: c[:longitude],
          number: map_number,
          default_latitude: default_latitude,
          default_longitude: default_longitude,
          type: map_type)
        # p "D: c: #{c}, dlat: #{default_latitude}, dlon: #{default_longitude}" +
          # ", type: #{map_type}"
      else
        sw = Geographic::gtoc(
          #latitude: c[:latitude] + c[:height],
          latitude: c[:latitude],
          longitude: c[:longitude],
          number: map_number,
          type: map_type)
      end
      if sw[:x].nil? || sw[:y].nil?
        p "E: cannot get SW coordinate, code: #{tagcode[:code]}, coordinate: " +
          "#{c}, converted: #{sw}"
      end
      if !default_latitude.nil? && !default_longitude.nil?
        ne = Geographic::gtoc(
          latitude: c[:latitude] + c[:height],
          longitude: c[:longitude] + c[:width],
          number: map_number,
          default_latitude: default_latitude,
          default_longitude: default_longitude,
          type: map_type)
      else
        ne = Geographic::gtoc(
          latitude: c[:latitude] + c[:height],
          longitude: c[:longitude] + c[:width],
          number: map_number,
          type: map_type)
      end
    end
    if ne[:x].nil? || ne[:y].nil?
      p "E: cannot get NE coordinate, code: #{tagcode[:code]}, coordiante: " +
        "#{c}, converted: #{ne}"
    end
    id = generate_uniq_id(ids, MAX_ID)
    ids << id
    #p "  c: #{c}, sw: #{sw}, ne: #{ne}"
    if crowdwalk_coordinate
      south, west, north, east = -sw[:x], sw[:y], -ne[:x], ne[:y]
    else
      south, west, north, east = sw[:y], sw[:x], ne[:y], ne[:x]
    end
    file.puts <<-EOS
    <Area angle="0.0" id="#{id}" maxHeight="#{max_height}" minHeight="#{min_height}" pWestX="#{west}" pNorthY="#{north}" pEastX="#{east}" pSouthY="#{south}">
      <tag>#{tagcode[:tag]}</tag>
    EOS
    if meshcode_tag
      file.puts <<-EOS
      <tag>#{tagcode[:code]}</tag>
      EOS
      # <tag>#{tagcode[:code][4, 7]}</tag>
    end
    file.puts <<-EOS
    </Area>
    EOS
  end
  file.puts tail_lines
end

# create pollution file
File.open(output_pollution_file, "w") do |file|
  alldata.each do |a|
    str = "#{a[:time] * 60}"
    alltagcodes.each do |c|
      if (match = a[:flood].select { |i| i[:code] == c[:code] }).length != 1
        str << ",0.0"
      else
        str << ",#{match[0][:depth]}"
      end
    end
    file.puts "#{str}"
  end
end
