// -*- mode: java; indent-tabs-mode: nil -*-
/**
 * Think Formula: JSON style functions
 *
 * <a name="ThinkFormula"></a>
 * <hr>
 * <h2>Formula Evaluation</h2>
 * Think Formula は、関数形式の計算・判断ルーチン記述であり、
 * 以下の用途で用いられる。
 * <ul>
 *  <li> RationalAgent の判断ルーチン記述。
 *  </li>
 *  <li> 主観的距離マップ(mental map)の計算記述。
 *  </li>
 * </ul>
 * 
 * <h3>形式の概要</h3>
 * Think Formula の記述形式は、LISP のS式 を JSON 形式に修正したものになっている。
 * 具体的には以下の形式になっている。
 * <ul>
 *   <li> LISP S式型標記 <br/>
 *    <pre>
 *     [ "funcName", arg, arg, ... ]
 *    </pre>
 *   </li>
 *   <li> キーワード引数型標記 <br/>
 *    <pre>
 *     { "" : "funcName", 
 *       "key1" : arg1, 
 *       "key2" : "arg2,
 *        ... }
 *    </pre>
 *   </li>
 * </ul>
 */
package nodagumi.ananPJ.Agent.Think;

