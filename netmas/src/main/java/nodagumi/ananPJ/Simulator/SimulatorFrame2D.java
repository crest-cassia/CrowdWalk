package nodagumi.ananPJ.Simulator;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.swing.JFrame;
import javax.swing.JPanel;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.NetworkParts.Link.Lift;
import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.misc.GetDoublesDialog;

public class SimulatorFrame2D extends JFrame 
	implements MouseListener {
	/**
	 * A 2D version of simulation preview
	 */
	EvacuationSimulator simulator = null;
	Image backgroundImage = null;
	int layer = 0;

	double scale = 1.0;
	double tx = 0.0, ty = 0.0;
	double displayMinHeight = Double.MAX_VALUE, displayMaxHeight = Double.MIN_VALUE;
	double minHeight = Double.MAX_VALUE, maxHeight = Double.MIN_VALUE;
	
	public void promptDisplaySettings() {
		final String[] labels = {"Scale", "TX", "TY", "Lowest", "Highest"}; 
    	double[][] range = {
    			{-10000, 10000, scale},
    			{-10000, 10000, tx},
    			{-10000, 10000, ty},
    			{-10000, 10000, displayMinHeight},
    			{-10000, 10000, displayMaxHeight}}; 
    	double[] ret = GetDoublesDialog.showDialog("Show Options", labels, range);
    	if (ret != null) {
    		scale = ret[0];
    		tx = ret[1];
    		ty = ret[2];
    		displayMinHeight = ret[3];
    		displayMaxHeight = ret[4];
    	}
	}

	double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
	double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;

	public SimulatorFrame2D(EvacuationSimulator _simulator, int _layer) {
		super("Simulation Frame");
		simulator = _simulator;
		layer = _layer;

		//promptDisplaySettings();
		setupDisplaySettings();
		
		JPanel panel = new JPanel(){

			@Override
			public void paintComponent (Graphics g0) {
		    	super.paintComponent(g0);
		    	
		    	Graphics2D g = (Graphics2D)g0;

		    	/* draw agents for each height */
		    	g.drawRect(798, 10, 204, 714);
		    	g.drawRect(800, 12, 200, 710);

		    	final double widthRatio = 160 / (maxY - minY);
		    	final double heightRatio = 640 / (maxHeight - minHeight);
		    	for (AgentBase agent : simulator.getAgents()) {
		    		if (agent.isEvacuated()) continue;
		    		final Point2D pos = agent.getPos();
		    		final double height = agent.getHeight();
		    		
		    		final int x = (int)((pos.getY() - minY) * widthRatio) + 820; 
		    		final int y = (int)((maxHeight - height) * heightRatio) + 62;
		    		
		    		g.fillOval(x, y, 3, 3);
		    	}
		    	/* draw lifts */
		    	Lift.LiftManager lm = Lift.LiftManager.getInstance();
		    	for (String shaftKey : lm.keySet()) {
		    		Lift.Shaft shaft = lm.get(shaftKey);
		    		final Point2D pos = shaft.get(0).getFrom().getAbsoluteCoordinates();
		    		final int x = (int)((pos.getY() - minY) * widthRatio) + 820; 
		    		final double sh = shaft.get(shaft.size() - 1).getFrom().getHeight();
		    		final int sy = (int)((maxHeight - sh) * heightRatio) + 62;
		    		final double eh = shaft.get(0).getTo().getHeight();
		    		final int ey = (int)((maxHeight - eh) * heightRatio) + 62;
		    		g.setColor(Color.YELLOW);
		    		g.drawLine(x, sy, x, ey);
		    	}
		    	
	    		g.setColor(Color.BLACK);

	    		/* draw network */
		    	g.scale(scale, scale);
		    	g.translate(tx, ty);

		    	if (backgroundImage != null) {
		    		g.drawImage(backgroundImage, 0, 0,
		    				(int)(backgroundImage.getWidth(null)),
		    				(int)(backgroundImage.getHeight(null)),
		    				null);
		    	}
				System.out.println("step1");
		    	for (final MapLink link : simulator.getLinks()) {
                    // temporally non scaling
		    		link.draw(g, true, false, false, false);
		    	}
				System.out.println("step2");
		    	g.setStroke(new BasicStroke(1.0f));
		    	for (MapNode node : simulator.getNodes()) {
		    		node.draw(g, true, false, false);
		    	}
				System.out.println("step3");
		    	for (AgentBase agent : simulator.getAgents()) {
		    		agent.draw(g, true);
		    	}
				System.out.println("step4)");
		    }
		};
		setSize(1024, 768);
		
		System.out.println("before add(panel)");
		add(panel);
		System.out.println("after add(panel)");
		
		// Runtime.getRuntime().gc();
		
		addMouseListener(this);
	}

	private void setupDisplaySettings() {
    	for (final MapNode node : simulator.getNodes()) {
    		//if (!node.isInLayer(layer, minHeight, maxHeight)) continue;
    		final double x = node.getX();
    		minX = Math.min(x, minX);
    		maxX = Math.max(x, maxX);
    		final double y = node.getY();
    		minY = Math.min(y, minY);
    		maxY = Math.max(y, maxY);
    		
    		minHeight = Math.min(node.getHeight(), minHeight);
    		maxHeight = Math.max(node.getHeight(), maxHeight);
    		displayMinHeight = minHeight;
    		displayMaxHeight = maxHeight;
    	}
    	final double width = maxX - minX;
    	final double height = maxY - minY;
    	
    	final double scaleX = 500.0 / width;
    	final double scaleY = 500.0 / height;
    	
    	scale = Math.min(scaleX, scaleY);
    	tx = -minX + 10;
    	ty = minY;
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		promptDisplaySettings();
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}
}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
