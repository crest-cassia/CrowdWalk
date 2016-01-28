// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.navigation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;

public class CalcPath {
    /* calculate evacuation paths
     */
    public static class Nodes extends MapNodeTable {};
    public static class NodeByHeight extends HashMap<Double, Nodes> {};

    public static class NodeLinkLen {
        public MapNode node;
        public MapLink link;
        public double len;

        NodeLinkLen(MapNode _pred, MapLink _path, double _len) {
            node = _pred;
            link = _path;
            len = _len;
        }
    }

    public interface PathChooser {
        abstract public boolean isExit(MapLink link); 
        abstract public double evacuationRouteCost(MapLink link);
        abstract public double initialCost(MapNode node);
    }

    public interface PathChooserFactory {
        abstract public PathChooser generate(double height);
        abstract public String hintName();
    }
    
    
    private static void connectFloors(MapNodeTable subgoals,
            PathChooserFactory factory,
            Comparator<MapNode> comp) {
        Collections.sort(subgoals, comp);

        for (MapNode node : subgoals) {
            boolean added = false;
            PathChooser chooser = factory.generate(node.getHeight());
            for (MapLink link : node.getUsableLinkTable()) {
                if (!chooser.isExit(link)) continue;

                MapNode other = link.getOther(node);
                System.err.println(other.getAbsoluteCoordinates());
                NavigationHint hint = other.getHint(factory.hintName());
                if (hint == null) continue;/* might be higher/lower than the exits */
                final double len =
                    hint.distance
                    + link.getLength();
                NavigationHint myhint = new NavigationHint(hint.exit,
                        link, len);
                node.addNavigationHint(factory.hintName(), myhint);
                System.out.println("CalcPath call " +
                        "addNavigationHint node:" + node.ID + ", " + 
                        factory.hintName());
                System.err.println(node.getHeight() + "\t" +    len);
                added = true;
                break;
            }
            if (!added) {
                node.addTag(" NO_EXIT_ADDED");
            }
        }
    }
}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
