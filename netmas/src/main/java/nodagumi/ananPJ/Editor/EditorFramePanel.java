// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Editor;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.OBNodeSymbolicLink;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.NetworkMap.Area.MapArea;
import nodagumi.ananPJ.misc.Hover;
import nodagumi.ananPJ.navigation.NavigationHint;

/* a panel specifically designed to draw objects on a 
 * specific editor frame. note that the frame is further related to
 * a specific group.
 */
public class EditorFramePanel extends JPanel {

    public static enum TextPosition { CENTER, UPPER, LOWER, LEFT, RIGHT }

    /* -- Parameters
     */
    /* parameters related to what is edited */
    private EditorFrame frame = null;
    private MapPartGroup ob_node = null;

    /* parameters related to editing */
    private Hover hoverNode = null;
    private Hover hoverLink = null;
    private MapArea hoverArea = null;
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

    // 一方通行の編集時に表示するラベル関連
    private boolean showOneWayIndicator = false;
    private MapNode oneWayfirstNode = null;
    private MapNode oneWaylastNode = null;
    private TextPosition oneWayLabelPositionA = TextPosition.CENTER;
    private TextPosition oneWayLabelPositionB = TextPosition.CENTER;

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

    public void setPosition(int dx, int dy) {
        tx = dx;
        ty = dy;
    }

    public void localScroll(int dx, int dy) {
        ob_node.tx += dx / scale;
        ob_node.ty += dy / scale;
    }
    
    public void localZoomX(int z) {
        if (z > 0) {
            ob_node.sx -= 0.01;
        } else {
            ob_node.sx += 0.01;
        }
    }
    
    public void localZoomY(int z) {
        if (z > 0) {
            ob_node.sy -= 0.01;
        } else {
            ob_node.sy += 0.01;
        }
    }

    public void setLocalZoomX(double sx) {
        ob_node.sx = sx;
    }

    public void setLocalZoomY(double sy) {
        ob_node.sy = sy;
    }
    
    public void localRotate(int z) {
        ob_node.r += (z * Math.PI) / 64;
        repaint();
    }
    
