
pattern = "0001010001000000100000101"

command1 = "./start.sh #{pattern}"
puts command1
system command1

command2 = "./deploy.sh #{pattern}"
puts command2
system command2

