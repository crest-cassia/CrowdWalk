# encoding: utf-8


require './libs/libMongo'

$simulation = Simulation.new


def execErrorMode errorMsg
	puts errorMsg

	execFindMode 
	exit
	
end


def execFindMode 
	puts "Find all data."
	$simulation.find({}).each do |item|
		p item
	end
end


def execDropMode 
	puts "Drop all data."
	$simulation.remove({})
	execFindMode
end


def execInsertMode query,id
	puts "Insert #{id} by query #{query}"

	$simulation.insert({'query'=>query, 'id'=>id})
	
	execFindMode
end




if ARGV.length == 0 then
	execErrorMode 'Few argument error.' 

else 
	if ARGV[0] == 'drop' then
		execDropMode

	elsif ARGV[0] == 'insert' then

		if ARGV.length <= 1 then
			execErrorMode 'Few argument error.'
		end

		execInsertMode ARGV[1],ARGV[2]

	elsif ARGV[0] == 'find' then	
		execFindMode
	else 
		execErrorMode 'Invalid argument error.'
	end
end


