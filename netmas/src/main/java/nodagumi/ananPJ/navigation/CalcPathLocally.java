// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.navigation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.navigation.CalcPath.NodeLinkLen;
import nodagumi.ananPJ.navigation.CalcPath.Nodes;
import nodagumi.ananPJ.navigation.CalcPath.PathChooser;
import nodagumi.ananPJ.navigation.CalcPath.PathChooserFactory;

public class CalcPathLocally extends Thread {
	/* Calculates the path for nodes on the same floor.
	 */

	/* interfaces */
	public static void calc(CalcPath.NodeByHeight nodesByHeight,
			PathChooserFactory factory) {

		//TODO run the following in threads
		for (double height : nodesByHeight.keySet()) {
			System.err.print("caluculating for " + height);
			System.err.print("(" + nodesByHeight.get(height).size() + " nodes)");
			PathChooser chooser = factory.generate(height); 
			Nodes subgoals = calcSubGoals(factory.hintName(),
					height,
					nodesByHeight.get(height),
					chooser);
			Dijkstra.Result result = Dijkstra.calc(subgoals, chooser);
			mergeHints(factory.hintName(), result);
			System.err.println(" done");
		}
	}

	/* workers */
	static Nodes calcSubGoals(String realm,
			double height,
			Nodes nodes,
			PathChooser chooser) {
		Nodes subgoals = new Nodes();
		//TODO: exits and other stairs are treated equally, but should not be
		for (MapNode node : nodes) {
			for (MapLink link : node.getPathways()) {
				if (chooser.isExit(link)) {
					subgoals.add(node);
					break;
				}
			}
		}
		return subgoals;
	}

	static void mergeHints(String realm,
			Dijkstra.Result result) {
		for (MapNode node : result.keySet()) {
			NodeLinkLen nll = result.get(node);
			if (nll.node == null) continue; /* subgoal */
			node.addNavigationHint(realm,
					new NavigationHint(nll.node, nll.link, nll.len));
            System.out.println("CalcPathLocally.calcSubGoals call " +
                    "addNavigationHint node:" + node.ID + ", " + realm);
		}
	}
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
