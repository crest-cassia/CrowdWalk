
require 'mongo'

# mongo からシミュレーションデータを取得
class Simulation
	
	# 初期化
	def initialize()
		@connection = Mongo::Connection.new
		@db = @connection.db('test')
		@collection = @db.collection('simulations')

	end

	def insert(doc)
		@collection.remove({"query"=>doc["query"]})
		@collection.insert(doc)
	end

	def removeAll()
		@collection.remove({})
	end

end


query = ARGV[0]

if ARGV.length <= 0 then
	puts "Error"
	exit
end

dirname = "kanazawa-"+query

sim = Simulation.new

doc = {"query"=>query, "id"=>dirname}
sim.insert(doc)

puts doc


