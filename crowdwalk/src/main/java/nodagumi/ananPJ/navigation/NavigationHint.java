// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.navigation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ClassNotFoundException;

import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;

//======================================================================
/**
 * 最適ルーティングの情報。
 */
public class NavigationHint implements Comparable<NavigationHint> {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /** 最終目標ゴールタグ */
    /** 起点ノード */
    /** 向かう方向のノード */
    public MapNode exit;
    /** 辿るリンク */
    public MapLink way;
    /** ゴールまでの距離 */
    public double distance;

    //--------------------------------------------------
    /**
     * constructor.
     */
    public NavigationHint(MapNode e, MapLink w, double d) {
        exit = e;
        way = w;
        distance = d;
    }

    //--------------------------------------------------
    /**
     * compare.
     */
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
