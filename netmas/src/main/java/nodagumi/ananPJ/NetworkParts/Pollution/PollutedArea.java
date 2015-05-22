package nodagumi.ananPJ.NetworkParts.Pollution;

import nodagumi.ananPJ.NetworkParts.OBNode;
//import nodagumi.ananPJ.NetworkParts.OBNodeSymbolicLink;


//import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Graphics2D;
//import java.awt.GridLayout;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ClassNotFoundException;
import java.util.ArrayList;

import javax.media.j3d.Appearance;
import javax.media.j3d.TransformGroup;
//import javax.swing.JButton;
import javax.swing.JDialog;
//import javax.swing.JLabel;
//import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.vecmath.Vector3f;

public abstract class PollutedArea extends OBNode {

        protected double lastPollutionLevel = 0.0;  // 更新チェック用

	public PollutedArea(int _id) {
		super(_id);
	}
	
	public abstract boolean contains(Point2D point);
	public abstract boolean contains(Vector3f point);
	public abstract boolean intersectsLine(Line2D line);
	public abstract double distance(Vector3f point);
	public abstract Shape getShape();
	public abstract ArrayList<Point2D> getAllVertices();	// tkokada
	public abstract double getAngle();	// tkokada
	
	public abstract void draw(Graphics2D g, boolean experiment);
	public abstract TransformGroup get3DShape(Appearance app);
	public abstract double getDensity();
	
	//For view customize : contact agent with pollution_area
	public abstract boolean getContactOfAgents();
	public abstract void setContactOfAgents(boolean _view);
	public static void showAttributeDialog(ArrayList<PollutedArea> areas,
			int x, int y) {
        /* Set attributes with a dialog */
    	class AttributeSetDialog  extends JDialog implements ActionListener, KeyListener {

			private ArrayList<PollutedArea> areas;

        	private ArrayList<String> tags;
        	
        	private JTextField[] tagFields;
        	boolean singleArea;

        	public AttributeSetDialog(ArrayList<PollutedArea> _areas) {
        		super();

        		this.setModal(true);
        		areas = _areas;

    			setUpPanel();
        	}
        	
        	private void setUpPanel() {
        		Container contentPane = getContentPane();
        		contentPane.add(OBNode.setupTagPanel(areas, this));

        		this.pack();
        	}
    		@Override
    		public void actionPerformed(ActionEvent e) {
    			if (e.getActionCommand().equals("OK")) {
    				for (PollutedArea area : areas) {
    					if (area.selected) {
    						if (singleArea) {
    							for (int i = 0; i < tags.size(); ++i) {
    								String str = tagFields[i + 2].getText();
    								area.tags.set(i, str);
    							}

    							String new_tag_str = tagFields[1].getText();
    							if (!new_tag_str.equals("")) {
    								area.tags.add(new_tag_str);
    							}
    						} else {
    							String new_tag_str = tagFields[0].getText();
    							area.tags.add(new_tag_str);
    						}
    					}
    				}
    				this.dispose();
    			} else if (e.getActionCommand().equals("Cancel")) {
    				this.dispose();
    			}
    		}

			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
				
			}
        }

    	AttributeSetDialog dialog = new AttributeSetDialog(areas);
       dialog.setLocation(x, y);
    	dialog.setVisible(true);
    }

    @Override
    public void setUserObject(Object userObject) {
        super.setUserObject(userObject);

        double pollutionLevel = ((Double)userObject).doubleValue();
        if (pollutionLevel != lastPollutionLevel) {
            networkMap.getNotifier().pollutionLevelChanged(this);
            lastPollutionLevel = pollutionLevel;
        }
    }
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
