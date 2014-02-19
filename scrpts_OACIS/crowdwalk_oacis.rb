require 'pp'
require "./file_generator"

include Math

digit = 2


dir = "sample/kamakura"
# id = `pwd | rev | awk -F \/ '{print $1}' | rev`.chomp
id = nil
# p id

# ARGV = [z1A,z1B,z2A,z2B,z3A,z3B,z4A,z4B,z5A,z5B,z6A,z6B,o5A,o5B]
#arr = 10 0.1 0.8 0.05 0.9 0.2 0.3 0.4 0.5 0.6 0.1 0.01 0.45 0.2 0.7
# p arg_arr = ARGV.slice(1,14).map {|x| x.to_f }

arg_arr =[]
arg_arr.push(ARGV[0].to_f)
arg_arr.push(ARGV[1].to_f)
arg_arr.push((1.0 - (ARGV[0].to_f + ARGV[1].to_f)).round(digit))

arg_arr.push(ARGV[2].to_f)
arg_arr.push(ARGV[3].to_f)
arg_arr.push((1.0 - (ARGV[2].to_f + ARGV[3].to_f)).round(digit))

arg_arr.push(ARGV[4].to_f)
arg_arr.push(ARGV[5].to_f)
arg_arr.push((1.0 - (ARGV[4].to_f + ARGV[5].to_f)).round(digit))

arg_arr.push(ARGV[6].to_f)
arg_arr.push(ARGV[7].to_f)
arg_arr.push((1.0 - (ARGV[6].to_f + ARGV[7].to_f)).round(digit))

arg_arr.push(ARGV[8].to_f)
arg_arr.push(ARGV[9].to_f)
arg_arr.push((1.0 - (ARGV[8].to_f + ARGV[9].to_f)).round(digit))

arg_arr.push(ARGV[10].to_f)
arg_arr.push(ARGV[11].to_f)
arg_arr.push((1.0 - (ARGV[10].to_f + ARGV[11].to_f)).round(digit))

arg_arr.push(ARGV[12].to_f)
arg_arr.push(ARGV[13].to_f)
arg_arr.push((1.0 - (ARGV[12].to_f + ARGV[13].to_f)).round(digit))

# pp arg_arr


p "debug: scenario file generation"
FileGenerator.generate_scenario(dir,"scenario.csv", id)
# p "debug: map file generation"
# FileGenerator.copy_map(dir,"2014_0109_kamakura11-3.xml", id)
# p "debug: gas(pollution) file generation"
# FileGenerator.copy_pollution(dir, "output_pollution.csv", id)
# p "debug: generation file generation "
FileGenerator.generate_gen(dir, "gen.csv", arg_arr, id)

# p "debug: property file generation"

#default
# FileGenerator.generate_property(dir, "properties.xml", "2014_0109_kamakura11-3",
# 																"gen","output_pollution", id, "scenario", ARGV[14].to_i)

# narrow
#FileGenerator.generate_property(dir, "properties.xml", "2014_0109_kamakura11-3_widthnarrow4",
										#						"gen","output_pollution", id, "scenario", ARGV[14].to_i)

# narrow 2_2_4 (wide)
FileGenerator.generate_property(dir, "properties.xml", "2014_0109_kamakura11-3_widthnarrow1_2_2_4",
 																"gen","output_pollution", id, "scenario", ARGV[14].to_i)

djava = '-Djava.library.path=libs/linux/amd64'
cpath = '-cp build/libs/netmas.jar:build/libs/netmas-pathing.jar'
command = 'java -Xms3072M -Xmx3072M ' + djava + ' ' + cpath + ' main cui'
if !id.nil?
	command = command + ' ' + dir + "/properties_#{id}.xml _output"
else
	command = command + ' ' + dir + "/properties.xml _output"
end

system(command)