    public void centering(boolean withScaling) {
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
        if (minX == Double.MAX_VALUE) {
            tx = 0.0;
            ty = 0.0;
            scale = 1.0;
            return;
        }

        final double width = maxX - minX;
        final double height = maxY - minY;
        final double scaleX = (getWidth() - 20) / width;
        final double scaleY = (getHeight() - 60) / height;
        
        if (withScaling) {
            scale = Math.min(scaleX, scaleY);
        }
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
        
        // viewArea を使って表示範囲外の無駄な描画処理を省く
        Dimension frameSize = new Dimension();
        frame.getSize(frameSize);
        Point2D position = revCalcPos(0, 0);
        Point2D size = revCalcPos(frameSize.width, frameSize.height);
        Rectangle2D viewArea = new Rectangle2D.Double(position.getX(), position.getY(), (size.getX() - position.getX()) + 1, (size.getY() - position.getY()) + 1);

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

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)imageStrength));
            g.drawImage(backgroundImage, 0, 0,
                    (int)(backgroundImage.getWidth(null)),
                    (int)(backgroundImage.getHeight(null)),
                    this);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            //System.out.println("tx:" + tx + ", ty:" + ty + ", obnode tx:" + ob_node.tx + ", ty:" + ob_node.ty + ", sx:" + ob_node.sx + ", sy:" + ob_node.sy + ", r:" + ob_node.r + ", scale:" + scale);

            g.setTransform(t);
        }

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
                    if (viewArea.intersectsLine(link.getLine2D())) {
                        g.setColor(Color.WHITE);
                        link.drawInEditor(g, showLinkLabels, false, showScaling);
                    }
                }
            }
            if (showNodes) {
                g.setStroke(new BasicStroke(1.0f));
                if (backgroundGroup.getChildNodes() != null) {
                    for (MapNode node : backgroundGroup.getChildNodes()) {
                        if (viewArea.contains(node.getX(), node.getY())) {
                            node.drawInEditor(g, false, false, Color.LIGHT_GRAY);
                        }
                    }
                }
            }
        }
        
        /* actual objects */
        if (showPollution) {
            for (final MapArea area : frame.getChildMapAreas()) {
                // tkokada
                // apply a rotation rectangle, here.
                double angle = area.getAngle();
                if (!Double.isNaN(angle)) {
                    AffineTransform af = g.getTransform();
                    af.rotate(angle, ((Rectangle2D) area.getShape()).getCenterX(), ((Rectangle2D) area.getShape()).getCenterY());
                    // draw center point of map area
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
                    area.drawInEditor(g) ;
                    g.setTransform(t);
                } else {
                    area.drawInEditor(g) ;
                }
            }
        }

        if (showLinks) {
            for (final MapLink link : frame.getChildLinks()) {
                if (viewArea.intersectsLine(link.getLine2D())) {
                    g.setColor(linkColor);
                    link.drawInEditor(g, showLinkLabels, false, showScaling);
                }
            }
        }
        
        if (showNodes) {
            g.setStroke(new BasicStroke(1.0f));
            if (frame.getChildNodes() != null) {
                for (MapNode node : frame.getChildNodes()) {
                    if (viewArea.contains(node.getX(), node.getY())) {
                        node.drawInEditor(g, showNodeLabels, false);
                    }
                }
            }
        }
        
        if (showGroups) {
            for (MapPartGroup group : frame.getChildGroups()) {
                group.drawInEditor(g, showSubGroups);
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
                    link.drawInEditor(g, showLinkLabels, true, showScaling);
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
                    node.drawInEditor(g, showNodeLabels, true);
                }
            }
        }

        /* temporary objects */
        if (hoverNode != null) {
            if (hoverNode.orig_node != null) {
                hoverNode.orig_node.drawInEditor(g, true, true);
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
            // ラベルの描画
            if (hoverLink.orig_link != null) {
                hoverLink.orig_link.drawLabel(g, showScaling);
            }
        }
        
        if (hoverArea != null) {
            g.setColor(Color.BLUE);
            // tkokada
            // apply the rotated rectangles to show.
            boolean drawSelectedArea = false;
            for (MapArea area : frame.getChildMapAreas()) {
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
        // draw selected MapArea by using EDIT_POLLUTION multi selection
        for (MapArea area: frame.getChildMapAreas()) {
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

        if (showOneWayIndicator) {
            g.setColor(Color.RED);
            drawScaleFixedText(g, oneWayfirstNode.getX(), oneWayfirstNode.getY(),
                    "A", oneWayLabelPositionA, Font.BOLD, 18);
            drawScaleFixedText(g, oneWaylastNode.getX(), oneWaylastNode.getY(),
                    "B", oneWayLabelPositionB, Font.BOLD, 18);
        }
    }

    // 背景画像を読み込み後直ちに表示する
    @Override
    public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) {
        if ((infoflags & ALLBITS) == 0) {
            return true;
        } else {
            repaint();
            return false;
        }
    }

    private void showHoverNodeInfo(Graphics2D g) {
        g.setColor(Color.MAGENTA);
        g.setFont(new Font("Roman", Font.ITALIC, (int) (24 / scale)));
        g.drawString(hoverNode.getTagString(),
                (int)hoverNode.getX(), (int)hoverNode.getY());

        MapLinkTable paths = hoverNode.getPathways();
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

    /**
     * 指定された位置、フォントスタイル、フォントサイズで text を描画する。
     * フォントサイズは描画スケールの影響を受けない。
     */
    public void drawScaleFixedText(Graphics2D g, double x, double y,
            String text, TextPosition position, int fontStyle, int fontSize) {
        double scale = g.getTransform().getScaleX();
        Font font = new Font("SansSerif", fontStyle, (int)(fontSize / scale));
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();

        double dx = 0.0;
        double dy = 0.0;
        switch (position) {
        case CENTER:
            dx = textWidth / -2.0;
            dy = textHeight / 5.0;
            break;
        case UPPER:
            dx = textWidth / -2.0;
            dy = textHeight / -3.0;
            break;
        case LOWER:
            dx = textWidth / -2.0;
            dy = textHeight / 1.3;
            break;
        case LEFT:
            dx = -textWidth - textHeight / 3.0;
            dy = textHeight / 5.0;
            break;
        case RIGHT:
            dx = textHeight / 3.0;
            dy = textHeight / 5.0;
            break;
        }

        g.drawString(text, (float)(x + dx), (float)(y + dy));
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
    
    public void updateHoverArea(MapArea area) {
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
    
    public void setOneWayIndicator(boolean enabled, MapNode firstNode, TextPosition positionA, MapNode lastNode, TextPosition positionB) {
        showOneWayIndicator = enabled;
        oneWayfirstNode = firstNode;
        oneWaylastNode = lastNode;
        oneWayLabelPositionA = positionA;
        oneWayLabelPositionB = positionB;
    }

    public void setBackgroundGroup(MapPartGroup group) {
        backgroundGroup = group;
    }
}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
