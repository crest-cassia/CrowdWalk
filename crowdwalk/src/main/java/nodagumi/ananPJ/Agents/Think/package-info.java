// -*- mode: java; indent-tabs-mode: nil -*-
/**
 * Think Rule Formula: JSON style functions
 *
 * <a name="ThinkFormula"></a>
 * <hr>
 * <h2>Rule Formula</h2>
 * Think Formula は、関数形式の計算・判断ルーチン記述であり、
 * 以下の用途で用いられる。
 * <ul>
 *  <li> {@link nodagumi.ananPJ.Agents.Think.ThinkFormula RationalAgent の判断ルーチン記述}。
 *  </li>
 *  <li> {@link nodagumi.ananPJ.navigation.Formula.NaviFormula 主観的距離マップ(mental map)の計算記述}。
 *  </li>
 * </ul>
 * 
 * <h3>形式の概要</h3>
 * Think Formula の記述形式は以下のふた通りを用意している。
 * これらは混在していても良い。
 * <ul>
 *   <li> 配列型関数記述 : 
 *    JSON の配列の形式で関数を記述する。
 *    <pre>
 *     [ "funcName", Arg, Arg, ... ]
 *    </pre>
 *    引数 Arg には、即値（数値もしくは文字列）、あるいは関数が再帰的に現れてよい。
 *    引数の数は厳密にはチェックされない。余分な引数は無視される。
 *    </br>
 *    この形式は、LISPのS式と同等の記述である。
 *    すなわち、S式の括弧 "(", ")" を "[", "]" に置き換え、
 *    セパレータ(任意長の空白・タブ・改行を ",")にしたものである。
 *    また、LISP の Symbol に相当するものはないため、文字列で代用する。
 *   </li>
 *   <li> Object型関数記述 : 
 *    JSON のObjectの形式で関数を記述する。
 *    <pre>
 *     { "" : "funcName", 
 *       "key1" : Arg1, 
 *       "key2" : Arg2,
 *        ... }
 *    </pre>
 *    引数に対応する "key"-Arg ペアは、その関数に必要なものを指定する。
 *    その関数に必要のない "key" が指定されれば、無視される。
 *   </li>
 *   <li> 上記の2記述方式からはみ出す形式として、以下の特集形式がある。
 *     <ul>
 *        <li> 順序実行記述 : Form1, Form2, ... の順に関数が実行される。
 *          <pre>
 *            [ Form1, Form2, ... ]
 *          </pre>
 *          Form1, Form2 は、関数記述のいずれかである。
 *          よって、Form1 は、文字列の即値であってはならない。
 *        </li> 
 *        <li> 条件分岐 : Cond が "false" もしくは Null 以外の場合、
 *             Then が実行される。
 *             それ以外の場合、Else が実行される。
 *          <pre>
 *            [ "if", Cond, Then (, Else) ]
 *          </pre>
 *             Cond, Then, Else は各々、関数記述である。
 *        </li> 
 *     </ul>
 *   </li>
 * </ul>
 * 
 * <h3>詳細記述へのリンク</h3>
 *   利用できる関数の詳細は、以下のリンク参照。
 * <ul>
 *   <li>{@link nodagumi.ananPJ.Agents.Think.ThinkFormula ThinkFormula}</li>
 *   <li>{@link nodagumi.ananPJ.Agents.Think.ThinkEngine ThinkEngine}:
 *       形式上の定義などの説明。</li>
 * </ul>
 *
 * <h3>具体的な事例</h3>
 *   Formula を使ったいくつかの事例を示す。
 * 
 * <h4>RationalAgentの行動ルール</h4>
 *    {@link nodagumi.ananPJ.Agents.RationalAgent RationalAgent}の行動ルールは、
 *    generation file の "agentType"/"rule" の下に、以下のように記述する。
 *    (sample/generatedTown/gridTown00_bench.rational.gen.json 参照)
 *    <pre>
 *	       "rule":[
 *		   ["if", {"":"listenAlert", "message":"xxx_emergency"},
 *		    [{"":"changeGoal", "goal":"node_09_05"},
 *		     {"":"clearPlannedRoute"},
 *		     {"":"log","tag":"change goal to 09_05"},
 *		     {"":"clearAlert", "message":"emergency"}]],
 *		   ["if", {"":"listenAlert", "message":"xxx_foo-bar-baz"},
 *		    [{"":"insertRoute", "route":"node_02_00"},
 *		     {"":"log","tag":"insert route: 02_00"},
 *		     {"":"clearAlert", "message":"foo-bar-baz"}]]
 *	       ]},
 *    </pre>
 *    なお、上記は以下のように書いても同じである(Object型関数記述)。
 *    <pre>
 *     	       "rule":[
 *		   {"":"if",
 *		    "condition":{"":"listenAlert",
 *				 "message":"xxx_emergency"},
 *		    "then":[{"":"changeGoal", "goal":"node_09_05"},
 *			    {"":"clearPlannedRoute"},
 *			    {"":"log","tag":"change goal to 09_05"},
 *			    {"":"clearAlert", "message":"emergency"}]}
 *		   {"":"if",
 *		    "condition":{"":"listenAlert",
 *				 "message":"xxx_foo-bar-baz"},
 *		    "then":[{"":"insertRoute", "route":"node_02_00"},
 *			    {"":"log","tag":"insert route: 02_00"},
 *			    {"":"clearAlert", "message":"foo-bar-baz"}]}
 *	       ]
 *    </pre>
 * <h4>MentalMapルール記述</h4>
 *   エージェントが経路を選ぶ際、基本的には物理的最短距離を選ぶが、
 *   その距離を、心理条件(メンタルモード)によって変更することが可能である
 *   ({@link nodagumi.ananPJ.navigation.Formula.NaviFormula NaviFormula})。
 *   その変更された距離の計算方式は、
 *   property file の "mental_map_rules" に記述される
 *   (sample/generatedTown/gridTown00.prop.json 参照)
 *   <pre>
 *	  "mental_map_rules" :
 *	    {
 *	      "major" :
 *	        {"":"if",
 *	    	 "cond":{"":"hasTag","tag":"major"},
 *		 "then":":length",
 *		 "else":{"":"*", "values":[":length", 10.0]}}
 *	      "normal" : ":length"
 *	    }
 *   </pre>
 *   <p>
 *   この計算は、リンクごとに行われる。
 *   現在注目しているリンクについて、 
 *   ":length" はリンクの長さを、
 *   ":width" はリンクの幅を表す。
 *   また、{ "": "hasTag", "tag":Tag } は、そのリンクが Tag に指定された
 *   タグを持っていれば "true" となる。
 *   この他、四則演算 ("+", "-", "*", "/") などの関数も使える。
 *   </p>
 *   <p>
 *   上記の例は、2つのメンタルモード("major" と "normal") の計算方法を
 *   指定している。
 *   ("mental_map_rules" のキーに指定された値の JSON Object の
 *    キーが、メンタルモードに対応する。)
 *   この2つのモードの中で、"normal" の場合には、リンクの長さ ":length" を
 *   そのまま用いて最短距離計算を行う。
 *   "major" メンタルモードの場合には、条件分岐となっていて、
 *   リンクが "major" というタグを持てばそのままの長さ、
 *   タグを持たないリンクについては長さを10倍にして、距離を計算する。
 *   これにより、"major" というメンタルモードをもつエージェントは、
 *   "major" タグのあるリンクを優先して選ぶようになる。
 *   </p>
 *   
 *   <p>(注意1) 
 *      {@link nodagumi.ananPJ.Agents.Think.ThinkFormulaArithmetic#line-461 random} という関数を
 *      用いることもできるが、上記の計算は静的に行われるため、
 *      同じメンタルモードをもつエージェントに対して、同じ値を返す。
 *      random で異なる値を出すためには、地図変更のイベントを起こさないと行けない。
 *   </p>
 *
 *   <p>(注意2) 
 *      メンタルモードの変更は、generation ファイルにおいて、
 *      "agentType"/"rule"/"mentalMode" で初期値を設定する。
 *      また、シミュレーション中は、RationalAgent であれば、
 *        { "":"setParam", "name":"mentalMode", "value": Mode }
 *      で指定できる。
 *      (sample/generatedTown/gridTown00.array2.gen.json 参照)
 *   </p>
 */
package nodagumi.ananPJ.Agents.Think;

