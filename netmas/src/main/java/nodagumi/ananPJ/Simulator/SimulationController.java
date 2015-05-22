// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Simulator;

public interface SimulationController {
	public void start();
	public void pause();
	public void step();
	public boolean isRunning();
	
	/* callback from the simulator */
	public SimulationPanel3D setupFrame(EvacuationSimulator simulator);
    public SimulationPanel3D setupFrame(EvacuationSimulator simulator,
            SimulationPanel3D panel);
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
