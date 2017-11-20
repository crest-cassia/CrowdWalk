// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.misc;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;


public class Hover {
    private Point2D currentPos;
    private Line2D line;
    private MapNode dummyHoverNode;
    private MapLink dummyHoverLink;
    public MapNode orig_node;
    public MapLink orig_link;
    public MapNode from,to;
    public double width,length;
    public static final BasicStroke narrow = new BasicStroke(1.0f);
    //constructor
    public Hover(MapNode _orig_node,
            Point2D _pos){
        orig_node = _orig_node;
        currentPos = _pos;
    }

    public Hover(MapNode _from,MapNode _to,double _width,double _length){
        line = new Line2D.Double(_from.getPosition(),_to.getPosition());
        from = _from;
        to   = _to;
        width =_width;
        length = _length;
    }
    // getter and mutator
    public void setPos(Point2D _pos){
        currentPos = _pos;
    }
    
    /*public void setDummyHoverNode(MapNode _node){
        dummyHoverNode = _node;
        pos = dummyHoverNode.pos;
    }*/
    
    public MapNode getDummyHoverNode(){
        return dummyHoverNode;
    }
    public void setDummyHoverLink(MapLink _link){
        dummyHoverLink = _link;
    }
    public MapLink getDummyHoverLink(){
        return dummyHoverLink;
    }
    
    //wrapper method
    public Point2D getCurrentPos(){ 
        return currentPos;
    }
    public double getX() {
            return currentPos.getX();
    }
    public double getY() {
            return currentPos.getY();   
    }
    public Line2D getLine2D() {
        return line;
    }
    
    public GeneralPath getRect(Graphics2D g, boolean showScaling) {
        double scale = 1.0;
        if (!showScaling)
            scale = g.getTransform().getScaleX();
        double x1 = from.getX();
        double y1 = from.getY();
        double x2 = to.getX();
        double y2 = to.getY();
        double fwidth = 4.0 / scale;

        double dx = (x2 - x1);
        double dy = (y2 - y1);
        double a = Math.sqrt(dx*dx + dy*dy);

        double edx = fwidth * dx / a / 2;
        double edy = fwidth * dy / a / 2;

        GeneralPath p = new GeneralPath();
        p.moveTo(x1 - edy, y1 + edx);
        p.lineTo(x1 + edy, y1 - edx);
        p.lineTo(x2 + edy, y2 - edx);
        p.lineTo(x2 - edy, y2 + edx);
        p.lineTo(x1 - edy, y1 + edx);
        p.closePath();

        return p;
    }

    public void setSelected(boolean select){
        
        //if (dummyHoverNode  != null)dummyHoverNode.selected=select;
        //else dummyHoverLink.selected =select;
    }
    
    public String getTagString() {
        return dummyHoverNode.getTagString();
    }
    public MapLinkTable getUsableLinkTable() {
        return dummyHoverNode.getUsableLinkTable();
    }
}
// ;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
