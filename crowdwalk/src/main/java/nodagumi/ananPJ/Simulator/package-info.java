// -*- mode: java; indent-tabs-mode: nil -*-
/**
 * Inside of Simulator.
 *
 * <a name="InsideOfSimulator"></a>
 * <hr>
 * <h3>Inside of Simulation Process</h3>
 * When CrowdWalk starts, it executes the simulation as the following
 * procedure.
 * <ol>
 *   <!-- === init ============= -->
 *   <li> {@link nodagumi.ananPJ.Simulator.EvacuationSimulator#begin begin()}
 *        :initialization
 *     <ol>
 *       <li>
 *         {@link nodagumi.ananPJ.Simulator.EvacuationSimulator#buildMap() buildMap()}
 *       </li>
 *       <li> 
 *         {@link nodagumi.ananPJ.Simulator.EvacuationSimulator#buildScenario() buildScenario()}
 *       </li>
 *       <li> 
 *         {@link nodagumi.ananPJ.Simulator.EvacuationSimulator#buildPollution() buildPollution()}
 *       </li>
 *       <li> 
 *         {@link nodagumi.ananPJ.Simulator.EvacuationSimulator#buildAgentHandler() buildAgentHandler()}
 *       </li>
 *       <li> 
 *         {@link nodagumi.ananPJ.Simulator.EvacuationSimulator#buildRoutes() buildRoutes()}
 *       </li>
 *       <li> 
 *         {@link nodagumi.ananPJ.Simulator.EvacuationSimulator#buildRubyEngine() buildRubyEngine()}
 *       </li>
 *       <li> 
 *         {@link nodagumi.ananPJ.Simulator.AgentHandler#prepareForSimulation() agentHandler.prepareForSimulation()}
 *       </li>
 *       <li> 
 *         call {@code setupSimulationLoggers()} of RubyWrapper if using Ruby.
 *       </li>
 *       <li> 
 *         {@link nodagumi.ananPJ.Simulator.EvacuationSimulator#initLoggers() initLoggers()}
 *       </li>
 *       <li> 
 *         call {@code prepareForSimulation()} of RubyWrapper if using Ruby.
 *       </li>
 *       <li> 
 *         (in {@link nodagumi.ananPJ.BasicSimulationLauncher#initializeSimulatorEntity() initializeSimulatorEntity()})
 *         {@code random.setSeed(properties.getRandseed())}
 *       </li>
 *     </ol>
 *   </li>
 *   <!-- === main ============= -->
 *   <li> 
 *     {@link nodagumi.ananPJ.BasicSimulationLauncher#simulateMainLoop() simulateMainLoop()}
 *     : repeat {@link nodagumi.ananPJ.BasicSimulationLauncher#simulateMainLoop() simulateOneStepBare()} until {@code finished} becomes {@code true}.
 *   </li>
 *   <!-- === finish =========== -->
 *   <li> 
 *     {@link nodagumi.ananPJ.Simulator.EvacuationSimulator#finish() finish()}
 *        :finalize
 *     <ol>
 *       <li>
 *         {@link nodagumi.ananPJ.Simulator.EvacuationSimulator#finalizeLoggers() finalizeLoggers()}
 *       </li>
 *       <li>
 *         call {@code finalizeSimulation} of RubyWrapper if using Ruby.
 *       </li>
 *     </ol>
 *   </li>
 *   <!-- ====================== -->
 * </ol>
 */
package nodagumi.ananPJ.Simulator;

