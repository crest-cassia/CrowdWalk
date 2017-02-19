// -*- mode: java; indent-tabs-mode: nil -*-
/**
 * CrowdWalk の概要.
 *
 * <hr>
 * <h3>設定ファイルの書き方</h3>
 * <ul>
 *  <li> {@link nodagumi.ananPJ.misc.CrowdWalkPropertiesHandler 実行properties}</li>
 *  <li> {@link nodagumi.ananPJ.Scenario.Scenario シナリオ記述}</li>
 *  <li> {@link nodagumi.ananPJ.misc.AgentGenerationFile エージェント生成ルール}</li>
 *  <li> {@link nodagumi.ananPJ.Agents.AgentFactory エージェント設定 config}</li>
 *  <li> {@link nodagumi.ananPJ.Agents.Think.ThinkEngine エージェント行動ルール}</li>
 *  <li> <a href="#fallback"> 各種設定とFallback</a></li>
 * </ul>
 * <h4><a name="fallback">各種設定とFallback</a></h4>
 * Agent のconfig など、シミュレーションの各種設定は、以下の優先順位で決定される。
 * <ol>
 *   <li> Agentに関するパラメータについては、
 *       {@link nodagumi.ananPJ.misc.AgentGenerationFile エージェント生成ルール}
 *       の中の、"agentType" の項目で、エージェントクラス名と共に規定値を
 *       指定できる。
 *       設定できる項目はエージェントクラスにより異なる。
 *       各クラスでの設定項目は、
 *       {@link nodagumi.ananPJ.Agents.AgentFactory エージェント設定 config} を
 *       参照。
 *   </li>
 *   <li> 全ての設定項目について、
 *        以下の優先順序で値の設定が行われる。
 *     <ol>
 *       <li> 起動時のコマンドラインオプションで {@code "--fallback <JSON文字列>" }
 *            と指定した値。
 *            JSON文字列の記載方法は、以下の properties ファイルの形式に準じる。
 *            --fallback のオプションは、複数個指定できる。
 *            重複して指定された場合、コマンドラインで後で指定したものが
 *            優先される。
 *       </li>
 *       <li> properties ファイルの "fallback_file" で指定した JSON ファイルに
 *            おいて規定値を指定できる。
 *            記載例は、"sample/simpleGrid/fallbackParameters.json" を参照。
 *      </li>
 *       <li> 開発レポジトリの "src/main/resources/fallbackParameters.json" に
 *            おいて規定値を指定できる。
 *            このファイルは、buildする段階で jar ファイルに取り込まれる。
 *      </li>
 *      <li> 上記のいずれにも指定されなかった設定項目については、プログラム中で、
 *           指定した定数が規定値として指定される。
 *      </li>
 *     </ol>
 *   </li>
 * </ol>
 * プログラム中では、上記の優先順位に則ったパラメータの設定は、
 * {@link nodagumi.ananPJ.misc.SetupFileInfo SetupFileInfo クラス} の
 * {@link nodagumi.ananPJ.misc.SetupFileInfo#filterFallbackTerm filterFallbackTerm()}, 
 * {@link nodagumi.ananPJ.misc.SetupFileInfo#fetchFallbackTerm fetchFallbackTerm()},
 * {@link nodagumi.ananPJ.misc.SetupFileInfo#fetchFallbackString fetchFallbackString()},
 * {@link nodagumi.ananPJ.misc.SetupFileInfo#fetchFallbackDouble fetchFallbackDouble()},
 * {@link nodagumi.ananPJ.misc.SetupFileInfo#fetchFallbackInt fetchFallbackInt()},
 * {@link nodagumi.ananPJ.misc.SetupFileInfo#fetchFallbackBoolean fetchFallbackBoolean()}
 * を使うべきである。
 * 例えば、{@link nodagumi.ananPJ.Agents.AgentBase AgentBase クラス}の
 * {@code getDoubleFromConfig()}, {@code getIntFromConfig()}, 
 * {@code getTermFromConfig()} などの定義を参考にすべきである。
 * <br>
 * 各種フォールバックの説明は、以下を参照。
 * <ul>
 *   <li> {@link nodagumi.ananPJ.Simulator.AgentHandler} </li>
 *   <li> {@link nodagumi.ananPJ.Agents.WalkAgent} </li>
 *   <li> {@link nodagumi.ananPJ.Agents.AwaitAgent} </li>
 *   <li> {@link nodagumi.ananPJ.Agents.NaiveAgent} </li>
 *   <li> {@link nodagumi.ananPJ.Agents.CapriciousAgent} </li>
 *   <li> {@link nodagumi.ananPJ.Agents.BustleAgent} </li>
 *   <li> {@link nodagumi.ananPJ.Agents.RationalAgent} </li>
 *   <li> {@link nodagumi.ananPJ.Agents.RubyAgent} </li>
 *   <li> {@link nodagumi.ananPJ.NetworkMap.Link.MapLink} </li>
 *   <li> {@link nodagumi.ananPJ.NetworkMap.Node.MapNode} </li>
 *   <li> {@link nodagumi.ananPJ.Simulator.Obstructer.ObstructerBase} </li>
 *   <li> {@link nodagumi.ananPJ.Simulator.Obstructer.Pollution} </li>
 *   <li> {@link nodagumi.ananPJ.Simulator.Obstructer.Flood} </li>
 * </ul>
 */
package nodagumi.ananPJ;
