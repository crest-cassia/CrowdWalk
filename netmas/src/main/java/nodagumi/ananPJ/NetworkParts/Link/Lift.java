package nodagumi.ananPJ.NetworkParts.Link;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.NetworkParts.Link.*;

public class Lift extends MapLink {
	/**
	 * A simplified model of a lift.
	 * - The time to pass the lift is constant.
	 * - The length is the time for an agent to leave this link.
	 * - The width is the capacity of the lift (unused).
	 */
	private double timeToDecend = 0.0;
	MapNode upperFloor = null;

	public static class Shaft extends ArrayList<Lift> {
		/**
		 * 
		 */
		String id = null;
		Lift targetFloor = null;
		double targetTime = 0.0;
		Lift lastFloor = null;
		double lastTime = -1.0;
		double time = 0.0;

		enum State { BOTTOM, TOP, DESCENDING, ASCENDING }
		State state = State.BOTTOM;

		public Shaft(String _id) {
			id = _id;
		}

		public void update(double _time) {
			//TODO ad-hoc
			if (lastTime < 0.0) initialize();

			time = _time;
			if (time < targetTime) return;

			lastTime = time;
			lastFloor = targetFloor; 

			switch (state) {
			case ASCENDING:
				/* reached top, lastFloor = targetFloor = top */
				state = State.TOP;
				targetTime = time + 15.0;
				break;
			case TOP:
				/* start decend, lastFloor = top, targetFloor = bottom */
				state = State.DESCENDING;
				targetFloor = get(size() -1);  
				targetTime = time +
				Math.abs(targetFloor.timeToDecend - lastFloor.timeToDecend);
				break;
			case DESCENDING:
				/* reached bottom, lastFloor = targetFloor = bottom */
				state = State.BOTTOM;
				targetTime = time + 15.0;
				break;
			case BOTTOM:
				/* start ascend, lastFloor = bottom, targetFloor = top */
				targetFloor = findNextTarget();
				if (targetFloor == null) {
					targetFloor = lastFloor;
					break;
				}
				state = State.ASCENDING;
				targetTime = time + 
				Math.abs(targetFloor.timeToDecend - lastFloor.timeToDecend);
				break;
			}
		}
		
		public double getHeight() {
			double fromHeight = lastFloor.getAverageHeight();
			double toHeight = targetFloor.getAverageHeight();
			
			return (time - lastTime) / (targetTime - lastTime)
			* (toHeight - fromHeight) + fromHeight;
		}

		public Point2D getPos() {
			Point2D fromPos = lastFloor.getFrom().getAbsoluteCoordinates();
			Point2D toPos = targetFloor.getTo().getAbsoluteCoordinates();
			
			double r = (time - lastTime) / (targetTime - lastTime);
			double x = r * (toPos.getX() - fromPos.getX()) + fromPos.getX();
			double y = r * (toPos.getY() - fromPos.getY()) + fromPos.getY();
			
			return new Point2D.Double(x, y);
		}

		private Lift findNextTarget() {
			for (Lift lift : this) {
				if (lift.agentWating) {
					return  lift;
				}
			}
			return null;
		}

		private void initialize() {
			Collections.sort(this, new Comparator<Lift>() {
				@Override
				public int compare(Lift lhs, Lift rhs) {
					return (int) (lhs.getTo().getHeight() - rhs.getTo().getHeight());
				}
			});
			
			double timeToDecend = 0.0;
			for (Lift lift : this) {
				timeToDecend += lift.length;
				lift.timeToDecend = timeToDecend;
			}

			Collections.sort(this, new Comparator<Lift>() {
				@Override
				public int compare(Lift lhs, Lift rhs) {
					return (int) (rhs.getTo().getHeight() - lhs.getTo().getHeight());
				}
			});
			
			lastFloor = get(size() - 1);
			targetFloor = get(size() - 1);
		}
	}

	static public class LiftManager extends HashMap<String, Shaft>{
		/**
		 * A singleton that holds all the lift information.
		 */
		static LiftManager manager = null;
		
		private LiftManager() {}
		
		public static LiftManager getInstance() {
			if (manager == null) {
				manager = new LiftManager();
			}
			return manager;
		}
		
		Shaft get(String id) {
			Shaft shaft = super.get(id);
			if (shaft == null) {
				shaft = new Shaft(id);
				super.put(id, shaft);
			}
			return shaft;
		}
	}

	private String shaftId;
	private Shaft shaft;
	
	public Lift(int _id,
			MapNode _from, MapNode _to,
			double _length, double _width) {
		super(_id, _from, _to, _length, _width);
		
		shaftId = findRoot(_from).getAbsoluteCoordinates().toString();
		
		LiftManager manager = LiftManager.getInstance();
		shaft = manager.get(shaftId);
		shaft.add(this);
		upperFloor = (_from.getHeight() > _to.getHeight()) ? _from : _to;
	}

	public MapNode findRoot(MapNode node) {
		final MapLinkTable links = node.getPathways();
		for (final MapLink link : links) {
			if (!link.hasTag("Lift")) continue;
			if (link.getFrom() == node) continue;
			return ((Lift)link).findRoot(link.getFrom());
		}
		return node;
	}

	public MapNode findLeaf(MapNode node) {
		final MapLinkTable links = node.getPathways();
		for (final MapLink link : links) {
			if (!link.hasTag("Lift")) continue;
			if (link.getTo() == node) continue;
			return ((Lift)link).findRoot(link.getTo());
		}
		return node;
	}

	int numAgentsWaiting() {
		int count = 0;
		for (MapLink link : upperFloor.getPathways()) {
			if (link.hasTag("Lift")) continue;
			count += link.getAgents().size();
		}
		return count;
	}

	@Override
	public void update(double time) {
		shaft.update(time);
	}

	private boolean agentWating = false;

	@Override
	public void draw(Graphics2D g,
			boolean in_simulation,
			boolean show_label,
			boolean isSymbolic,
            boolean showScaling) {
		/*
		if (drawLayer == -1) {
			Line2D line = new Line2D.Double(from.getLocalX(), from.height,
					to.getLocalX(), to.height);
			g.setStroke(broad);
			g.setColor(Color.BLACK);
			g.draw(line);
			g.setStroke(narrow);
			g.setColor(Color.WHITE);
			g.draw(line);
		}
		*/
	}
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
