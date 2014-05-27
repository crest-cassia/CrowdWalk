package nodagumi.ananPJ.Agents;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Random;

import nodagumi.ananPJ.NetworkParts.Link.MapLink;


public class Staff extends RunningAroundPerson implements Serializable {
    private static final long serialVersionUID = -7925686612702722379L;
    public static String magic = "Staff";
    protected double emptyspeed = 1.0, confidence = 1.0;

    //TODO obsolete code
    public Staff(Random _random) {
        super(0, _random);
    }

    /*public Staff(MapNode initialNode, double _emptySpeed,
            double _confidence, double _hitPoint, double _generatedTime, NavigationMode _mode) {
        super(initialNode, _emptySpeed, _confidence, _hitPoint, _generatedTime, _mode);
    }*/
    public Staff(int _id,
            double _emptySpeed,
            double _confidence,
            double _maxAllowedDamage,
            double _generatedTime,
            Random _random) {
        super(_id, _emptySpeed, _confidence, _maxAllowedDamage,
                _generatedTime, _random);
    }

    public static String getTypeName() {
        return "Staff";
    }

    @Override
    public void draw(Graphics2D g,
            boolean experiment) {
        if (current_link == null) return;

        Point2D p = getPos();
        if (selected) {
            g.setColor(Color.RED);
            g.fillOval((int)(p.getX()- 2), (int)(p.getY() - 2), 6, 6);
            g.setColor(Color.BLACK);
            g.fillOval((int)(p.getX()- 3), (int)(p.getY() - 3), 8, 8);
        } else {
            g.setColor(Color.BLUE);
            g.fillOval((int)(p.getX()- 2), (int)(p.getY() - 2), 6, 6);
            g.setColor(Color.BLACK);
            g.fillOval((int)(p.getX()- 3), (int)(p.getY() - 3), 8, 8);
        }
    }
    // tkokada
    public MapLink navigate() {
        MapLink navigatedLink = null;
        return navigatedLink;
    }
}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
