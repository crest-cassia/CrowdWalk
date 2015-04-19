package nodagumi.ananPJ.navigation;

import java.util.ArrayList;
import java.util.HashMap;

import nodagumi.ananPJ.NetworkMapBase;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.navigation.CalcPath.NodeLinkLen;
import nodagumi.ananPJ.navigation.CalcPath.Nodes;
import nodagumi.ananPJ.navigation.CalcPath.PathChooser;
import nodagumi.ananPJ.navigation.Dijkstra;


public class Flooding extends Dijkstra {

    static public Result calc(Nodes subgoals, PathChooser chooser) {
        Result frontier = new Result();
        Result terminals = new Result();
        for (MapNode subgoal : subgoals) {
            double cost = chooser.initialCost(subgoal);
            frontier.put(subgoal, new NodeLinkLen(null, null, cost));
            terminals.put(subgoal, new NodeLinkLen(null, null, cost));
        }
        while (terminals.size() > 0) {
            for (MapNode terminal : terminals.keySet()) {
                for (MapLink nextLink : terminal.getPathwaysReverse()) {
                    MapNode other_node = nextLink.getOther(terminal);
                    if (frontier.containsKey(other_node))
                        continue;
                    double len = frontier.get(terminal).len + nextLink.length *
                        chooser.evacuationRouteCost(nextLink);
                }
            }
        }
        return frontier;
    }

    
    //private MapNodeTable nodes = null;
    //private MapLinkTable links = null;
    private NetworkMapBase map = null ;

    private ArrayList<String> goalTags = null;
    // routing table
    private ArrayList<FloodingRoutingTable> tables = null;

    public Flooding(NetworkMapBase _map,
            ArrayList<String> _goalTags) {
	map = _map ;
        goalTags = _goalTags;
    }

    // update flooding routing table with updated node list
    public void update(NetworkMapBase _map,
            ArrayList<String> _goalTags) {
        if (_map != null)
	    map = _map;
        if (_goalTags != null)
            goalTags = _goalTags;

        if (tables != null)
            tables.clear();

        // create routing table for all map nodes
	for (MapNode node : map.getNodes())
            tables.add(new FloodingRoutingTable(node));

        for (String goalTag : goalTags) {
            MapNodeTable goalNodes = new MapNodeTable();
	    for (MapNode node : map.getNodes())
                if (node.hasTag(goalTag))
                    goalNodes.add(node);


            for (MapNode node : goalNodes) {
                // nodes which the route has determined
		MapNodeTable determinedNodes = map.getNodes();
                // terminal nodes which receives flooding routing messages
                MapNodeTable terminalNodes = new MapNodeTable();
                // goal node already has the goal
                determinedNodes.add(node);
                // set first terminal nodes
                for (MapLink pathWay : node.getPathways()) {
                    // one of the linked node with node variable (of goalNodes)
                    MapNode otherNode = pathWay.getOther(node);
                    terminalNodes.add(otherNode);
                    determinedNodes.add(otherNode);
                    for (FloodingRoutingTable table : tables) {
                        if (table.getEntryNode() == otherNode)
                            table.addFloodingRouteEntry(new
                                FloodingRouteEntry(otherNode, goalTag,
                                    pathWay, pathWay.length));;
                    }
                }
                while (terminalNodes.size() > 0) {
                    MapNodeTable oldTerminalNodes = terminalNodes;
                    terminalNodes.clear();
                    for (MapNode terminalNode : oldTerminalNodes) {
                        for (MapLink pathWay : terminalNode.getPathways()) {
                            MapNode otherNode = pathWay.getOther(node);
                            if (!determinedNodes.contains(otherNode)) {
                                terminalNodes.add(otherNode);
                                for (FloodingRoutingTable table : tables) {
                                    if (table.getEntryNode() == otherNode) {
                                        table.addFloodingRouteEntry(new
                                            FloodingRouteEntry(otherNode,
                                                goalTag, pathWay,
                                                pathWay.length));;
                                        break;
                                    }
                                }
                                determinedNodes.add(otherNode);
                            }
                        }
                    }
                }
            }
        }
    }

    // returns next hop map link
    public MapLink navigate(String goal, MapNode current) {
        MapLink nextHop = null;
        for (FloodingRoutingTable table : tables) {
            if (table.getEntryNode() == current) {
                nextHop = table.getEntryFromGoal(goal).getNextHopLink();
                break;
            }
        }
        return nextHop;
    }

    class FloodingRouteEntry {

        private MapNode entryNode = null;
        private String goalTag = null;

        private MapLink nextHopLink = null;
        private double cost = 0.0;

        public FloodingRouteEntry(MapNode _entryNode, String _goalTag,
                MapLink _nextHopLink, double _cost) {
            entryNode = _entryNode;
            goalTag = _goalTag;
            nextHopLink = _nextHopLink;
            cost = _cost;
        }
        public MapNode getEntryNode() {
            return entryNode;
        }
        public MapLink getNextHopLink() {
            return nextHopLink;
        }
        public void setNextHopLink(MapLink _nextHopLink) {
            nextHopLink = _nextHopLink;
        }
        public String getGoalTag() {
            return goalTag;
        }
        public double getCost() {
            return cost;
        }
        public void setCost(double _cost) {
            cost = _cost;
        }
    }

    class FloodingRoutingTable {

        private MapNode entryNode = null;

        private ArrayList<FloodingRouteEntry> entries =
            new ArrayList<FloodingRouteEntry>();

        public FloodingRoutingTable(MapNode _entryNode) {
            entryNode = _entryNode;
        }
        // add route entry. if the cost of the entry has smaller cost, function
        // returns true.
        public boolean addFloodingRouteEntry(FloodingRouteEntry _entry) {
            boolean addFlag = true;
            for (FloodingRouteEntry entry : entries) {
                if (_entry.getGoalTag().equals(entry.getGoalTag())) {
                    addFlag = false;
                    if (_entry.getCost() < entry.getCost()) {
                        entry.setNextHopLink(_entry.getNextHopLink());
                        entry.setCost(_entry.getCost());
                    }
                    break;
                }
            }
            if (addFlag)
                entries.add(_entry);
            return addFlag;
        }
        public void removeFloodingRouteEntry(FloodingRouteEntry _entry) {
            entries.remove(_entry);
        }
        public void removeFloodingRouteEntry(String _goalTag) {
            for (FloodingRouteEntry entry : entries)
                if (entry.getGoalTag().equals(_goalTag)) {
                    entries.remove(entry);
                    break;
                }
        }
        public MapNode getEntryNode() {
            return entryNode;
        }
        public FloodingRouteEntry getEntryFromGoal(String _goal) {
            FloodingRouteEntry retval = null;
            for (FloodingRouteEntry entry : entries) {
                if (entry.getGoalTag().equals(_goal)) {
                    retval = entry;
                    break;
                }
            }
            return retval;
        }
    }
}

