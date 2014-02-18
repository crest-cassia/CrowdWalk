package nodagumi.ananPJ;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;

import javax.swing.JFrame;

import com.sun.j3d.utils.pickfast.PickCanvas;


public abstract class NetworkPanel3D extends NetworkPanel3DBase 
    implements Serializable {
    /**
     * Listen to user events
     * - User keystrokes
     * - Picking objects
     * - View control by mouse dragging
     */
    private static final long serialVersionUID = -6526471479789757625L;
    protected NetworkPanel3D(ArrayList<MapNode> _nodes,
            ArrayList<MapLink> _links, JFrame _parent) {
        super(_nodes, _links, _parent);
    }

    protected void deserialize(ArrayList<MapNode> _nodes,
            ArrayList<MapLink> _links, JFrame _parent) {
        super.deserialize(_nodes, _links, _parent);
    }

    protected transient PickCanvas pick_canvas;
    protected int button_held_down = 0;
    protected double drag_sensitivity = 0.01;
    
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
        trans_trans.x += (int)dx;
        trans_trans.y -= (int)dy;
        view_changed();
        update_viewtrans();
    }

    private void drag_screen_left(double dx, double dy, MouseEvent e) {
        rot_x += dy * drag_sensitivity; 
        rot_z += dx * drag_sensitivity;

        if (!e.isShiftDown()) {
            if (rot_x > 0) rot_x = 0;
            if (rot_x < -Math.PI / 2) rot_x = -Math.PI / 2;
        }
        view_changed();
        update_viewtrans();
    }

    protected void mouseWheelCallback(int c) {
        if (c > 0) {
            for (int i = 0; i < c; i++) zoom_scale *= 0.9;
        } else {
            c = -c;
            for (int i = 0; i < c; i++) zoom_scale *= 1.1;
        }
        view_changed();
        update_viewtrans();
    }
    
    protected void view_changed() {
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
}
//;;; Local Variables:
//;;; mode:java
//;;; c-basic-offset:4
//;;; tab-width:4
//;;; End:
