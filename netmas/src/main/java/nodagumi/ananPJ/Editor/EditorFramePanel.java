package nodagumi.ananPJ.Editor;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import nodagumi.ananPJ.NetworkMapEditor.EditorMode;
import nodagumi.ananPJ.Agents.EvacuationAgent;
import nodagumi.ananPJ.NetworkParts.MapPartGroup;
import nodagumi.ananPJ.NetworkParts.OBNode;
import nodagumi.ananPJ.NetworkParts.OBNodeSymbolicLink;
import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.NetworkParts.Pollution.PollutedArea;
import nodagumi.ananPJ.misc.Hover;
import nodagumi.ananPJ.navigation.NavigationHint;

/* a panel specifically designed to draw objects on a 
 * specific editor frame. note that the frame is further related to
 * a specific group.
 */
public class EditorFramePanel extends JPanel implements Serializable {
    private static final long serialVersionUID = 5696254371521431653L;

    /* -- Parameters
     */
    /* parameters related to what is edited */
    private EditorFrame frame = null;
    private MapPartGroup ob_node = null;

    /* parameters related to editing */
    private Hover hoverNode = null;
    private Hover hoverLink = null;
    private EvacuationAgent hoverAgent = null;
    private PollutedArea hoverArea = null;
    private Rectangle2D selectedArea = null;
    private Line2D scaleLine = null;
    public boolean setScaleMode = false;
    private Line2D tempLine = null;

    /* parameters related strictly to drawing */
    private double tx = 0.0, ty = 0.0;
    private double scale = 1.0;
    
    private Image backgroundImage = null;
    private MapPartGroup backgroundGroup = null;    // このグループのマップを背景に表示する(表示のみ)
    private double imageStrength = 1.0;
    
    private boolean showNodes = true;
    private boolean showNodeLabels = false;
    private boolean showLinks = true;
    private boolean showLinkLabels = false;
    private boolean showAgents = true;
    // tkokada, modified initial value
    //private boolean showGroups = true;
    private boolean showGroups = false;
    private boolean showSubGroups = false;
    private boolean showPollution = true;
    // show objects with scaling mode or not. scaling mode zoomes up or down 
    // objects as wheel control.
    private boolean showScaling = false;

    public boolean updateStatusEnabled = false;
    public Point point_on_panel = new Point(0, 0);

    Color linkColor = Color.YELLOW;

    /* constructor
     */
    public EditorFramePanel (EditorFrame _frame,
            MapPartGroup _ob_node) {
        super ();
        frame = _frame;
        ob_node = _ob_node;
    }

    public void zoom(int z) {
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;

        Point2D cp = revCalcPos(cx, cy);
        double scaleOld = scale;

        double r;
        if (z > 0) {
            r = 0.8;
        } else {
            r = 1.25;
        }

        z = Math.abs(z);
        for (int i = 0; i < z; i++) {
            scale *= r;
        }
        
        if (scale > 100.0) scale = 100.0;
        if (scale < 0.01) scale = 0.01;

        //TODO: needs calculation for rotation
        tx += cp.getX() * (scaleOld - scale); 
        ty += cp.getY() * (scaleOld - scale); 
    }
    
    public double getDrawingScale() {
        return scale;
    }
    
    public void scroll(int dx, int dy) {
        tx += dx;
        ty += dy;
    }

    public void localScroll(int dx, int dy) {
        ob_node.tx += dx;
        ob_node.ty += dy;
    }
    
    public void localZoomX(int z) {
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        Point2D cp = revCalcPos(cx, cy);
        double scaleOldX = ob_node.sx;
        double scaleOldY = ob_node.sy;

        if (z > 0) {
            ob_node.sx -= 0.01;
        } else {
            ob_node.sx += 0.01;
        }
        ob_node.tx += cp.getX() * (scaleOldX - ob_node.sx); 
        ob_node.ty += cp.getY() * (scaleOldY - ob_node.sy); 
    }
    
    public void localZoomY(int z) {
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        Point2D cp = revCalcPos(cx, cy);
        double scaleOldX = ob_node.sx;
        double scaleOldY = ob_node.sy;

        if (z > 0) {
            ob_node.sy -= 0.01;
        } else {
            ob_node.sy += 0.01;
        }

        ob_node.tx -= cp.getX() * (scaleOldX - ob_node.sx); 
        ob_node.ty -= cp.getY() * (scaleOldY - ob_node.sy); 
    }
    
    public void localRotate(int z) {
        ob_node.r += (z * Math.PI) / 64;
        repaint();
    }
    
