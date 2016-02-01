// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.navigation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ClassNotFoundException;

import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.Itk.Term;

//======================================================================
/**
 * 最適ルーティングの情報。
 */
public class NavigationHint implements Comparable<NavigationHint> {
    //::::::::::::::::::::::::::::::::::::::::::::::::::
    /** デフォルトの主観モード */
    static public final Term DefaultMentalMode = null ;
    
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /** 主観モード */
    public Term mentalMode ;
    /** 最終目標ゴールタグ */
    public String goalTag ;
    /** 起点ノード */
    public MapNode fromNode ;
    /** 向かう方向のノード */
    public MapNode toNode;
    /** 辿るリンク */
    public MapLink viaLink;
    /** ゴールまでの距離 */
    public double distance;

    //--------------------------------------------------
    /**
     * constructor.
     */
    public NavigationHint(Term _mentalMode, String _goalTag,
                          MapNode _fromNode, MapLink _viaLink, MapNode _toNode,
                          double _distance) {
        set(_mentalMode, _goalTag,
            _fromNode, _viaLink, _toNode, _distance) ;
    }
    
    //--------------------------------------------------
    /**
     * 値設定
     */
    public NavigationHint set(Term _mentalMode, String _goalTag,
                              MapNode _fromNode, MapLink _viaLink,
                              MapNode _toNode, double _distance) {
        mentalMode = _mentalMode ;
        goalTag = _goalTag ;
        fromNode = _fromNode ;
        toNode = _toNode ;
        viaLink = _viaLink ;
        distance = _distance;

        return this ;
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
