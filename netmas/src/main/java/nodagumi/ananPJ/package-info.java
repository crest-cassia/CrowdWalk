// -*- mode: java; indent-tabs-mode: nil -*-
/**
 * CrowdWalk の概要.
 *
 * <hr>
 * <h3>設定ファイルの書き方</h3>
 * <ul>
 *  <li> {@link nodagumi.ananPJ.misc.NetmasPropertiesHandler 実行properties}</li>
 *  <li> {@link nodagumi.ananPJ.Scenario.Scenario シナリオ記述}</li>
 *  <li> {@link nodagumi.ananPJ.misc.AgentGenerationFile エージェント生成ルール}</li>
 *  <li> {@link nodagumi.ananPJ.misc.GenerateAgent エージェント設定 config}</li>
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
 *       {@link nodagumi.ananPJ.misc.GenerateAgent エージェント設定 config} を
 *       参照。
 *   </li>
 *   <li> 全ての設定項目について、
 *        シミュレーション実行のための properties ファイルの
 *        "fallback_file" で指定した JSON ファイルにおいて規定値を指定できる。
 *        記載例は、"sample/simpleGrid/fallbackParameters.json" を参照。
 *   </li>
 *   <li> 全ての設定項目について、
 *        開発レポジトリの "src/main/resources/fallbackParameters.json" に
 *        おいて規定値を指定できる。
 *        このファイルは、buildする段階で jar ファイルに取り込まれる。
 *   </li>
 *   <li> 上記のいずれにも指定されなかった設定項目については、プログラム中で、
 *        指定した定数が規定値として指定される。
 *   </li>
 * </ol>
 * 上記の優先順位に則ったパラメータの設定は、できるだけ、標準的なメソッドとして
 * 用意されるべきである。
 * 例えば、{@link nodagumi.ananPJ.Agents.AgentBase AgentBase クラス}の
 * getDoubleFromConfig(), getIntFromConfig(), getTermFromConfig() などの
 * メソッドである。
 */
package nodagumi.ananPJ;