    public void centering() {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
        for (MapNode node : frame.getChildNodes()) {
            if(node.isBetweenHeight(
                    frame.getMinHeight(),
                    frame.getMaxHeight())) {
                minX = Math.min(minX, node.getX());
                maxX = Math.max(maxX, node.getX());
                minY = Math.min(minY, node.getY());
                maxY = Math.max(maxY, node.getY());
            }
        }
        final double width = maxX - minX;
        final double height = maxY - minY;
        final double scaleX = (getWidth() - 20) / width;
        final double scaleY = (getHeight() - 60) / height;
        
        scale = Math.min(scaleX, scaleY);
        tx = -(minX + maxX) / (2) * scale + (getWidth() - 20) / 2;
        ty = -(minY + maxY) / (2) * scale + (getHeight() - 40) / 2;
    }

    /* theoretical value to display */
    public Point2D calcPos(Point2D p) {
        /* translate local position  < reference parent position */ 
        double xx = p.getX() * scale + tx;
        double yy = p.getY() * scale + ty;
        return new Point2D.Double(xx, yy);
    }
    
    /* display to theoretical value */
    /* this method is called by zoom relate function */  
    public Point2D revCalcPos(int x, int y) {
        point_on_panel = SwingUtilities.convertPoint(null, x, y, this);
        
        AffineTransform trans = new AffineTransform();
        trans.translate(tx, ty);
        trans.scale(scale, scale);

        try {
            trans.invert();
        } catch (NoninvertibleTransformException e) {
            e.printStackTrace();
        }
        return trans.transform(new Point2D.Double(point_on_panel.getX(), point_on_panel.getY()), null);
    }

