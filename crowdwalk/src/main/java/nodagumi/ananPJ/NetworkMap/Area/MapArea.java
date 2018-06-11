// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.NetworkMap.Area;

import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import math.geom3d.Point3D;

import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.Simulator.PollutionHandler.*;

//======================================================================
/**
 * 地図上のエリア。
 */
public abstract class MapArea extends OBNode {

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 現在の Pollution 情報
     */
    public PollutionLevelInfo pollutionLevel = null ;

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public MapArea(String _id) {
        super(_id);
    }

    //------------------------------------------------------------
    /**
     * Pollution Level を格納。
     */
    public void pollutionIsUpdated() {
        if(pollutionLevel.isChanged(true)) {
            map.getNotifier().pollutionLevelChanged(this);
        }
    }

    //------------------------------------------------------------
    /**
     * Pollution Level を取得。
     */
    public PollutionLevelInfo getPollutionLevel() {
        return pollutionLevel ;
    }

    //------------------------------------------------------------
    /**
     * Pollution されているかどうか。
     */
    public boolean isPolluted() {
        return pollutionLevel.isPolluted() ;
    }

    //------------------------------------------------------------
    /**
     * 包含判定。
     */
    public abstract boolean contains(Point2D point);

    //------------------------------------------------------------
    /**
     * 包含判定。
     */
    public abstract boolean contains(Point3D point);

    //------------------------------------------------------------
    /**
     * 交差判定。
     */
    public abstract boolean intersectsLine(Line2D line);

    //------------------------------------------------------------
    /**
     * 形状を取得。
     */
    public abstract Shape getShape();

    //------------------------------------------------------------
    /**
     * 高さの範囲の最小値を取得。
     */
    public abstract double getMinHeight();

    //------------------------------------------------------------
    /**
     * 高さの範囲の最大値を取得。
     */
    public abstract double getMaxHeight();

    //------------------------------------------------------------
    /**
     * 頂点リスト。
     */
    public abstract ArrayList<Point2D> getAllVertices();	// tkokada

    //------------------------------------------------------------
    /**
     * 方向。
     */
    public abstract double getAngle();	// tkokada

    //------------------------------------------------------------
    /**
     * to/from DOM codes
     */
    static public String getNodeTypeString() {
        return "Area";
    }

    //------------------------------------------------------------
    /**
     * タイプ。
     */
    @Override
    public NType getNodeType() {
        return NType.AREA;
    }

    //------------------------------------------------------------
    /**
     * 末端判定。
     */
    @Override
    public boolean isLeaf() {
        return true;
    }

}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
