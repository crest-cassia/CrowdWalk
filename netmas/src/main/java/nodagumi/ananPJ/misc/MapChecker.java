package nodagumi.ananPJ.misc;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.ProgressMonitor;

import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;



class ProgressBarDialog extends JDialog 
	implements ActionListener {
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
	public static boolean checkForPiledNodes(MapNodeTable nodes) {
		ProgressMonitor bar = new ProgressMonitor(null,
	    		"Checking node collisions",
                "", 0, nodes.size() - 1);

		HashMap<Point2D, MapNodeTable > collisions =
			new HashMap<Point2D, MapNodeTable >();
		for (int i = 0; i < nodes.size() - 1; ++i) {
			bar.setProgress(i);
			for (int j = i + 1; j < nodes.size(); ++j){
				MapNode lhs = nodes.get(i);
				MapNode rhs = nodes.get(j);
				
				if (lhs.getAbsoluteCoordinates().equals(rhs.getAbsoluteCoordinates())) {
					Point2D pos = lhs.getAbsoluteCoordinates();
					MapNodeTable nodesHere = collisions.get(pos);
					if (nodesHere == null) {
						nodesHere = new MapNodeTable();
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

		for (MapNodeTable cnodes : collisions.values()) {
			for (MapNode node : cnodes) {
				node.selected = true;
			}
		}
		
		return false;
	}
	
	/* [2014.12.26]
	 * ターゲットタグを指定するようにした。
	 */
	public static MapLinkTable getReachableLinks(MapNodeTable nodes,
												 String targetTag) {
		final MapNodeTable exits = new MapNodeTable();
    	for (final MapNode node : nodes) {
    		if (node.hasTag(targetTag)) exits.add(node);
    	}

		Stack<MapNode> path = new Stack<MapNode>();
		Stack<Integer> selection = new Stack<Integer>();
		MapLinkTable reachableLinks = new MapLinkTable();
		
		for (MapNode exit : exits) {
			path.push(exit);
			selection.push(0);
			
			while(!path.empty()) {
				MapNode node = path.peek();
				Integer index = selection.pop();

				final MapLinkTable paths  = node.getPathways();
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
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
