require 'pp'
require 'rexml/document'
require 'rexml/formatters/pretty'
require 'csv'

module FileGenerator

GENERATION_PATTERN = ["EACH", "RANDOM", "EACHRANDOM", "RANDOMALL", 
											"TIMEEVERY", "LINER_GENERATE_AGENT_RATIO"]

	# generate map file for crowdwalk
	def self.generate_map(dirname="2links", filename="map.xml")
		doc = REXML::Document.new
		doc << REXML::XMLDecl.new('1.0', 'UTF-8', 'no')
		doc << REXML::DocType.new("properties", "SYSTEM \"http://java.sun.com/dtd/properties.dtd\"")
		# properties.add_element("entry", {'key' => ''}).add_text ""
		# doc.write STDOUT
		doc.write(File.new(filename, "w"))
	end

	# copy map file for crowdwalk
	def self.copy_map(dirname="2links", filename="map.xml", id=nil)
		if !id.nil?
			origin = dirname + "/" + filename
			filename.slice!('.xml')
			copy = dirname + "/" + filename + "_#{id}.xml"
			command = "cp #{origin} #{copy}"
			system(command)
			if dirname.include?("/")
				str = dirname.split("/")
				dirname = str.join("\\/")
			end

			system("sed -i -e \"s/#{dirname}\\/gen2/#{dirname}\\/gen_#{id}/g\" #{copy}")
			system("sed -i -e \"s/#{dirname}\\/output_pollution/#{dirname}\\/output_pollution_#{id}/g\" #{copy}")
			system("sed -i -e \"s/#{dirname}\\/scenario/#{dirname}\\/scenario_#{id}/g\" #{copy}")
		end
	end

	def self.copy_pollution(dirname="2links", filename="pollution.csv", id=nil)
		if !id.nil?
			origin = dirname + "/" + filename
			filename.slice!('.csv')
			copy = dirname + "/" + filename + "_#{id}.csv"
			command = "cp #{origin} #{copy}"
			system(command)
		end
	end

	# generate generation file for crowdwalk
	def self.generate_gen(dirname="2links", filename="gen.csv", ratio=[], id=nil)
		if !id.nil?
			filename.slice!('.csv')
			gen_file = dirname + "/" + filename + "_#{id}.csv"
		else
			gen_file = dirname + "/" + filename
		end

		# generation_moji_pattern(gen_file, 0.5, 0.5, 1.0, "DENSITY")
		# generation_kamakura_pattern(gen_file, 0.333, 0.333, 0.333, "DENSITY")
		# size of ratio is 21 
		generation_kamakura_pattern2(gen_file, ratio[0], ratio[1], ratio[2], ratio[3], ratio[4], 
			ratio[5], ratio[6], ratio[7], ratio[8], ratio[9], ratio[10], ratio[11], ratio[12], ratio[13], 
			ratio[14], ratio[15], ratio[16], ratio[17], ratio[18], ratio[19], ratio[20], "DENSITY")
	end


	# generate scenario file for crowdwalk
	def self.generate_scenario(dirname="2links", filename="scenario.csv", id=nil)
		if id.nil?
			CSV.open("#{dirname}/"+filename, "w") do |csv|
				csv << [1, 0, "START", nil,"18:00", nil, nil]#start"
			end
		else
			filename.slice!(".csv")
			CSV.open("#{dirname}/"+filename+"_#{id}.csv", "w") do |csv|
				csv << [1, 0, "START", nil,"18:00", nil, nil]#start"
			end
		end
	end

	# generate property file for crowdwalk
	def self.generate_property(	dirname="2links", filename="properties.xml", map="map-width-1", gen="gen",
															gas="gas", id=nil, scenario="scenario", seed=2525)
		if !id.nil?
			filename.slice!(".xml")
			filename = filename + "_#{id}.xml"
		end
		doc = REXML::Document.new
		doc << REXML::XMLDecl.new('1.0', 'UTF-8')
		doc << REXML::DocType.new("properties", "SYSTEM \"http://java.sun.com/dtd/properties.dtd\"")

		# sample/kitakyushu
		properties = doc.add_element("properties", {})
		properties.add_element("comment").add_text "NetmasCuiSimulator"
		properties.add_element("entry", {'key' => "debug"}).add_text "false"
		properties.add_element("entry", {'key' => "io_handler_type"}).add_text "none"
		mapfile = dirname + "/" + map
		mapfile += id.nil? ? ".xml" : "_#{id}.xml"
		properties.add_element("entry", {'key' => "map_file"}).add_text mapfile
		gasfile = dirname + "/" + gas
		gasfile += id.nil? ? ".csv" : "_#{id}.csv"
		properties.add_element("entry", {'key' => "pollution_file"}).add_text gasfile
		genfile = dirname + "/" + gen
		genfile += id.nil? ? ".csv" : "_#{id}.csv"
		properties.add_element("entry", {'key' => "generation_file"}).add_text genfile
		scenariofile = dirname + "/" + scenario
		scenariofile += id.nil? ? ".csv" : "_#{id}.csv"
		properties.add_element("entry", {'key' => "scenario_file"}).add_text scenariofile
		properties.add_element("entry", {'key' => "timer_enable"}).add_text "false"
		timer_file = dirname + "/" + "timer"
		timer_file += id.nil? ? ".log" : "_#{id}.log"
		properties.add_element("entry", {'key' => "timer_file"}).add_text timer_file
		properties.add_element("entry", {'key' => "interval"}).add_text "0"
		# properties.add_element("entry", {'key' => "addr"})#.add_text ""
		# properties.add_element("entry", {'key' => "port"})#.add_text ""
		# properties.add_element("entry", {'key' => "serialize_file"}).add_text "/tmp/serialized.xml"
		# properties.add_element("entry", {'key' => "serialize_interval"}).add_text "60"
		# properties.add_element("entry", {'key' => "deserialized_file"}).add_text "/tmp/serialized.xml"
		properties.add_element("entry", {'key' => "randseed"}).add_text "#{seed}"
		properties.add_element("entry", {'key' => "random_navigation"}).add_text "false"
		properties.add_element("entry", {'key' => "speed_model"}).add_text "density"
		# properties.add_element("entry", {'key' => "density_density_speed_model_macro_timestep"}).add_text "10"
		properties.add_element("entry", {'key' => "time_series_log"}).add_text "true"
		time_series_log = dirname + "/" + "time_series"
		time_series_log += id.nil? ? ".log" : "_#{id}.log"
		properties.add_element("entry", {'key' => "time_series_log_path"}).add_text time_series_log
		properties.add_element("entry", {'key' => "damage_speed_zero_log"}).add_text "true"
		damage_speed_zero_log_path = dirname + "/" + "damage_speed_zero"
		damage_speed_zero_log_path += id.nil? ? ".csv" : "_#{id}.csv"
		properties.add_element("entry", {'key' => "damage_speed_zero_log_path"}).add_text damage_speed_zero_log_path
		properties.add_element("entry", {'key' => "time_series_log_interval"}).add_text "1"
		properties.add_element("entry", {'key' => "loop_count"}).add_text "1"
		properties.add_element("entry", {'key' => "exit_count"}).add_text "0"
		properties.add_element("entry", {'key' => "all_agent_speed_zero_break"}).add_text "true"
		# properties.add_element("entry", {'key' => "exit_count"}).add_text "1200"

		# properties.add_element("entry", {'key' => ""}).add_text ""
		# doc.write STDOUT

		# pretty_formatter = REXML::Formatters::Pretty.new(4)
		# output = StringIO.new
		# pretty_formatter.write(doc, File.new(dirname+"/"+filename, "w"))
		# # pp output.string
		f = File.new(dirname+"/"+filename, "w")
		doc.write(f)
		f.flush
	end

	private

	# for kamakura
	# ratioA: NAGHOSHI_CLEAN_CENTER_EXIT,
	# ratioB: OLD_MUNICIPAL_HOUSING_EXIT, 
	# ratioC: KAMAKURA_Jr_HIGH_EXIT
	def self.generation_kamakura_pattern(filename, ratioA, ratioB, ratioC, model)
		zaimoku_num = [1005, 957, 1479, 643, 1385, 1148]
		omachi5_num = 711
		# ohmachi is only OHMACHI5
		exits = {	"NAGHOSHI_CLEAN_CENTER_EXIT" => ratioA,
							"OLD_MUNICIPAL_HOUSING_EXIT" => ratioB,
							"KAMAKURA_Jr_HIGH_EXIT" => ratioC }
		CSV.open(filename, "w") do |csv|
			zaimoku_num.each_with_index{ |num, i|
				exits.each{|k,v|
					csv << ["TIMEEVERY","ZAIMOKU#{i+1}","18:00:00","18:00:00",1,1,"#{(num*v).to_i}","#{model}","#{k}"]
				}
			}
			exits.each{ |k,v|
				csv << ["TIMEEVERY","OHMACHI5","18:00:00","18:00:00",1,1,"#{(omachi5_num*v).to_i}","#{model}","#{k}"]
			}
		end
	end

	# ad-hoc 
	def self.generation_kamakura_pattern2(filename, z1_ratioA, z1_ratioB, z1_ratioC,
			z2_ratioA, z2_ratioB, z2_ratioC, z3_ratioA, z3_ratioB, z3_ratioC, z4_ratioA,
			z4_ratioB, z4_ratioC,	z5_ratioA, z5_ratioB, z5_ratioC, z6_ratioA, z6_ratioB,
			z6_ratioC, o5_ratioA, o5_ratioB, o5_ratioC, model="DENSITY")
		# default(7,328 persons)
		#z_num, o_num = [1005, 957, 1479, 643, 1385, 1148], 711
		# 10,000 persons
		#z_num, o_num = [1371, 1306, 2018, 878, 1890, 1567], 970
		# 7,500 persons
		#z_num, o_num = [1029, 979, 1514, 658, 1417, 1175], 728
		# 5,000 persons
		#z_num, o_num = [686, 653, 1009, 439, 945, 783], 485
		# 2,500 persons
		z_num, o_num = [343, 326, 505, 219, 472, 392], 243 
		# each 10 persons
		#z_num, o_num = [10, 10, 10, 10, 10, 10], 10
		CSV.open(filename, "w") do |csv|
			csv << ["TIMEEVERY","ZAIMOKU1","18:00:00","18:00:00",1,1,"#{(z_num[0]*z1_ratioA).to_i}","#{model}","NAGHOSHI_CLEAN_CENTER_EXIT"]
			csv << ["TIMEEVERY","ZAIMOKU1","18:00:00","18:00:00",1,1,"#{(z_num[0]*z1_ratioB).to_i}","#{model}","OLD_MUNICIPAL_HOUSING_EXIT"]
			csv << ["TIMEEVERY","ZAIMOKU1","18:00:00","18:00:00",1,1,"#{(z_num[0]*z1_ratioC).to_i}","#{model}","KAMAKURA_Jr_HIGH_EXIT"]

			csv << ["TIMEEVERY","ZAIMOKU2","18:00:00","18:00:00",1,1,"#{(z_num[1]*z2_ratioA).to_i}","#{model}","NAGHOSHI_CLEAN_CENTER_EXIT"]
			csv << ["TIMEEVERY","ZAIMOKU2","18:00:00","18:00:00",1,1,"#{(z_num[1]*z2_ratioB).to_i}","#{model}","OLD_MUNICIPAL_HOUSING_EXIT"]
			csv << ["TIMEEVERY","ZAIMOKU2","18:00:00","18:00:00",1,1,"#{(z_num[1]*z2_ratioC).to_i}","#{model}","KAMAKURA_Jr_HIGH_EXIT"]

			csv << ["TIMEEVERY","ZAIMOKU3","18:00:00","18:00:00",1,1,"#{(z_num[2]*z3_ratioA).to_i}","#{model}","NAGHOSHI_CLEAN_CENTER_EXIT"]
			csv << ["TIMEEVERY","ZAIMOKU3","18:00:00","18:00:00",1,1,"#{(z_num[2]*z3_ratioB).to_i}","#{model}","OLD_MUNICIPAL_HOUSING_EXIT"]
			csv << ["TIMEEVERY","ZAIMOKU3","18:00:00","18:00:00",1,1,"#{(z_num[2]*z3_ratioC).to_i}","#{model}","KAMAKURA_Jr_HIGH_EXIT"]

			csv << ["TIMEEVERY","ZAIMOKU4","18:00:00","18:00:00",1,1,"#{(z_num[3]*z4_ratioA).to_i}","#{model}","NAGHOSHI_CLEAN_CENTER_EXIT"]
			csv << ["TIMEEVERY","ZAIMOKU4","18:00:00","18:00:00",1,1,"#{(z_num[3]*z4_ratioB).to_i}","#{model}","OLD_MUNICIPAL_HOUSING_EXIT"]
			csv << ["TIMEEVERY","ZAIMOKU4","18:00:00","18:00:00",1,1,"#{(z_num[3]*z4_ratioC).to_i}","#{model}","KAMAKURA_Jr_HIGH_EXIT"]

			csv << ["TIMEEVERY","ZAIMOKU5","18:00:00","18:00:00",1,1,"#{(z_num[4]*z5_ratioA).to_i}","#{model}","NAGHOSHI_CLEAN_CENTER_EXIT"]
			csv << ["TIMEEVERY","ZAIMOKU5","18:00:00","18:00:00",1,1,"#{(z_num[4]*z5_ratioB).to_i}","#{model}","OLD_MUNICIPAL_HOUSING_EXIT"]
			csv << ["TIMEEVERY","ZAIMOKU5","18:00:00","18:00:00",1,1,"#{(z_num[4]*z5_ratioC).to_i}","#{model}","KAMAKURA_Jr_HIGH_EXIT"]

			csv << ["TIMEEVERY","ZAIMOKU6","18:00:00","18:00:00",1,1,"#{(z_num[5]*z6_ratioA).to_i}","#{model}","NAGHOSHI_CLEAN_CENTER_EXIT"]
			csv << ["TIMEEVERY","ZAIMOKU6","18:00:00","18:00:00",1,1,"#{(z_num[5]*z6_ratioB).to_i}","#{model}","OLD_MUNICIPAL_HOUSING_EXIT"]
			csv << ["TIMEEVERY","ZAIMOKU6","18:00:00","18:00:00",1,1,"#{(z_num[5]*z6_ratioC).to_i}","#{model}","KAMAKURA_Jr_HIGH_EXIT"]

			csv << ["TIMEEVERY","OHMACHI5","18:00:00","18:00:00",1,1,"#{(o_num*o5_ratioA).to_i}","#{model}","NAGHOSHI_CLEAN_CENTER_EXIT"]
			csv << ["TIMEEVERY","OHMACHI5","18:00:00","18:00:00",1,1,"#{(o_num*o5_ratioB).to_i}","#{model}","OLD_MUNICIPAL_HOUSING_EXIT"]
			csv << ["TIMEEVERY","OHMACHI5","18:00:00","18:00:00",1,1,"#{(o_num*o5_ratioC).to_i}","#{model}","KAMAKURA_Jr_HIGH_EXIT"]
		end
	end

	# for moji?
	def self.generation_moji_pattern(filename, ratioA, ratioB, ratio, model)
		baseWString = "TIMEEVERY,WEST_STATION_LINKS,18:00:00,18:09:00,60,60,"
    baseEString = "TIMEEVERY,EAST_STATION_LINKS,18:00:00,18:09:00,60,60,"
    CSV.open(filename, "w") do |csv|
			csv << baseWString.split(',')
			csv << baseWString.split(',') 
			csv << baseWString.split(',') + [ratioA*ratio, model, "EAST_STATION_N_NODES","POINT_A","E_POINT_A"]
			csv << baseWString.split(',') + [ratioA*ratio, model, "EAST_STATION_MN_NODES","POINT_A","E_POINT_B"]
			csv << baseWString.split(',') + [ratioA*ratio, model, "EAST_STATION_MS_NODES","POINT_A","E_POINT_C"]
			csv << baseWString.split(',') + [ratioA*ratio, model, "EAST_STATION_S_NODES","POINT_A","E_POINT_D"]

			csv << baseEString.split(',') + [ratioB*ratio, model, "WEST_STATION_N_NODES","POINT_B","W_POINT_A"]
			csv << baseEString.split(',') + [ratioB*ratio, model, "WEST_STATION_MN_NODES","POINT_B","W_POINT_B"]
			csv << baseEString.split(',') + [ratioB*ratio, model, "WEST_STATION_MS_NODES","POINT_B","W_POINT_C"]
			csv << baseEString.split(',') + [ratioB*ratio, model, "WEST_STATION_S_NODES","POINT_B","W_POINT_D"]

			csv << baseWString.split(',') + [(1.0 - ratioA)*ratio, model, "EAST_STATION_N_NODES","POINT_C","E_POINT_A"]
			csv << baseWString.split(',') + [(1.0 - ratioA)*ratio, model, "EAST_STATION_MN_NODES","POINT_C","E_POINT_B"]
			csv << baseWString.split(',') + [(1.0 - ratioA)*ratio, model, "EAST_STATION_MS_NODES","POINT_C","E_POINT_C"]
			csv << baseWString.split(',') + [(1.0 - ratioA)*ratio, model, "EAST_STATION_S_NODES","POINT_C","E_POINT_D"]

			csv << baseEString.split(',') + [((1.0 - ratioB)*ratio).to_i, model, "WEST_STATION_N_NODES","POINT_D","W_POINT_A"]
			csv << baseEString.split(',') + [((1.0 - ratioB)*ratio).to_i, model, "WEST_STATION_MN_NODES","POINT_D","W_POINT_B"]
			csv << baseEString.split(',') + [((1.0 - ratioB)*ratio).to_i, model, "WEST_STATION_MS_NODES","POINT_D","W_POINT_C"]
			csv << baseEString.split(',') + [((1.0 - ratioB)*ratio), model, "WEST_STATION_S_NODES","POINT_D","W_POINT_D"]
		end

    # out.write(baseWString + ((int)(ratioA * 1 * ratio)).toString() + "," + model + ",EAST_STATION_N_NODES,POINT_A,E_POINT_A\n")
    # out.write(baseWString + ((int)(ratioA * 1 * ratio)).toString() + "," + model + ",EAST_STATION_MN_NODES,POINT_A,E_POINT_B\n")
    # out.write(baseWString + ((int)(ratioA * 1 * ratio)).toString() + "," + model + ",EAST_STATION_MS_NODES,POINT_A,E_POINT_C\n")
    # out.write(baseWString + ((int)(ratioA * 1 * ratio)).toString() + "," + model + ",EAST_STATION_S_NODES,POINT_A,E_POINT_D\n")
    # out.write(baseEString + ((int)(ratioB * 1 * ratio)).toString() + "," + model + ",WEST_STATION_N_NODES,POINT_B,W_POINT_A\n")
    # out.write(baseEString + ((int)(ratioB * 1 * ratio)).toString() + "," + model + ",WEST_STATION_MN_NODES,POINT_B,W_POINT_B\n")
    # out.write(baseEString + ((int)(ratioB * 1 * ratio)).toString() + "," + model + ",WEST_STATION_MS_NODES,POINT_B,W_POINT_C\n")
    # out.write(baseEString + ((int)(ratioB * 1 * ratio)).toString() + "," + model + ",WEST_STATION_S_NODES,POINT_B,W_POINT_D\n")
    # out.write(baseWString + ((int)((1.0 - ratioA) * 1 * ratio)).toString() + "," + model + ",EAST_STATION_N_NODES,POINT_C,E_POINT_A\n")
    # out.write(baseWString + ((int)((1.0 - ratioA) * 1 * ratio)).toString() + "," + model + ",EAST_STATION_MN_NODES,POINT_C,E_POINT_B\n")
    # out.write(baseWString + ((int)((1.0 - ratioA) * 1 * ratio)).toString() + "," + model + ",EAST_STATION_MS_NODES,POINT_C,E_POINT_C\n")
    # out.write(baseWString + (((int)(1.0 - ratioA) * 1 * ratio)).toString() + "," + model + ",EAST_STATION_S_NODES,POINT_C,E_POINT_D\n")
    # out.write(baseEString + (((int)(1.0 - ratioB) * 1 * ratio)).toString() + "," + model + ",WEST_STATION_N_NODES,POINT_D,W_POINT_A\n")
    # out.write(baseEString + (((int)(1.0 - ratioB) * 1 * ratio)).toString() + "," + model + ",WEST_STATION_MN_NODES,POINT_D,W_POINT_B\n")
    # out.write(baseEString + (((int)(1.0 - ratioB) * 1 * ratio)).toString() + "," + model + ",WEST_STATION_MS_NODES,POINT_D,W_POINT_C\n")
    # out.write(baseEString + (((int)(1.0 - ratioB) * 1 * ratio)).toString() + "," + model + ",WEST_STATION_S_NODES,POINT_D,W_POINT_D\n")
	end
end



# for debug 
if __FILE__ == $0
	dir="sample/kamakura" #3,4links,4blidges, 
	id = 0
	p "debug: scenario file generation"
	FileGenerator.generate_scenario(dir,"scenario.csv", id)

	p "debug: map file generation"
	FileGenerator.copy_map(dir,"2014_0109_kamakura11-3.xml", id)

	p "debug: generation file generation "
	FileGenerator.generate_gen(dir, "gen.csv", id)

	p "debug: gas(pollution) file generation"
	FileGenerator.copy_pollution(dir,"output_pollution", id)

	p "debug: property file generation"
	FileGenerator.generate_property(dir, "properties.xml", "2014_0109_kamakura11-3",
																	"gen","output_pollution", id, "scenario", 2525)
end
