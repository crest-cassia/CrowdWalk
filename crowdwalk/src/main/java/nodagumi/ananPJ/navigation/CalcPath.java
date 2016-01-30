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
    public interface PathChooser {
        abstract public double evacuationPathCost(MapLink link);
        abstract public double initialCost(MapNode node);
    }

}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
