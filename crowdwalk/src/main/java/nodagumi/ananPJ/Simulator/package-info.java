// -*- mode: java; indent-tabs-mode: nil -*-
/**
 * Inside of Simulator.
 *
 * <a name="InsideOfSimulator"></a>
 * <hr>
 * <h2>Inside of Simulation Process</h2>
 * When CrowdWalk starts, it executes the simulation as the following
 * procedure.
 * <h3>Overall Procedure</h3>
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
 *         Call {@code setupSimulationLoggers()} of <b>RubyWrapper</b> if using Ruby.
 *       </li>
 *       <li> 
 *         {@link nodagumi.ananPJ.Simulator.EvacuationSimulator#initLoggers() initLoggers()}
 *       </li>
 *       <li> 
 *         Call {@code prepareForSimulation()} of <b>RubyWrapper</b> if using Ruby.
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
 *     : repeat {@link nodagumi.ananPJ.BasicSimulationLauncher#simulateOneStepBare() simulateOneStepBare()} until {@code finished} becomes {@code true}.
 *     <br>
 *     See <a href="#MainLoop"> Main Loop </a> for details.
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
 *         Call {@code finalizeSimulation} of <b>RubyWrapper</b> if using Ruby.
 *       </li>
 *     </ol>
 *   </li>
 *   <!-- ====================== -->
 * </ol>
 * <!-- ==================================================== -->
 * <a name="MainLoop"></a>
 * <hr>
 * <h3>Main Loop</h3>
 * In {@link nodagumi.ananPJ.BasicSimulationLauncher#simulateOneStepBare() simulateOneStepBare()}, 
 * {@link nodagumi.ananPJ.Simulator.EvacuationSimulator#updateEveryTick() EvacuationSimulator#updateEveryTick()} is called.
 * In the method, the following steps are executed in a cycle.
 * <ol>
 *   <!-- ====================== -->
 *   <li>
 *     Call {@code preUpdate()} of <b>RubyWrapper</b> if using Ruby.
 *   </li>
 *   <!-- ====================== -->
 *   <li>
 *     {@link nodagumi.ananPJ.Simulator.PollutionHandler#updateAll(SimTime,NetworkMap,Collection) PollutionHandler#updateAll()} if
 *     using pollusion data.
 *   </li>
 *   <!-- ====================== -->
 *   <li>
 *     {@link nodagumi.ananPJ.Scenario.Scenario#update(SimTime,NetworkMap) Scenario#update()}
 *   </li>
 *   <!-- ====================== -->
 *   <li>
 *     {@link nodagumi.ananPJ.Simulator.AgentHandler#update(SimTime) AgentHandler#update()}
 *     <ol>
 *       <!-- ========== -->
 *       <li>
 *         {@link nodagumi.ananPJ.Simulator.AgentHandler#generateAgentsAndSetup(SimTime)}
 *       </li>
 *       <!-- ========== -->
 *       <li>
 *         {@link nodagumi.ananPJ.Simulator.AgentHandler#preUpdateNetworkMap(SimTime)}
 *         <ol>
 *           <li>
 *             {@link nodagumi.ananPJ.NetworkMap.Link.MapLink#preUpdate(SimTime) MapLink#preUpdate()}
 *           </li>
 *         </ol>
 *       </li>
 *       <!-- ========== -->
 *       <li>
 *         {@link nodagumi.ananPJ.Simulator.AgentHandler#preUpdateAgents(SimTime)}
 *         <BR>
 *         {@code getEffectiveLinkSet()} の {@code Link}ごとに、
 *         {@link nodagumi.ananPJ.Simulator.AgentHandler#preUpdateAgentsOnLink(MapLink,SimTime)}を呼び出す。
 *         その {@code forwardLane}, {@code backwardLane}について
 *         その上にいる {@code Agent} ごとに、
 *         {@link nodagumi.ananPJ.Agents.AgentBase#preUpdate(SimTime)} を呼び出し。
 *         そこでは以下を実施。
 *         <ol>
 *           <!-- ===== -->
 *           <li>
 *             Call {@code preUpdate} of <b>RubyAgent</b> if possible.
 *           </li>
 *           <!-- ===== -->
 *           <li>
 *             {@link nodagumi.ananPJ.Agents.RationalAgent#thinkCycle()}
 *           </li>
 *           <!-- ===== -->
 *           <li>
 *             {@link nodagumi.ananPJ.Agents.WalkAgent#updateSpeed(SimTime)}
 *             <ol>
 *               <li>
 *                 {@link nodagumi.ananPJ.Agents.WalkAgent#calcSpeed(double, SimTime)}
 *                 <ol>
 *                   <li>
 *                     Call {@code calcSpeed()} of <b>RubyAgent</b> if possible.
 *                   </li>
 *                   <li>
 *                     In {@link nodagumi.ananPJ.Agents.WalkAgent#calcCostFromNodeViaLink(MapLink, MapNode, Term)},
 *                     call {@code calcCostFromNodeViaLink()} of <b>RubyAgent</b> if possible.
 *                   </li>
 *                 </ol>
 *               </li>
 *             </ol>
 *           </li>
 *           <!-- ===== -->
 *           <li>
 *             {@link nodagumi.ananPJ.Agents.WalkAgent#advanceNextPlace(double, SimTime, boolean)}
 *           </li>
 *           <!-- ===== -->
 *         </ol>
 *       </li>
 *       <!-- ========== -->
 *       <li>
 *         {@link nodagumi.ananPJ.Simulator.AgentHandler#updateAgents(SimTime)}
 *         <br> エージェントごとに、以下を実施。
 *         in {@link nodagumi.ananPJ.Agents.WalkAgent#update(SimTime)}.
 *         <ol>
 *           <li>
 *             ゴールにたどり着いていたら、
 *             {@link nodagumi.ananPJ.Agents.AgentBase#finalizeEvacuation(SimTime, boolean, boolean)}.
 *           </li>
 *           <li>
 *             Call {@code update()} of <b>RubyAgent</b> if possible.
 *           </li>
 *           <li>
 *             {@link nodagumi.ananPJ.Agents.WalkAgent#moveToNextPlace(SimTime)}.
 *           </li>
 *         </ol>
 *       </li>
 *       <!-- ========== -->
 *       <li>
 *         {@link nodagumi.ananPJ.Simulator.AgentHandler#updatePollution()}
 *       </li>
 *       <!-- ========== -->
 *       <li>
 *         {@link nodagumi.ananPJ.Simulator.AgentHandler#updateLinks(SimTime)}
 *       </li>
 *       <!-- ========== -->
 *       <li>
 *         表示のための処理
 *       </li>
 *       <!-- ========== -->
 *     </ol>
 *   </li>
 *   <!-- ====================== -->
 *   <li>
 *     Call {@code postUpdate} of <b>RubyWrapper</b> if using Ruby.
 *   </li>
 *   <!-- ====================== -->
 *   <li>
 *     Update clock.
 *   </li>
 *   <!-- ====================== -->
 * </ol>
 */
package nodagumi.ananPJ.Simulator;

