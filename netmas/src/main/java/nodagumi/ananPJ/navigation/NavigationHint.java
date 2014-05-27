package nodagumi.ananPJ.navigation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ClassNotFoundException;

import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;

public class NavigationHint implements Comparable<NavigationHint>,
       Serializable {
    final public MapNode exit;
    final public MapLink way;
    final public double distance;

    public NavigationHint(final MapNode e, final MapLink w, final double d) {
        exit = e;
        way = w;
        distance = d;
    }

    public int compareTo(NavigationHint e) {
        if (e.distance < distance) return -1;
        else if (e.distance > distance) return 1;
        else return 0;
    }
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
