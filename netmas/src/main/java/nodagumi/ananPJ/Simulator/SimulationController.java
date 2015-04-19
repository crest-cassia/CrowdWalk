package nodagumi.ananPJ.Simulator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public interface SimulationController {
	public void start();
	public void pause();
	public void step();
	public boolean isRunning();
	
	/* callback from the simulator */
	public SimulationPanel3D setupFrame(EvacuationSimulator simulator);
    public SimulationPanel3D setupFrame(EvacuationSimulator simulator,
            SimulationPanel3D panel);
	public void notifyViewChange(SimulationPanel3D panel);
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
