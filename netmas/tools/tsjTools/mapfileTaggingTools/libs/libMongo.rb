require 'mongo'

# mongo からシミュレーションデータを取得
class Simulation
	
	# 初期化
	def initialize()
		@connection = Mongo::Connection.new
		@db = @connection.db('test')
		@collection = @db.collection('simulations')

	end

	def insert(obj)
		id = @collection.insert(obj)
	end

	def remove(query)
		@collection.remove(query)
	end

	def find(query)
		@collection.find(query)
	end


	# query にもっとも近い(distance的な意味で) neighbor をとってくるよ
	def calcNearestNeighbor(query, &distance)
		
		distanceArray = []

		@collection.find.each do |row|
			dist = distance.call(query, row)
			
			distanceArray.push({ 'distance'=>dist, 'id'=>row['id'] })
			#puts row['id']
		end

		distanceArray = distanceArray.sort_by{|o| o['distance'] }
		
		id = distanceArray.first['id']

		neighbor = @collection.find_one({'id'=>id})

		return neighbor
	end
end



=begin
# Usage::
simulation = Simulation.new

query = { 'item1'=> 0, 'item2' => 2, 'item3' => 3, 'item4' => 4}

neighbor = simulation.calcNearestNeighbor(query) do |a, b|
	dist =  (a['item1']-b['item1'])*(a['item1']-b['item1'])
	dist += (a['item2']-b['item2'])*(a['item2']-b['item2'])
	dist += (a['item3']-b['item3'])*(a['item3']-b['item3'])
	dist += (a['item4']-b['item4'])*(a['item4']-b['item4'])
		
	dist
end

puts neighbor['id']
=end

