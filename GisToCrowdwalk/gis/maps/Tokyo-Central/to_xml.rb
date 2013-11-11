#!/usr/bin/env ruby

ShapeFile = ARGV[0]
LinkFile = ARGV[1]

$ids = Hash.new
$node_id_to_obnode_id = Hash.new

def get_new_id
  id = (rand() * 1000000).to_i
  while $ids[id] != nil
    id = (rand() * 1000000).to_i
  end
  id
end


node_id = 0
nodes = Array.new
min_x = 2000
max_x = 0
min_y = 2000
max_y = 0

open(ShapeFile).each do |line|
  case line
  when /^Shape\:(\d+)/
    node_id = $1.to_i
  when /     \(   (\d+\.\d+),    (\d+\.\d+), 0, 0\)/
    x = $1.to_f
    y = $2.to_f
    min_x = x if x < min_x
    min_y = y if y < min_y
    max_x = x if x > max_x
    max_y = y if y > max_y
    id = get_new_id
    $ids[id] = node_id
    $node_id_to_obnode_id[node_id] = id
    nodes.push('    <Node height="0" id="%d" x="%f" y="%f"><tag>%d</tag></Node>
' % [id, x, y, node_id])
  end
end

print '<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<Group id="0" pNorthWestX="%f" pNorthWestY="%f" pSouthEastX="%f" pSouthEastY="%f" pTheta="0.0" r="0.0" tx="0.0" ty="0.0" sx="0.0" sy="0.0" defaultHeight="0.0" minHeight="0.0" maxHeight="0.0" scale="1.0">
<Group id="0" pNorthWestX="%f" pNorthWestY="%f" pSouthEastX="%f" pSouthEastY="%f" pTheta="0.0" r="0.0" tx="0.0" ty="0.0" sx="0.0" sy="0.0" defaultHeight="0.0" minHeight="0.0" maxHeight="0.0" scale="1.0">
' % [min_x, min_y, max_x, max_y, min_x, min_y, max_x, max_y]

nodes.each do |node|
  print node
end

open(LinkFile).each do |line|
  #MESHCODE ND1   ND2   RD_TPCD    LINE_NUM CAR_RD_CD TOLL_RD_CD KANRI_CD   LK_LENGTH WIDTH_TPCD LINE_CD    TRAF_12H  TRAVEL_SPD REG_SPD_CD 
  #533946   00001 10036 9                 0 0         0          5                  8 2          (NULL)                 0           0 (NULL)
  items = line.chomp.split
  next unless /\d+/.match(items[0])

  from = items[1].to_i
  to = items[2].to_i
  line_num = items[4]
  length = items[8].to_f
  width = items[8].to_f

  id = get_new_id
  $ids[id] = "#{from}_#{to}"

  from_node = $node_id_to_obnode_id[from]
  to_node = $node_id_to_obnode_id[to]

  if from_node == nil || to_node == nil
    next
  end

  print '    <Link id="%d" from="%d" to="%d" length="%f"  width="%f"><tag>%d</tag></Link>
' % [id, from_node, to_node,
  length, width, line_num]
end

print '  </Group>
</Group>
'
