# coding: utf-8

# oacis_exec.rb


# query_array <= 0 0 0 1 1 1 0 1 ...1 0 0 1
query_array = ARGV


# "0001110101000...1001" <= ["0", "0", "0", "1", "1", "1", "0", "1", ...,"1", "0", "0", "1"]
query = ""
query_array.each do |ch|
	query += ch
end

command = "./start.sh #{query}" 
puts command
system(command)

