package nodagumi.ananPJ.misc;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.ProgressMonitor;

import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;



class ProgressBarDialog extends JDialog 
    implements ActionListener, Serializable {
	int count;

	JProgressBar bar;
	JButton cancel;
	
	boolean canceled = false;
	
	public ProgressBarDialog(String message, int start, int end) {
		count = start;

		setLayout(new BorderLayout());
		
		add(new JLabel(message), BorderLayout.WEST);
		
		bar = new JProgressBar(start, end);
		bar.setValue(start);
		bar.setStringPainted(true);
		add(bar, BorderLayout.CENTER);

		cancel = new JButton("Cancel");
		cancel.addActionListener(this);
		add(cancel, BorderLayout.EAST);
		
		pack();
	}
	
	public boolean step() {
		++count;
		bar.setValue(count);
		repaint();
		
		return canceled;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() == "Cancel") canceled = true;
	}
}

public class MapChecker {
	public static boolean checkForPiledNodes(ArrayList<MapNode> nodes) {
		ProgressMonitor bar = new ProgressMonitor(null,
	    		"Checking node collisions",
                "", 0, nodes.size() - 1);

		HashMap<Point2D, ArrayList<MapNode> > collisions =
			new HashMap<Point2D, ArrayList<MapNode> >();
		for (int i = 0; i < nodes.size() - 1; ++i) {
			bar.setProgress(i);
			for (int j = i + 1; j < nodes.size(); ++j){
				MapNode lhs = nodes.get(i);
				MapNode rhs = nodes.get(j);
				
				if (lhs.getAbsoluteCoordinates().equals(rhs.getAbsoluteCoordinates())) {
					Point2D pos = lhs.getAbsoluteCoordinates();
					ArrayList<MapNode> nodesHere = collisions.get(pos);
					if (nodesHere == null) {
						nodesHere = new ArrayList<MapNode>();
						collisions.put(pos, nodesHere);
					}
					if (!nodesHere.contains(lhs)) nodesHere.add(lhs);
					if (!nodesHere.contains(rhs)) nodesHere.add(rhs);
				}
			}
		}
		bar.close();
		if (collisions.size() == 0)	return true;

		int i = JOptionPane.showConfirmDialog(null,
				"" + collisions.size() + " collisions were found.\n"
				+ "Resolve them?", "Collisions found", JOptionPane.YES_NO_OPTION);
		if (i == JOptionPane.NO_OPTION) return false;

		for (ArrayList<MapNode> cnodes : collisions.values()) {
			for (MapNode node : cnodes) {
				node.selected = true;
			}
		}
		
		return false;
	}
	
	public static ArrayList<MapLink> getReachableLinks(ArrayList<MapNode> nodes) {
		final ArrayList<MapNode> exits = new ArrayList<MapNode>();
    	for (final MapNode node : nodes) {
    		if (node.hasTag("Exit")) exits.add(node);
    	}

		Stack<MapNode> path = new Stack<MapNode>();
		Stack<Integer> selection = new Stack<Integer>();
		ArrayList<MapLink> reachableLinks = new ArrayList<MapLink>();
		
		for (MapNode exit : exits) {
			path.push(exit);
			selection.push(0);
			
			while(!path.empty()) {
				MapNode node = path.peek();
				Integer index = selection.pop();

				final ArrayList<MapLink> paths  = node.getPathways();
				if (index == paths.size()) {
					path.pop();
					continue;
				}
				
				final MapLink link = paths.get(index);
				++index;
				selection.push(index);

				if (!reachableLinks.contains(link)) {
					reachableLinks.add(link);
					path.push(link.getOther(node));
					selection.push(0);
				}
			}
		}
		return reachableLinks;
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
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