    @Override
    public void paintComponent (Graphics g0) {
        super.paintComponent(g0);
        
        Graphics2D g = (Graphics2D)g0;

        if (setScaleMode) {
            //g.drawString("Scale Mode", 0, 20);
            frame.setStatus("SET_SCALE");
        } else {
            if (updateStatusEnabled) {
                frame.setStatus();
            }
        }

        g.translate(tx, ty);
        g.scale(scale, scale);

        AffineTransform t = g.getTransform();

        if (backgroundImage != null) {
            g.translate(ob_node.tx, ob_node.ty);
            g.scale(ob_node.sx, ob_node.sy);
            g.rotate(ob_node.r);

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                    (float)imageStrength));
            g.drawImage(backgroundImage, 0, 0,
                    (int)(backgroundImage.getWidth(null)),
                    (int)(backgroundImage.getHeight(null)),
                    null);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                    1.0f));
        }
        g.setTransform(t);

        /* show frame */
        if (frame.getRegion() != null){
            g.setStroke(MapLink.narrow);
            g.setColor(Color.ORANGE);
            //g.draw(frame.getRegion());
            g.draw(new Rectangle2D.Double(
                    frame.getRegion().getX(),
                    frame.getRegion().getY(),
                    frame.getBounds().getWidth()-40,
                    frame.getBounds().getHeight()-190));
        }
        /* background group */
        if (backgroundGroup != null) {
            if (showLinks) {
                for (final MapLink link : backgroundGroup.getChildLinks()) {
                    g.setColor(linkColor);
                    link.draw(g, false, showLinkLabels, false, showScaling);
                }
            }
            if (showNodes) {
                g.setStroke(new BasicStroke(1.0f));
                if (backgroundGroup.getChildNodes() != null) {
                    for (MapNode node : backgroundGroup.getChildNodes()) {
                        node.draw(g, false, false, false, Color.LIGHT_GRAY);
                    }
                }
            }
        }
        
        /* actual objects */
        if (showPollution) {
            for (final PollutedArea area : frame.getChildPollutedAreas()) {
                // tkokada
                // apply a rotation rectangle, here.
                double angle = area.getAngle();
                if (!Double.isNaN(angle)) {
                    AffineTransform af = g.getTransform();
                    af.rotate(angle, ((Rectangle2D) area.getShape()).getCenterX(), ((Rectangle2D) area.getShape()).getCenterY());
                    // draw center point of pollution area
                    /*
                    g.setColor(Color.GREEN);
                    g.fill(new Rectangle2D.Double(
                            ((Rectangle2D) area.getShape()).getCenterX() - 6 /g.getTransform().getScaleX(),
                            ((Rectangle2D) area.getShape()).getCenterY() - 6 / g.getTransform().getScaleX(),
                            6 * 2 / g.getTransform().getScaleX(),
                            6 * 2 / g.getTransform().getScaleX()));
                    */
                    //System.out.println("area cx:" + ((Rectangle2D) area.getShape()).getCenterX() + "cy:" + ((Rectangle2D) area.getShape()).getCenterY());
                    g.setTransform(af);
                    //System.out.println("area cx:" + ((Rectangle2D) area.getShape()).getCenterX() + "cy:" + ((Rectangle2D) area.getShape()).getCenterY());
                    //System.out.println("tx:" + tx + ", ty:" + ty + ", obnode tx:" + ob_node.tx + ", ty:" + ob_node.ty + ", sx:" + ob_node.sx + ", sy:" + ob_node.ty
                    //      + ", r:" + ob_node.r + ", scaleX:" + g.getTransform().getScaleX());
                    area.draw(g,  false);
                    g.setTransform(t);
                } else {
                    area.draw(g, false);
                }
            }
        }
        if (showLinks) {
            for (final MapLink link : frame.getChildLinks()) {
                g.setColor(linkColor);
                link.draw(g, false, showLinkLabels, false, showScaling);
            }
        }
        
        if (showNodes) {
            g.setStroke(new BasicStroke(1.0f));
            if (frame.getChildNodes() != null) {
                for (MapNode node : frame.getChildNodes()) {
                    node.draw(g, false, showNodeLabels, false);
                }
            }
        }
        
        if (showAgents) {
            for (EvacuationAgent agent : frame.getChildAgents()) {
                agent.draw(g, false);
            }
        }
        
        if (showGroups) {
            for (MapPartGroup group : frame.getChildGroups()) {
                group.draw(g, false, showSubGroups);
            }
        }
        
        /* symbolic links */
        /* first try links */
        if (showLinks) {
            for (OBNodeSymbolicLink symlink : frame.getSymbolicLinks()) {
                OBNode orig = symlink.getOriginal();
                if (orig == null) {
                    System.err.println("Dangling symlink? " + symlink.ID);
                }
                OBNode.NType ntype = orig.getNodeType();
            
                if (ntype == OBNode.NType.LINK) {
                    MapLink link = (MapLink)orig;
                    link.draw(g, false, showLinkLabels, true, showScaling);
                }
            }
        }
        /* then nodes links */
        if (showNodes) {
            for (OBNodeSymbolicLink symlink : frame.getSymbolicLinks()) {
                OBNode orig = symlink.getOriginal();
                OBNode.NType ntype = orig.getNodeType();
            
                if (ntype == OBNode.NType.NODE) {
                    MapNode node = (MapNode)orig;
                    node.draw(g, false, showNodeLabels, true);
                }
            }
        }

        /* temporary objects */
        if (hoverNode != null) {
            if (hoverNode.orig_node != null) {
                hoverNode.orig_node.draw(g, false, true, true);
            }
            g.setColor(Color.MAGENTA);
            final double scale = g.getTransform().getScaleX();
            Rectangle2D rect = new Rectangle2D.Double(hoverNode.getX() - 6 / scale,
                    hoverNode.getY() - 6 / scale,
                    12 / scale,
                    12 / scale);
            g.fill(rect);
            g.setColor(Color.CYAN);
            rect = new Rectangle2D.Double(hoverNode.getX() - 5 / scale,
                    hoverNode.getY() - 5 / scale,
                    10 / scale,
                    10 / scale);
            g.fill(rect);
            if (frame.getChildNodes().contains(hoverNode)) {
                showHoverNodeInfo(g);
            }
        }
        
        if (hoverLink != null) {
            //g.setStroke(MapLink.narrow);
            g.setColor(Color.BLUE);
            //g.draw(hoverLink.getRect(g));
            g.fill(hoverLink.getRect(g, showScaling));
            if (hoverLink.orig_link != null) {
                hoverLink.orig_link.draw(g, false, true, false, showScaling);
            }
        }
        
        if (hoverAgent != null) {
            Point2D p = hoverAgent.getPos();
            g.setColor(Color.WHITE);
            g.fillOval((int)(p.getX()- 5), (int)(p.getY() - 5), 12, 12);
            g.setColor(Color.BLUE);
            g.fillOval((int)(p.getX()- 4), (int)(p.getY() - 4), 10, 10);
        }

        if (hoverArea != null) {
            g.setColor(Color.BLUE);
            // tkokada
            // apply the rotated rectangles to show.
            boolean drawSelectedArea = false;
            for (PollutedArea area : frame.getChildPollutedAreas()) {
                if (((Rectangle2D) area.getShape()).contains((Rectangle2D) hoverArea.getShape())) {
                    double angle = area.getAngle();
                    if (!Double.isNaN(angle)) {
                        AffineTransform af = g.getTransform();
                        af.rotate(angle, ((Rectangle2D) area.getShape()).getCenterX(), ((Rectangle2D) area.getShape()).getCenterY());
                        g.setTransform(af);
                        g.draw(hoverArea.getShape());
                        g.setTransform(t); 
                        drawSelectedArea = true;
                        break;
                    }
                }
            }
            if (!drawSelectedArea) {
                g.draw(hoverArea.getShape());
            }
        }
        // draw selected Pollution Area by using EDIT_POLLUTION multi selection
        for (PollutedArea area: frame.getChildPollutedAreas()) {
            if (area.selected) {
                double angle = area.getAngle();
                if (!Double.isNaN(angle)) {
                    AffineTransform af = g.getTransform();
                    af.rotate(angle, ((Rectangle2D) area.getShape()).getCenterX(), ((Rectangle2D) area.getShape()).getCenterY());
                    g.setTransform(af);
                    g.setColor(Color.BLUE);
                    g.draw(area.getShape());
                    g.setTransform(t); 
                }
            }
        }

        if (selectedArea != null) {
            g.setColor(Color.BLUE);
            g.draw(selectedArea);
            
        }
        
        if (scaleLine != null) {
            g.setStroke(MapLink.broad);
            g.setColor(Color.WHITE);
            g.draw(scaleLine);
            g.setStroke(MapLink.narrow);
            g.setColor(Color.BLACK);
            g.draw(scaleLine);
        }
        
        if (tempLine != null) {
            g.setStroke(MapLink.broad);
            g.setColor(Color.YELLOW);
            g.draw(tempLine);
            g.setStroke(MapLink.narrow);
            g.setColor(Color.BLACK);
            g.draw(tempLine);
        }       
    }

    private void showHoverNodeInfo(Graphics2D g) {
        g.setColor(Color.MAGENTA);
        g.setFont(new Font("Roman", Font.ITALIC, (int) (24 / scale)));
        g.drawString(hoverNode.getTagString(),
                (int)hoverNode.getX(), (int)hoverNode.getY());

        ArrayList<MapLink> paths = hoverNode.getPathways();
        for (MapLink path : paths) {
            MapNode otherNode = path.getOther(hoverNode.getDummyHoverNode());
            HashMap<String, NavigationHint> hints = otherNode.getHints();
            float dy = 0;
            for (String key : hints.keySet()) {
                dy += 10 / scale;
                NavigationHint hint = hints.get(key);
                g.drawString(key + " " + hint.distance,
                        (float)otherNode.getAbsoluteX(),
                        (float)otherNode.getAbsoluteY() + dy);
                
            }
        }
    }

    /* -- Methods to set how drawn 
     */
    public void addBackground(Image img) {
        assert (img != null);
        backgroundImage = img;
    }
    
    public void setImageStrength(double d) {
        imageStrength = d;
    }

    public void updateHoverNode(Hover _hoverNode) {
        hoverNode = _hoverNode;
    }
    
    public void updateHoverLink(Hover _hoverLink) {
        hoverLink = _hoverLink;
    }
    
    public void updateHoverAgent(EvacuationAgent agent) {
        hoverAgent = agent;
    }
    
    public void updateHoverArea(PollutedArea area) {
        hoverArea = area;
    }
    
    public void updateSelectedArea(Rectangle2D rect) {
        selectedArea = rect;
    }
    
    public void setLinkColor(Color c) {
        linkColor = c;
    }

    public void setShowNodes(boolean showNodes) {
        this.showNodes = showNodes;
    }

    public void setShowNodeNames(boolean showNodeNames) {
        this.showNodeLabels = showNodeNames;
    }
    
    public void setShowLinkNames(boolean showLinkNames) {
        this.showLinkLabels = showLinkNames;
    }

    public void setShowLinks(boolean showLinks) {
        this.showLinks = showLinks;
    }

    public void setShowAgents(boolean showAgents) {
        this.showAgents = showAgents;
    }
    
    public void setShowGroups(boolean showGroups) {
        this.showGroups = showGroups;
    }
    
    public void setShowPollution(boolean showPollution) {
        this.showPollution = showPollution;
    }

    public void setShowScaling(boolean showScaling) {
        this.showScaling = showScaling;
    }
    
    public void setScaleLine(Line2D line) {
        scaleLine = line;
    }
    
    public void setTempLine(Line2D line) {
        tempLine = line;
    }
    
    public void setBackgroundGroup(MapPartGroup group) {
        backgroundGroup = group;
    }
    // tkokada
    /*
    private void writeObject(ObjectOutputStream stream) {
        try {
            stream.defaultWriteObject();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream stream) {
        try {
            stream.defaultReadObject();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
    }
    */
}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
