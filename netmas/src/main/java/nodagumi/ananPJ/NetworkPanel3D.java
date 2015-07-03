// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;

import nodagumi.ananPJ.Gui.ViewChangeListener;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.misc.NetmasPropertiesHandler;

import javax.swing.JFrame;

import com.sun.j3d.utils.pickfast.PickCanvas;


public abstract class NetworkPanel3D extends NetworkPanel3DBase {
    /**
     * Listen to user events
     * - User keystrokes
     * - Picking objects
     * - View control by mouse dragging
     */
    protected NetworkPanel3D(MapNodeTable _nodes,
            MapLinkTable _links, JFrame _parent, NetmasPropertiesHandler _properties) {
        super(_nodes, _links, _parent, _properties);

        addViewChangeListener("viewpoint changed", new ViewChangeListener() {
            public void update() {
                update_viewtrans();
            }
        });
    }

	protected void setupFrame(MapNodeTable _nodes,
							  MapLinkTable _links, JFrame _parent) {
		super.setupFrame(_nodes, _links, _parent);
	}

    protected transient PickCanvas pick_canvas;
    protected int button_held_down = 0;
    protected double drag_sensitivity = 0.01;
    protected boolean viewpointChangeInhibited = false;
    
    @Override
    protected void setupContents() {
        synchronized(this) {
            super.setupContents();

            if (getIsInitialized()) {
                KeyListener[] removedKListeners;
                removedKListeners = canvas.getKeyListeners();
                for (KeyListener kl : removedKListeners)
                    canvas.removeKeyListener(kl);
                MouseListener[] removedMListeners;
                removedMListeners = canvas.getMouseListeners();
                for (MouseListener ml : removedMListeners)
                    canvas.removeMouseListener(ml);
                MouseMotionListener[] removedMMListeners;
                removedMMListeners = canvas.getMouseMotionListeners();
                for (MouseMotionListener mml : removedMMListeners)
                    canvas.removeMouseMotionListener(mml);

                MouseWheelListener[] removedMWListeners;
                removedMWListeners = canvas.getMouseWheelListeners();
                for (MouseWheelListener mwl : removedMWListeners)
                    canvas.removeMouseWheelListener(mwl);
                pick_canvas = null;
            }
            /* User key events */
            canvas.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {
                    keyTypedCallback(e.getKeyChar());
                }

                /* not used */
                public void keyPressed(KeyEvent e) {
                    int keycode = e.getKeyCode();
                    if (keycode == KeyEvent.VK_CONTROL)
                        cntlPressed = true;
                }

                public void keyReleased(KeyEvent e) {
                    int keycode = e.getKeyCode();
                    if (keycode == KeyEvent.VK_CONTROL && cntlPressed)
                        cntlPressed = false;
                }
            });
            /* Picking objects */
            pick_canvas = new PickCanvas(canvas, scene);
            canvas.addMouseListener(new MouseListener() {
                public void mouseClicked(MouseEvent e) {
                    mouseClickedCallback(e);
                }

                public void mousePressed(MouseEvent e) {
                    button_held_down = e.getButton();
                }
                public void mouseReleased(MouseEvent e) {
                    button_held_down = 0;
                    last_point = null;
                }

                /* not used */
                public void mouseEntered(MouseEvent arg0) {}
                public void mouseExited(MouseEvent arg0) {}
            });
            /* View movement */
            canvas.addMouseMotionListener(new MouseMotionListener() {
                public void mouseMoved(MouseEvent e) {
                    mouseMovedCallback(e);
                }

                public void mouseDragged(MouseEvent e) {
                    mouseDraggedCallback(e);
                }
            });
            canvas.addMouseWheelListener(new MouseWheelListener() {
                public void mouseWheelMoved(MouseWheelEvent e) {
                    mouseWheelCallback(e.getWheelRotation());
                }
            });
        }
    }

    protected void mouseClickedCallback(MouseEvent e) {

    }
    protected void mouseMovedCallback(MouseEvent e) {

    }

    /* modify viewTrans here
     */
    protected Point2D last_point = null;
    // tkokada
    protected boolean cntlPressed = false;
    protected void mouseDraggedCallback(MouseEvent e) {
        Point2D point = e.getPoint();
        if (last_point != null) {
            switch(button_held_down) {
            case MouseEvent.BUTTON1:
                if (cntlPressed) {
                    drag_screen_right(point.getX() - last_point.getX(),
                            point.getY() - last_point.getY(),
                            e);
                } else {
                    drag_screen_left(point.getX() - last_point.getX(),
                            point.getY() - last_point.getY(),
                            e);
                }
                break;
            case MouseEvent.BUTTON2:
                break;
            case MouseEvent.BUTTON3:
                drag_screen_right(point.getX() - last_point.getX(),
                        point.getY() - last_point.getY(),
                        e);
                break;
            default:
                break;
            }
        }
        last_point = point;
    }

    private void drag_screen_right(double dx, double dy, MouseEvent e) {
        if (getViewpointChangeInhibited()) {
            return;
        }
        trans_trans.x += (int)dx;
        trans_trans.y -= (int)dy;
        notifyViewChange("viewpoint changed");
    }

    private void drag_screen_left(double dx, double dy, MouseEvent e) {
        if (getViewpointChangeInhibited()) {
            return;
        }
        rot_x += dy * drag_sensitivity; 
        rot_z += dx * drag_sensitivity;

        if (!e.isShiftDown()) {
            if (rot_x > 0) rot_x = 0;
            if (rot_x < -Math.PI / 2) rot_x = -Math.PI / 2;
        }
        notifyViewChange("viewpoint changed");
    }

    protected void mouseWheelCallback(int c) {
        if (getViewpointChangeInhibited()) {
            return;
        }
        if (c > 0) {
            for (int i = 0; i < c; i++) zoom_scale *= 0.9;
        } else {
            c = -c;
            for (int i = 0; i < c; i++) zoom_scale *= 1.1;
        }
        notifyViewChange("viewpoint changed");
    }
    
    protected void keyTypedCallback(char c) {
        switch(c) {
        case 'h':
            setViewToHome();
            break;
        default:
            break;
        }
    }

    /**
     * マウス操作による視点移動とズーム機能を一時的に禁止するかどうかのフラグをセットする.
     */
    public synchronized void setViewpointChangeInhibited(boolean b) {
        viewpointChangeInhibited = b;
    }

    /**
     * マウス操作による視点移動とズーム機能を一時的に禁止するかどうかを返す.
     *
     * @return 禁止する場合は true
     */
    public synchronized boolean getViewpointChangeInhibited() {
        return viewpointChangeInhibited;
    }
}
//;;; Local Variables:
//;;; mode:java
//;;; c-basic-offset:4
//;;; tab-width:4
//;;; End:
