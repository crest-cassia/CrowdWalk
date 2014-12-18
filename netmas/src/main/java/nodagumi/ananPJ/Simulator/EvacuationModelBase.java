package nodagumi.ananPJ.Simulator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import nodagumi.ananPJ.NetworkMap;
import nodagumi.ananPJ.Agents.EvacuationAgent;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.NetworkParts.Pollution.PollutedArea;
import nodagumi.ananPJ.misc.NetmasPropertiesHandler;

public interface EvacuationModelBase {
    public NetworkMap getMap();
    public MapLinkTable getLinks();
    public MapNodeTable getNodes();
    public List<EvacuationAgent> getAgents();
    public ArrayList<PollutedArea> getPollutions();
    public AgentHandler getAgentHandler();

    public double getTimeScale ();
    public int getDisplayMode();

    public double getTickCount();
    public int getScreenshotInterval();
    public void setScreenshotInterval(int t);

    public void registerAgent(EvacuationAgent agent);

    public void start();
    public void pause();
    public void step();
    public boolean isRunning();

    public void recalculatePaths();
    public NetmasPropertiesHandler getProperties();
}
