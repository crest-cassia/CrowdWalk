require 'mongo'
require 'bson'
require 'pp'
require 'csv'

con = Mongo::Connection.new
db = con.db("oacis_development")

#'52e6491f6f7236a46f000001'#'52e1f4d16f7236e382000001' #<-narrow
# '52ea09576f72367e3e000001' #'52e6491f6f7236a46f000001'#'52e1f4d16f7236e382000001' #<-narrow
# 7500 narrow 52f47d596f7236d984000001
# 5000 narrow 52f8378b6f72363f0d000001
# 2500 narrow 52f9d47d6f723679f7000001
# 10K wide 52f0de4f6f72363db0000001
# 7500 wide 52fb4b7e6f7236a806000001
# 5000 wide 5301d7b06f7236564f000001
# 2500 wide 530321526f7236668c000001
sim_id = '530321526f7236668c000001'
save = "crowdwalk_wide_2500persons.csv"



coll_parameter_sets = db["parameter_sets"]
coll_runs = db["runs"]

# arr_pset = []
pset_h = {}
coll_parameter_sets.find.each{|doc|
	if doc["simulator_id"] == BSON::ObjectId(sim_id)
		h = {}
		# h["_id"] = doc["_id"]
		h["v"] = doc["v"]
		# arr_pset.push(h)
		pset_h[doc["_id"]] = h
	end
}


# arr_run = []
coll_runs.find.each{|doc|
	if doc["simulator_id"] == BSON::ObjectId(sim_id)
		if pset_h.key?(doc["parameter_set_id"])
			# res = {"tick" => 3200}
			if doc.key?("result")
				pset_h[doc["parameter_set_id"]]["result"] = doc["result"]
			else
				pset_h[doc["parameter_set_id"]]["result"] = {"tick" => nil}
			end

			pset_h[doc["parameter_set_id"]]["real_time"] = doc["real_time"]
		end
	end
}


CSV.open(save, "w") do |csv|
	csv << ["z1_a","z1_b","z2_a","z2_b","z3_a","z3_b",
					"z4_a","z4_b","z5_a","z5_b","z6_a","z_b", "o5_a","o5_b","tick","real time"]
	pset_h.each{|k,v|
		row = []
		v["v"].each_value{|val|
			row.push(val)
		}
		row.push(v["result"]["tick"])
		row.push(v["real_time"])
		csv << row
	}	
end
