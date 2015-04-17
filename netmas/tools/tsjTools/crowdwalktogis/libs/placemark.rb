

# エージェントのデータフォーマット
class Placemark
	def initialize name
		@name    = name
		@gxTrack = Array.new
	end
	
	def push(args={})
		@gxTrack.push args
	end
	
	attr_accessor :name
	attr_reader   :gxTrack
end


