# -*- coding: utf-8 -*-

require 'date'
require 'erb'
require './libs/placemark'

# 時刻のフォーマットを定義
class DateTime
	def to_s
		self.strftime '%Y-%m-%dT%H:%M:%SZ'
	end
end



class Parser
	@@timeStarting = DateTime::parse "2014-05-01T00:00:00Z"
	

	# 時間間隔を指定して初期化
	def initialize(args={})
		args = {
			intervals_sec: 1.0 
		}.merge(args)

		@placemarks = {}
		@intervals = Rational args[:intervals_sec],24*60*60
		@currentTimestamp = @@timeStarting

	end


	# エージェントを追加
	def push(args={})
		name      = args[:name]
		latitude  = args[:latitude]
		longitude = args[:longitude]
		altitude  = args[:altitude]

		if !@placemarks.key?(name) then
			# 指定された name の Placemark が存在しない場合, 新規に追加する
			placemark = Placemark.new name
			@placemarks[name] = placemark
		end

		placemark = @placemarks[name]
		
		placemark.push({:when=>@currentTimestamp, :longitude=>longitude, :latitude=>latitude, :altitude=>altitude})
	
	end


	# 1ステップ進行する
	def step 
		@currentTimestamp += @intervals
	end


	# KML として出力
	def printKML(filename)

		baseLatitude  = 0
		baseLongitude = 0
		@placemarks.each_value do |placemark|
			baseLatitude  += placemark.gxTrack[0][:latitude]  / @placemarks.size
			baseLongitude += placemark.gxTrack[0][:longitude] / @placemarks.size
		end

		placemarks = @placemarks

		erb = ERB.new(IO.read(filename))
		puts erb.result(binding)

	end

	attr_reader :placemarks
end

