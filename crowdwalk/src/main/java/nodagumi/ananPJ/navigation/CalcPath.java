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
    
}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
