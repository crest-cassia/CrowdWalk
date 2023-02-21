// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Simulator;

import java.io.File;
import java.io.IOException;
import java.lang.Thread;
import java.lang.Runnable;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Formatter;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Link.MapLink.*;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.misc.CrowdWalkPropertiesHandler;
import nodagumi.ananPJ.misc.SetupFileInfo;
import nodagumi.ananPJ.Agents.Factory.AgentFactory;
import nodagumi.ananPJ.Agents.Factory.AgentFactoryByRuby;
import nodagumi.ananPJ.Agents.Factory.AgentFactoryList;
import nodagumi.ananPJ.Agents.Factory.AgentFactoryConfig;
import nodagumi.ananPJ.Scenario.*;
import nodagumi.ananPJ.Simulator.Obstructer.ObstructerBase.TriageLevel ;
import nodagumi.ananPJ.misc.SimTime;
import nodagumi.ananPJ.misc.Place;

import nodagumi.Itk.CsvFormatter;
import nodagumi.Itk.JsonFormatter;
import nodagumi.Itk.Term;
import nodagumi.Itk.*;



//======================================================================
//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
/**
 * Agent の管理を行う.
 * 
 * 以下のパラメータについて、fallback に記載できる。
 * これらのフォールバックは、 fallback の json の "agentHandler" 
 * にオブジェクトとしてまとめられる。
 * つまり、 fallback の中に、
 * <pre>{ ..., 
 *   "agentHandler" : { "slot1" : value1, "slot2" : value2 ...},
 *    ...}
 * </pre>
 * と記載する。
 * <ul>
 *  <li> "zeroSpeedThreshold" : (Double) 
 *       止まっているとみなす速度の閾値。
 *  </li>
 *  <li> "logColumnsOfIndividualPedestrians" : ([String, String, ...]) 
 *       individualPedestriansLog に出力する項目のリスト。
 *       詳細は、{@link #individualPedestriansLoggerFormatter}。
 *  </li>
 *  <li> "tickIntervalForIndividualPedestriansLog" : (Int) 
 *       ログを出力する時間間隔。tickCount の値で記述する。
 *  </li>
 * </ul>
 */
public class AgentHandler {
    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * agent の queue を処理する際に、先頭・末尾のどちらから処理するか
     * を示すフラグ。
     * MapLink の中でのソートでは、エージェントの進んだ距離に対して、
     * 昇順になるようになっている。
     * つまり、進んでいないエージェント（列の末尾にいるエージェント）が
     * queue (agents という変数)の先頭に来る。
     * このフラグは、update, preupdate において、
     * この queue を前から処理するか、後ろから処理するかを指定する。
     * true の場合、後ろから（つまり agent の列として先頭から）処理する。
     * 本来、これはかならず true であるべきだが、
     * 2014年12月までの実装での状態を残すため、
     * フラグで切り替えられるようにしておく。
     */
    // static private boolean usingFrontFirstOrderQueue = false ;
    static private boolean isUsingFrontFirstOrderQueue = true ;

    //============================================================
    //------------------------------------------------------------
    /**
     * switching queue order
     * @param flag : if true, use front_first order.
     *               else, use rear_first order.
     */
    static public void useFrontFirstOrderQueue(boolean flag) {
        isUsingFrontFirstOrderQueue = flag ;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 乱数生成器。
     */
    private Random random = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 所属するSimulator へのリンク。
     */
    private EvacuationSimulator simulator;

    /**
     * 所属するSimulator を取得。
     */
    public EvacuationSimulator getSimulator() {
        return simulator ;
    }

    /**
     * 所属するSimulator の RubyWrapper の呼び出し。
     */
    public Object callRubyWrapper(String methodName, Object... args) {
        return getSimulator().callRubyWrapper(methodName, args) ;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * AgentHandler の fallback
     */
    private Term fallback = null;
    
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェント生成ファイル
     */
    private AgentFactoryList agentFactoryList = null;

    final public AgentFactoryList getAgentFactoryList() {
        return agentFactoryList ;
    }

    /** 名前よりエージェント生成ルール (AgentFactoryConfig) 取得 */
    final public AgentFactoryConfig getAgentFactoryConfigByName(String ruleName)
    {
        return getAgentFactoryList().getAgentFactoryConfigByName(ruleName) ;
    }

    /** 名前より代表的 AgentFactory 取得 */
    final public AgentFactory getFirstAgentFactoryByName(String ruleName) {
        AgentFactoryConfig factoryConfig =
            getAgentFactoryConfigByName(ruleName) ;
        if(factoryConfig == null) {
            return null ;
        } else {
            return factoryConfig.getAgentFactoryList().get(0) ;
        }
    }
    
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 地図。
     */
    private NetworkMap networkMap;

    final public NetworkMap getMap() { return networkMap ; } ;
    final public NetworkMap setMap(NetworkMap _map) {
        networkMap = _map ;
        return networkMap ;
    }

    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * NetworkMapParts の prefix の規定値
     */
    public static String DefaultAgentIdPrefix = "ag" ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェントテーブル。
     * エージェントの id とエージェントを結びつけるもの。
     */
    private UniqIdObjectTable<AgentBase> agentTable =
        new UniqIdObjectTable<AgentBase>(DefaultAgentIdPrefix) ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 生成した全エージェントリスト。
     */
    private ArrayList<AgentBase> generatedAgents =
        new ArrayList<AgentBase>() ;

    /**
     * 避難完了したエージェントリスト。
     */
    private ArrayList<AgentBase> evacuatedAgents =
        new ArrayList<AgentBase>() ;

    /**
     * スタックしたエージェントリスト。
     */
    private ArrayList<AgentBase> stuckAgents =
        new ArrayList<AgentBase>() ;

    /**
     * 今動いているエージェントの集まり
     */
    private HashMap<String, AgentBase> walkingAgentTable =
        new HashMap<String, AgentBase>() ;

    /**
     * 最後のサイクルに避難完了したエージェントリスト。
     */
    private ArrayList<AgentBase> evacuatedAgentsInStep =
        new ArrayList<AgentBase>() ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェントが存在するリンクのリスト
     */
    private TreeSet<MapLink> effectiveLinkSet =
        new TreeSet<MapLink>() ;

    /**
     * エージェントが存在するリンクに接続するノードのリスト
     */
    private TreeSet<MapNode> effectiveNodeSet =
        new TreeSet<MapNode>() ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 平均速度。
     */
    private double averageSpeed = 0.0;

    /**
     * 生成されるはずの全エージェント数。
     * 現状、表示でのみ使用。[2017-4-22 I.Noda]
     */
    private int maxAgentCount = 0;

    /**
     */
    private boolean isAllAgentSpeedZeroBreak = false;

    /**
     */
    private boolean isAllAgentSpeedZero = false;

    /**
     * エージェントのスピードをゼロと見做す上限。
     */
    static private double Fallback_zeroSpeedThreshold = 0.0 ;

    /**
     * エージェントのスピードをゼロと見做す上限。
     */
    private double zeroSpeedThreshold = Fallback_zeroSpeedThreshold ;

    /**
     * CurrentLink が該当するリンクか判別するためのリンクタグ(individualPedestriansLog用)
     */
    private String searchTargetLinkTag = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    // ログ出力定義関連
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //agentMovementHistoryLogger 関係  (obsolute)
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * agentMovementHistoryLogger.
     * output log when each agent evacuated.
     */
    private Logger agentMovementHistoryLogger = null;

    /**
     * チェックポイントリスト
     */
    private List<String> checkpoints = null;

    /**
     * チェックポイントの通過時刻
     */
    private HashMap<AgentBase, HashMap<String, SimTime>> checkpointPassingTimes = null;

    /**
     * agentMovementHistoryLogger の CSV フォーマットは以下の通り。
     * <pre>
     * 各行のカラムの順：
     *		"GenerationFileの情報"
     *		"エージェントID"
     *		"発生時刻1"
     *		"発生時刻2"
     *		"到着時刻1"
     *		"到着時刻2"
     *		"移動時間1"
     *		"移動時間2"
     * </pre>
     */
    public static CsvFormatter<AgentBase> agentMovementHistoryLoggerFormatter =
        new CsvFormatter<AgentBase>() ; //javadoc で説明生成するため、private を public にする。
    static {
        CsvFormatter<AgentBase> formatter = agentMovementHistoryLoggerFormatter ;
        formatter
            .addColumn(formatter.new Column("GenerationFileの情報") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return agent.getConfigLine().replaceAll(",", " ") ;}})
            .addColumn(formatter.new Column("エージェントID") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return agent.ID ;}})
            .addColumn(formatter.new Column("発生時刻1") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        AgentHandler handler = (AgentHandler)agentHandlerObj;
                        return agent.generatedTime.getAbsoluteTimeString() ;}})
            .addColumn(formatter.new Column("発生時刻2") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return "" + (int)agent.generatedTime.getRelativeTime() ;}})
            .addColumn(formatter.new Column("到着時刻1") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        SimTime currentTime = (SimTime)timeObj ;
                        AgentHandler handler = (AgentHandler)agentHandlerObj;
                        return currentTime.getAbsoluteTimeString();}})
            .addColumn(formatter.new Column("到着時刻2") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        SimTime currentTime = (SimTime)timeObj ;
                        AgentHandler handler = (AgentHandler)agentHandlerObj;
                        return "" + (int)currentTime.getRelativeTime();}})
            .addColumn(formatter.new Column("移動時間1") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        SimTime currentTime = (SimTime)timeObj ;
                        AgentHandler handler = (AgentHandler)agentHandlerObj;
                        return
                            Itk.formatSecTime((int)currentTime.calcDifferenceFrom(agent.generatedTime)) ;}})
            .addColumn(formatter.new Column("移動時間2") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        SimTime currentTime = (SimTime)timeObj ;
                        AgentHandler handler = (AgentHandler)agentHandlerObj;
                        return "" + (int)(currentTime.calcDifferenceFrom(agent.generatedTime)) ;}})
            ;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //agentTrailLogger 関係。
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * agentTrailLogger.
     * Agent が evacuate した時、その軌跡を出力するログ。
     * JSON 形式で出力する。
     */
    public Logger agentTrailLogger = null ;

    /**
     * agentTrailLogger の JSON フォーマットのスタイル
     */
    public JsonFormatter.OverallStyle agentTrailLogStyle =
        JsonFormatter.OverallStyle.RecordPerLine ;

    /**
     * agentTrailLogger の JSON フォーマット。
     * <pre>
     * { "agentId": _AgentID_,
     *   "generatedBy": _RuleName_,
     *   "generatedAbsTime": _TimeStr_,
     *   "generatedRelTime": _TimeInSec_,
     *   "evacuatedAbsTime": _TimeStr_,
     *   "evacuatedRelTime": _TimeInSec_,
     *   "travelTime": _TimeInSec_,
     *   "tags": [ _tag_, _tag_, ... ],
     *   "trail": [ {"placeId": _PlaceID_, "time": _TimeInSec_} * ]
     * }
     * </pre>
     * 指定の方法は、properties file において、
     * <pre>
     *  "agent_trail_log" :
     *    {
     *      "file":",Log/agentTrail.log",
     *      "members": ["agentId", "evacuatedRelTime"]
     *    },
     * </pre>
     * という形で指定する。
     *
     * これ以外に、CrowdWalkWrapper クラスにおいて、
     * 新規の項目を追加できる。
     * {@link #addMemberToAgentTrailLogFormatterForRuby} 参照。
     */
    public static JsonFormatter<AgentBase> agentTrailLogFormatter =
        new JsonFormatter<AgentBase>() ;
    static {
        JsonFormatter<AgentBase> formatter = agentTrailLogFormatter ;
        formatter
            .addMember(formatter.new Member("agentId") {
                    public Object value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return agent.ID ;}})
            .addMember(formatter.new Member("generatedBy") {
                    public Object value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return agent.getFactory().getRuleName() ; }})
            .addMember(formatter.new Member("generatedAbsTime") {
                    public Object value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return agent.generatedTime.getAbsoluteTimeString() ;}})
            .addMember(formatter.new Member("generatedRelTime") {
                    public Object value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return Integer.valueOf((int)agent.generatedTime
                                           .getRelativeTime()) ; }})
            .addMember(formatter.new Member("evacuatedAbsTime") {
                    public Object value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        SimTime currentTime = (SimTime)timeObj ;
                        return currentTime.getAbsoluteTimeString();}})
            .addMember(formatter.new Member("evacuatedRelTime") {
                    public Object value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        SimTime currentTime = (SimTime)timeObj ;
                        return Integer.valueOf((int)currentTime
                                           .getRelativeTime()) ; }})
            .addMember(formatter.new Member("travelTime") {
                    public Object value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        SimTime currentTime = (SimTime)timeObj ;
                        return Integer.valueOf((int)currentTime
                                    .calcDifferenceFrom(agent
                                                        .generatedTime)) ;}})
            .addMember(formatter.new Member("tags"){
                    public Object value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return agent.getTags() ; }})
            .addMember(formatter.new Member("trail"){
                    public Object value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return agent.getTrail().getJsonObject() ; }})
            ;
    }
    
    //--------------------------------------------------
    /**
     * agentTrailLogFormatter 用の member を作成する。
     * ruby (CrowdWalkWrapper) から呼び出し用。
     * ruby 側では、CrowdWalkWrapperの継承クラスにおいて、
     * setupSimulationLoggers() というメソッドを再定義する。その中で、
     * <pre>
     *   addMemberToAgentTrailLogFormatter("foo") {|agent, currentTime, handler|
     *      ## ここで、ログに吐き出すデータ（文字列・数値・Array・Hash)を生成する
     *      ## Ruby のプログラムを書く。
     *      ## 以下は、その例
     *      [1, 2, 3, "hogehoge",
     *        {"a" {@code =>} 10,
     *         "b" {@code =>} nil}] ;
     *   }
     * </pre>
     * というものを好きなだけ並べる。
     * この例では、"foo" というスロットに、
     * 生成したデータ ([1,2,3,...]) が出力される。
     * なお、このメソッドの name に、"foo" が格納されて呼び出される。
     * 
     * sample/generatedTown/SampleWrapper.rb 参照。
     */
    public void addMemberToAgentTrailLogFormatterForRuby(String name) {
        agentTrailLogFormatter
            .addMember(agentTrailLogFormatter.new Member(name){
                    public Object value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        AgentHandler handler = (AgentHandler)agentHandlerObj ;
                        return
                            handler
                            .callRubyWrapper("callbackAgentTrailLogMember",
                                             name,
                                             agent, timeObj, agentHandlerObj) ;
                    }}) ;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * agentTrailLogger の中の、trail の aux 要素の定義。
     * 設定方法は、properties file において、
     * <pre>
     *  "agent_trail_log" :
     *    {
     *      ...
     *      "trail_aux": ["speed", "advancingDist"]
     *    },
     * </pre>
     * という形で指定する。
     * 可能な項目は、
     * <pre>
     *   "speed" : Node を超えた時のスピード。
     *   "advancingDist" : place における advancingDistance。
     * </pre>
     */
    public static JsonFormatter<AgentBase> agentTrailLogTrailAuxFormatter =
        new JsonFormatter<AgentBase>() ;
    static {
        JsonFormatter<AgentBase> formatter = agentTrailLogTrailAuxFormatter ;
        formatter
            .registerMember(formatter.new Member("speed") {
                    public Object value(AgentBase agent, Object timeObj,
                                        Object placeObj, Object nextLinkObj) {
                        return Double.valueOf(agent.getSpeed()) ;
                    }})
            .registerMember(formatter.new Member("advancingDist") {
                    public Object value(AgentBase agent, Object timeObj,
                                        Object placeObj, Object nextLinkObj) {
                        Place place = (Place)placeObj ;
                        return Double.valueOf(place.getAdvancingDistance()) ;
                    }})
            ;
    }
                                        
    /**
     * agentTrailLogger において、trail の auxのフォーマットが
     * 指定されているかどうか？
     */
    public boolean agentTrailLogHasTrailAux() {
        return agentTrailLogTrailAuxFormatter.getMemberList().size() > 0 ;
    }

    /**
     * agentTrailLogTrailAuxFormatter の取得。
     */
    public JsonFormatter<AgentBase> getAgentTrailLogTrailAuxFormatter() {
        return agentTrailLogTrailAuxFormatter ;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    // individualPedestriansLog 関係。
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * individualPedestriansLog の出力先 dir
     */
    private String individualPedestriansLogDir = null;

    /**
     * individualPedestriansLog の出力対象カラム
     */
    private List<String> logColumnsOfIndividualPedestrians = new ArrayList<String>();

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * individualPedestriansLogger
     */
    private Logger individualPedestriansLogger = null;

    /**
     * individualPedestriansLogger のCSVフォーマットは以下の通り。
     * <pre>各行のカラムの順：
     *		"pedestrianID"
     *		"current_position_in_model_x"
     *		"current_position_in_model_y"
     *		"current_position_in_model_z"
     *		"current_position_for_drawing_x"
     *		"current_position_for_drawing_y"
     *		"current_position_for_drawing_z"
     *		"current_acceleration"
     *		"current_velocity"
     *		"current_linkID"
     *		"current_nodeID_of_forward_movement"
     *		"current_nodeID_of_backward_movement"
     *		"current_distance_from_node_of_forward_movement"
     *		"current_moving_direction"
     *		"generated_time"
     *		"current_traveling_period"
     *		"current_exposure"
     *		"amount_exposure"
     *		"current_status_by_exposure"
     *		"next_assigned_passage_node"
     * ※ 以下のものはデフォルトでは出力されない。（以下の方法で指定すれば出力）
     *          "waiting"
     *          "in_search_target_link"
     *          "current_time"
     * </pre>
     * 標準の出力カラムはリソースデータ "fallbackParameters.json" の
     * agentHandler/logColumnsOfIndividualPedestrians で定義されている。
     * プロパティファイルに指定する Fallback file で再定義する事により出力カラムが変更できる。
     */
    public static CsvFormatter<AgentBase> individualPedestriansLoggerFormatter =
        new CsvFormatter<AgentBase>() ; //javadoc で説明生成するため、private を public にする。
    static {
        CsvFormatter<AgentBase> formatter = individualPedestriansLoggerFormatter ;
        formatter
            .addColumn(formatter.new Column("pedestrianID") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return agent.ID ;}})
            .addColumn(formatter.new Column("current_position_in_model_x") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return (agent.isEvacuated() ?
                                "" + 0.0 :
                                "" + (agent.getPosition().getX())) ;}})
            .addColumn(formatter.new Column("current_position_in_model_y") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return (agent.isEvacuated() ?
                                "" + 0.0 :
                                "" + (agent.getPosition().getY())) ;}})
            .addColumn(formatter.new Column("current_position_in_model_z") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return (agent.isEvacuated() ?
                                "" + 0.0 :
                                "" + (agent.getHeight())) ;}})
            .addColumn(formatter.new Column("current_position_for_drawing_x") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return (agent.isEvacuated() ?
                                "" + 0.0 :
                                "" + (agent.getPosition().getX() 
                                      + agent.getSwing().getX())) ;}})
            .addColumn(formatter.new Column("current_position_for_drawing_y") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return (agent.isEvacuated() ?
                                "" + 0.0 :
                                "" + (agent.getPosition().getY() 
                                      + agent.getSwing().getY())) ;}})
            .addColumn(formatter.new Column("current_position_for_drawing_z") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return (agent.isEvacuated() ?
                                "" + 0.0 :
                                "" + ((agent.getHeight() /
                                       ((MapPartGroup)agent.getCurrentLink().getParent()).getScale())
                                      + agent.getSwing().getZ())) ;}})
            .addColumn(formatter.new Column("current_acceleration") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return (agent.isEvacuated() ?
                                "" + 0.0 :
                                "" + (agent.getAcceleration())) ;}})
            .addColumn(formatter.new Column("current_velocity") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return (agent.isEvacuated() ?
                                "" + 0.0 :
                                "" + (agent.getSpeed())) ;}})
            .addColumn(formatter.new Column("current_linkID") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return (agent.isEvacuated() ?
                                "" + -1 :
                                agent.getCurrentLink().ID ) ;}})
            .addColumn(formatter.new Column("current_nodeID_of_forward_movement") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return (agent.isEvacuated() ?
                                "" + -1 :
                                agent.getNextNode().ID ) ;}})
            .addColumn(formatter.new Column("current_nodeID_of_backward_movement") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return (agent.isEvacuated() ?
                                agent.getLastNode().ID :
                                agent.getPrevNode().ID ) ;}})
            .addColumn(formatter.new Column("current_distance_from_node_of_forward_movement") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return (agent.isEvacuated() ?
                                "" + 0.0 :
                                "" + (agent.getRemainingDistance())) ;}})
            .addColumn(formatter.new Column("current_moving_direction") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return (agent.isEvacuated() ?
                                "" + 0 :
                                "" + ((int)agent.getDirection().value())) ;}})
            .addColumn(formatter.new Column("generated_time") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return "" + ((int)agent.generatedTime.getRelativeTime()) ;}})
            .addColumn(formatter.new Column("current_traveling_period") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        SimTime currentTime = (SimTime)timeObj ;
                        return "" + ((int)currentTime.getRelativeTime()) ;}})
            .addColumn(formatter.new Column("current_exposure") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        SimTime currentTime = (SimTime)timeObj ;
                        return "" + (agent.obstructer.currentValueForLog()) ;}})
            .addColumn(formatter.new Column("amount_exposure") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        SimTime currentTime = (SimTime)timeObj ;
                        return "" + (agent.obstructer.accumulatedValueForLog()) ;}})
            .addColumn(formatter.new Column("current_status_by_exposure") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        SimTime currentTime = (SimTime)timeObj ;
                        AgentHandler handler = (AgentHandler)agentHandlerObj ;
                        return agent.getTriageName();}})
            .addColumn(formatter.new Column("next_assigned_passage_node") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        SimTime currentTime = (SimTime)timeObj ;
                        AgentHandler handler = (AgentHandler)agentHandlerObj ;
                        return (agent.isEvacuated() ?
                                "" :
                                agent.getNextCandidateString()) ;}})
            .addColumn(formatter.new Column("waiting") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return "" + agent.isWaiting() ;}})
            .addColumn(formatter.new Column("in_search_target_link") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        AgentHandler handler = (AgentHandler)agentHandlerObj ;
                        return ((handler.searchTargetLinkTag == null || agent.isEvacuated()) ?
                                "false" :
                                "" + agent.getCurrentLink().hasTag(handler.searchTargetLinkTag)) ;}})
            .addColumn(formatter.new Column("current_time") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        SimTime currentTime = (SimTime)timeObj ;
                        return currentTime.getAbsoluteTimeString();}})
            .addColumn(formatter.new Column("pedestrian_tag") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return agent.getTags().toString();}})
            ;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * default of time interval in cycle of individualPedestriansLogger
     */
    static private int Fallback_tickIntervalForIndividualPedestriansLog = 1 ;

    /**
     * time interval in cycle of individualPedestriansLogger
     */
    private int tickIntervalForIndividualPedestriansLog
        = Fallback_tickIntervalForIndividualPedestriansLog ;

    /**
     * Offset of individual pedestrian log
     */
    private int offsetOfIndividualPedestriansLog = 0;

    /**
     * individualPedestriansLogger の出力対象エージェントを示すタグ
     */
    private ArrayList<String> tagsForIndividualPedestriansLog = new ArrayList();

    /**
     * individualPedestriansLogger の出力対象エージェントを示すタグの正規表現
     */
    private ArrayList<Pattern> regexpsForIndividualPedestriansLog = new ArrayList();

    /**
     * 対象エージェントを除外する場合は true
     */
    private boolean excludeAgentsInIndividualPedestriansLog = false;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    // EvacuatedAgentsLogger 関係。
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェントの脱出時のログ。
     * ゴールノードごとの脱出したエージェント数を各時刻毎に出力する。
     * CSV の時のフォーマットは、
     * <pre>
     *     先頭行：各目的地の名前（タグ）のリスト
     *     2行目以降：最初のサイクルより、各時刻毎の出口の人数を、各欄に出力。
     * </pre>
     */
    public Logger evacuatedAgentsLogger = null; // document へ出力のため、public
    
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * CSV フォーマットの際のフォーマッター。
     */
    private CsvFormatter<HashMap<MapNode, Integer>>
        evacuatedAgentsLoggerFormatter =
        new CsvFormatter<HashMap<MapNode, Integer>>() ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * ゴールノードごとの脱出したエージェント数(カウントアップする)
     */
    private HashMap<MapNode, Integer> evacuatedAgentsCountByExit =
        new LinkedHashMap<MapNode, Integer>();

    //------------------------------------------------------------
    /**
     * コンストラクタ。
     * @param _simulator : 親になるシミュレータ。
     */
    public AgentHandler(EvacuationSimulator _simulator) {
        simulator = _simulator;

        // simulatorから必須パラメータ取り出し。
        random = simulator.getRandom();
        setMap(simulator.getMap()) ;

        // fallback の取得
        Term wholeFallback = simulator.getFallbackParameters();
        fallback =
            SetupFileInfo
            .filterFallbackTerm(wholeFallback, "agentHandler") ;

        // zero speed threshold
        zeroSpeedThreshold =
            SetupFileInfo
            .fetchFallbackDouble(fallback,
                                 "zeroSpeedThreshold", zeroSpeedThreshold) ;
        // search target link tag
        searchTargetLinkTag =
            SetupFileInfo
            .fetchFallbackString(fallback,
                                 "searchTargetLinkTag", searchTargetLinkTag);

        // Logger setup
        setupSimulationLoggers() ;
        
        // ファイル類の読み込み
        loadAgentGenerationFile(simulator.getSetupFileInfo().getGenerationFile()) ;
    }

    //------------------------------------------------------------
    // 読み込み関連
    //------------------------------------------------------------
    /**
     * エージェント生成ファイル。
     */
    private void loadAgentGenerationFile(String generationFile) {
        try {
            /* [I.Noda] generation file の読み込みはここ */
             agentFactoryList =
                 new AgentFactoryList(generationFile,
                                      getMap(),
                                      simulator.getFallbackParameters(),
                                      hasDisplay(),
                                      simulator.getLinerGenerateAgentRatio(),
                                      random);
        } catch(Exception ex) {
            Itk.logError("Illegal AgentGenerationFile",
                         simulator.getSetupFileInfo().getGenerationFile(),
                         ex.getMessage());
            Itk.quitWithStackTrace(ex) ;
        }
        if (agentFactoryList != null) {
            for (AgentFactory factory : agentFactoryList) {
                maxAgentCount += factory.getMaxGeneration();
            }
        }
    }

    //------------------------------------------------------------
    // シミュレーション関連
    //------------------------------------------------------------
    /**
     * シミュレーション開始準備
     */
    public void prepareForSimulation() {
        for (AgentBase agent : getAllAgentCollection()) {
            agent.prepareForSimulation() ;
        }
        // 初回は全リンクを対象とする
        clearEffectiveLinkSet() ;
        for(MapLink link : simulator.getLinks()) {
            addEffectiveLink(link) ;
        }
        // [2016.02.22 I.Noda]
        // ノードによる制御は、しばらく必要ないので、以下はコメントアウト。
        //
        // 初回は全ノードを対象とする
        /*
        clearEffectiveNodeSet() ;
        for(MapNode node : simulator.getNodes()) {
            addEffectiveNode(node) ;
        }
        */
        // 全ノードのリンクを整列させ、インデックスをつける。
        for(MapNode node : simulator.getNodes()) {
            node.sortLinkTableByAngle() ;
        }
    }

    //------------------------------------------------------------
    /**
     * map に対する preprocess 処理
     */
    private void preUpdateNetworkMap(SimTime currentTime) {
        synchronized (simulator) {
            ArrayList<MapLink> emptyLinkList = new ArrayList<MapLink>() ;
            for(MapLink link : getEffectiveLinkSet()) {
                if(link.agents.isEmpty()) {
                    emptyLinkList.add(link) ;
                } else {
                    link.preUpdate(currentTime);
                }
            }
            for(MapLink link : emptyLinkList) {
                removeEffectiveLink(link) ;
            }
        }
    }

    //------------------------------------------------------------
    /**
     * エージェントの preprocess 処理
     */
    private void preUpdateAgents(SimTime currentTime) {
        if (true) {
            synchronized (simulator) {
                for (MapLink link : getEffectiveLinkSet()) {
                    preUpdateAgentsOnLink(link, currentTime) ;
                }
            }
        } else {
            /* [2017.07.04 I.Noda]
             * 試しに、マルチスレッドでの preUpdate を実装。
             * 残念ながら、うまく動作しない。
             * java.util.ConcurrentModificationException が発生する。
             * WalkAgent.chooseNextLinkBody(WalkAgent.java:1161)。
             * ここでの原因は、おそらく、MapNode.getUsableLinkTable()
             * で排他処理していないせい。
             * さらに、大して速くならない。8並列(paraN=8)としても、
             * 20%程度しか速くならない。メモリを増やしても(-Xmx32g)効果なし。
             * なので、ここのマルチスレッド化はしばらくあきらめ。
             */
            synchronized (simulator) {
                Iterator<MapLink> linkIterator =
                    getEffectiveLinkSet().iterator() ;
                ArrayList<Thread> threadList = new ArrayList<Thread>() ;
                int paraN = 8 ;
                for(int i = 0 ; i < paraN ; i++) {
                    Runnable worker = new Runnable(){
                            public void run() {
                                MapLink link = null;
                                while(true) {
                                    synchronized(linkIterator) {
                                        if(!linkIterator.hasNext()) break ;
                                        link = linkIterator.next() ;
                                    }
                                    preUpdateAgentsOnLink(link, currentTime) ;
                                }
                            }
                        } ;
                    Thread thread = new Thread(worker) ;
                    threadList.add(thread) ;
                    thread.start() ;
                }
                try {
                    for(Thread thread : threadList) {
                        thread.join() ;
                    }
                } catch (InterruptedException ex) {
                    Itk.quitWithStackTrace(ex) ;
                }
            }
        }
    }

    //------------------------------------------------------------
    /**
     * エージェントの preprocess 処理 (１つのリンク)
     */
    private void preUpdateAgentsOnLink(MapLink link, SimTime currentTime) {
        ArrayList<AgentBase> forwardAgents = link.getLane(Direction.Forward);
        for(int i = 0 ; i < forwardAgents.size() ; i++) {
            AgentBase agent =
                (isUsingFrontFirstOrderQueue ?
                 forwardAgents.get(forwardAgents.size() - i - 1) :
                 forwardAgents.get(i)) ;
            
            agent.preUpdate(currentTime);
        }
        ArrayList<AgentBase> backwardAgents = link.getLane(Direction.Backward);
        for (int i = backwardAgents.size() - 1; i >= 0; --i) {
            AgentBase agent = backwardAgents.get(i);
            agent.preUpdate(currentTime);
        }
    }
        
    //------------------------------------------------------------
    /**
     * シミュレーションサイクル。
     * @param currentTime : シミュレーションの現在時刻。
     */
    public void update(SimTime currentTime) {
        ArrayList<AgentBase> generatedAgentsInStep
            = generateAgentsAndSetup(currentTime) ;
        
        getSimulator().tryDelayedSetSeed(SeedRandEvent.Timing.afterGeneration) ;
        
        preUpdateNetworkMap(currentTime);
        preUpdateAgents(currentTime);
        updateAgents(currentTime);
        updatePollution();
        updateLinks(currentTime);

        updateAgentViews(currentTime);

        /* the following must be here, as direction etc. are
         * calculated in the methods call above, such as updateAgents.
         * [2015.05.25 I.Noda] おそらく表示のために、ここにないといけない。
         */
        registerGeneratedAgentsInStep(generatedAgentsInStep) ;
        updateEffectiveLinkSetAndRemoveEvacuatedAgents() ;
        notifyMovedAgents() ;
    }

    //------------------------------------------------------------
    /**
     * generation 定義による新規エージェントの生成
     */
    private ArrayList<AgentBase> generateAgentsAndSetup(SimTime currentTime) {
        ArrayList<AgentBase> generatedAgentsInStep = new ArrayList<AgentBase>();

        // agentFactoryList による生成。
        if (agentFactoryList != null) {
            for (AgentFactory factory : agentFactoryList) {
                factory.tryUpdateAndGenerate(simulator, currentTime,
                                             generatedAgentsInStep) ;
            }
        }

        // Agent の初期設定。
        if (! generatedAgentsInStep.isEmpty()) {
            generatedAgents.addAll(generatedAgentsInStep);
            for (AgentBase agent : generatedAgentsInStep) {
                /* [2015.05.25 I.Noda] 
                 * registerAgent から切り離して、Agent に ID をふる。
                 */
                assignUniqIdForAgent(agent) ;
                addWalkingAgent(agent) ;
                if(!agent.isEvacuated())
                    addEffectiveLink(agent.getCurrentLink()) ;
            }
        }
        return generatedAgentsInStep ;
    }

    //------------------------------------------------------------
    /**
     * 新規エージェントの登録
     */
    private void registerGeneratedAgentsInStep(ArrayList<AgentBase>  agentList) {
        for (AgentBase agent : agentList) {
            simulator.registerAgent(agent);
        }
    }

    //------------------------------------------------------------
    /**
     * エージェントが存在するリンクのリストを更新。
     * さらに、
     * evacuate したエージェントを、walkingAgentTable から除く。
     */
    private void updateEffectiveLinkSetAndRemoveEvacuatedAgents() {
        // エージェントが存在するリンクのリストを更新
        /** [2015.07.05 I.Noda]
         * effectiveLinkSet を、TreeSet としたので、
         * いちいちクリアする必要はない。
         * エージェントがいなくなったリンクは、
         * preUpdateNetworkMap() で削除される。
         */
        //clearEffectiveLinkSet() ;
        evacuatedAgentsInStep.clear();
        for (AgentBase agent : getWalkingAgentCollection()) {
            if(agent.isEvacuated()) {
                evacuatedAgentsInStep.add(agent) ;
                continue ;
            } 
            addEffectiveLink(agent.getCurrentLink()) ;
        }
        for(AgentBase agent : evacuatedAgentsInStep) {
            removeWalkingAgent(agent) ;
        }
    }

    //------------------------------------------------------------
    /**
     * エージェント位置がアップデートされたことを nofity
     */
    private void notifyMovedAgents() {
        if(!hasDisplay()) return ;

        // 位置が変化したエージェントを通知する
        for (AgentBase agent : getWalkingAgentCollection()) {
            if (agent.isEvacuated()) {
                continue;
            }
            boolean agentMoved = agent.updateLastPosition();
            boolean swingChanged = agent.updateLastSwing();
            if (agentMoved || swingChanged) {
                getMap().getNotifier().agentMoved(agent);
            }
        }
    }

    //------------------------------------------------------------
    /**
     * エージェントの update 処理
     */
    private void updateAgents(SimTime currentTime) {
        int count = 0;
        double speedTotal = 0.0;
        isAllAgentSpeedZero = true;

        for(AgentBase agent : getWalkingAgentCollection()) {
            if (!agent.isEvacuated()) {
                agent.update(currentTime);
                if (agent.isEvacuated()) {// この回に evacuate した場合。
                    updateNewlyEvacuatedAgent(agent, currentTime) ;
                } else { // まだ歩いている場合。
                    ++count;
                    speedTotal += agent.getSpeed();
                    if (agent.isWaiting()) {
                        // WAIT 中で停止しているエージェントは all_agent_speed_zero_break の対象外とするため
                        isAllAgentSpeedZero = false;
                    } else {
                        isAllAgentSpeedZero &= (agent.getSpeed() <= zeroSpeedThreshold) ;
                    }
                    if (checkpointPassingTimes != null) {
                        savePassingTime(agent, currentTime);
                    }
                }
            }
        }
        averageSpeed = speedTotal / count;

        if (evacuatedAgentsLogger != null) {
            evacuatedAgentsLoggerFormatter.outputValueToLoggerInfo(
                    evacuatedAgentsLogger, evacuatedAgentsCountByExit);
        }
    }

    //------------------------------------------------------------
    /**
     * このサイクルで evacuateしたエージェントの処理
     */
    private void updateNewlyEvacuatedAgent(AgentBase agent,
                                           SimTime currentTime)
    {
        evacuatedAgents.add(agent);
        if (agent.isStuck()) {
            stuckAgents.add(agent);
        }

        // ログ出力。
        logAgentMovementHistory(agent, currentTime) ;
        logAgentTrail(agent, currentTime) ;

        // 避難数カウント（最終ログ用）
        countEvacuatedAgentsForLogger(agent, currentTime) ;

    }

    //------------------------------------------------------------
    /**
     * エージェントの見え計算
     *
     * ※individual_pedestrians ログは swing 値を使用しているためここで出力する
     */
    private void updateAgentViews(SimTime currentTime) {
        boolean isLogCycle = isLogIndividualPedestriansCycle(currentTime) ;
        
        if (hasDisplay() || isLogCycle) {
            for (AgentBase agent : getWalkingAgentCollection()){
                if (! agent.isEvacuated()) {
                    // swing 値の計算
                    agent.updateViews();
                }
                if(isLogCycle) {
                    logIndividualPedestrians(agent, currentTime) ;
                }
            }
        }
    }

    //------------------------------------------------------------
    /**
     * Pollution のアップデート
     */
    private void updatePollution() {
        /* pollution */
        for (final AgentBase agent : getWalkingAgentCollection()) {
            if (!agent.isEvacuated()) {
                // (do nothing)
                // evacuatedAgentsCount++;
            } else {
                double damage = agent.obstructer.accumulatedValueForLog() ;
            }
        }
    }

    //------------------------------------------------------------
    /**
     * リンクの update 処理。
     *
     * リンク上の agents のソートと各レーンへの振り分け。
     * swing 値を計算する場合は前処理として必要。
     */
    private void updateLinks(SimTime currentTime) {
        if (hasDisplay() || individualPedestriansLogger != null) {
            synchronized (simulator) {
                for (MapLink link : simulator.getLinks()) {
                    link.update(currentTime);
                }
            }
        }
    }

    //------------------------------------------------------------
    // シミュレーション周辺
    //------------------------------------------------------------
    /**
     * エージェントを追加。
     * @param agent : 追加するエージェント。ID は自動生成。
     * @return agent がそのまま変える。
     */
    public AgentBase assignUniqIdForAgent(AgentBase agent) {
        String id = agentTable.putWithUniqId(agent) ;
        agent.ID = id;
        return agent ;
    }

    //------------------------------------------------------------
    /**
     * 終了チェック。
     * 終了条件は以下の通り。
     * * isAllAgentSpeedZeroBreak が true の場合：
     *     * 全員の速度が 0 なら終了。
     * * isAllAgentSpeedZeroBreak が false の場合：
     *     * agent がだれか終わっていないなら、未終了。
     *     * agentFactory のどれかが enable なら、未終了。
     * @return 終了判定。
     */
    public boolean isFinished() {
        if (isAllAgentSpeedZeroBreak) {
            for (AgentFactory factory : agentFactoryList) {
                if (factory.isEnabled()) return false;
            }
            boolean existNotFinished = false;
            for (final AgentBase agent : getWalkingAgentCollection()) {
                if (!agent.finished()) {
                    existNotFinished = true;
                    break;
                }
            }
            if (existNotFinished) {
                if (isAllAgentSpeedZero) {
                    Itk.logInfo("finished", "all agents speed zero");
                    return true;
                }
                return false;
            }
        } else {
            for (final AgentBase agent : getWalkingAgentCollection()) {
                if (!agent.finished()) return false;
            }
            for (AgentFactory factory : agentFactoryList) {
                if (factory.isEnabled()) return false;
            }
        }
        Itk.logInfo("finished","no more agents to generate");
        return true;
    }

    //------------------------------------------------------------
    /**
     * すべてのゴールタグを集める。
     * @return ゴールタグのリスト。
     */
    public ArrayList<String> getAllGoalTags() {
        ArrayList<String> all_goal_tags = new ArrayList<String>();

        for (AgentFactory factory : agentFactoryList) {
            Term goal_tag = factory.getGoal();
            if (goal_tag != null &&
                !all_goal_tags.contains(goal_tag.getString())) {
                all_goal_tags.add(goal_tag.getString());
            }
            for (Term mid_goal : factory.getNakedPlannedRoute()) {
                if (!all_goal_tags.contains(mid_goal.getString())) {
                    all_goal_tags.add(mid_goal.getString());
                }
            }
        }
        return all_goal_tags;
    }

    //------------------------------------------------------------
    /**
     * mid_goal を含まないすべてのゴールタグを集める。
     * @return ゴールタグのリスト。
     */
    public ArrayList<String> getGoalTags() {
        ArrayList<String> goal_tags = new ArrayList<String>();

        for (AgentFactory factory : agentFactoryList) {
            Term goal_tag = factory.getGoal();
            if (goal_tag != null &&
                !goal_tags.contains(goal_tag.getString())) {
                goal_tags.add(goal_tag.getString());
            }
        }
        return goal_tags;
    }

    //------------------------------------------------------------
    // アクセス関連 access
    //------------------------------------------------------------
    /**
     * 画面を持つかどうか。
     * @return 画面表示モードであれば true。
     */
    public boolean hasDisplay() {
        return simulator.hasDisplay() ;
    }

    //------------------------------------------------------------
    /**
     * 乱数シード設定
     * @param _random : セットする乱数発生器。
     */
    public void setRandom(Random _random) {
        random = _random;
        if (agentFactoryList != null)
            agentFactoryList.setRandom(_random);
    }

    //------------------------------------------------------------
    /**
     * エージェント発生頻度の割合を設定。（線形）
     * @param _ratio : 設定する割合。
     */
    public void setLinerGenerateAgentRatio(double _ratio) {
        agentFactoryList.setLinerGenerateAgentRatio(_ratio);
    }

    //------------------------------------------------------------
    /**
     * 全エージェントが速度ゼロになると終了するかのフラグ。
     * @return フラグの値。
     */
    public boolean getIsAllAgentSpeedZeroBreak() {
        return isAllAgentSpeedZeroBreak;
    }

    //------------------------------------------------------------
    /**
     * 全エージェントが速度ゼロになると終了するかのフラグのセット。
     * @param _isAllAgentSpeedZeroBreak : フラグの値。
     */
    public void setIsAllAgentSpeedZeroBreak(boolean _isAllAgentSpeedZeroBreak)
    {
        this.isAllAgentSpeedZeroBreak = _isAllAgentSpeedZeroBreak;
    }

    //------------------------------------------------------------
    /**
     * 歩いているエージェントのCollectionを返す。
     * @return エージェントのCollection。
     */
    public Collection<AgentBase> getWalkingAgentCollection() {
        return walkingAgentTable.values() ;
    }

    //------------------------------------------------------------
    /**
     * 生成されるはずの全エージェント数。
     * 表示でのみ使用。[2017-4-22 I.Noda]
     * @return エージェント数。
     */
    public int getMaxAgentCount() {
        return maxAgentCount;
    }

    //------------------------------------------------------------
    /**
     * 平均速度。
     * @return 全エージェントの平均速度。（既に求めてある値。）
     */
    public double getAverageSpeed() {
        return averageSpeed;
    }

    //------------------------------------------------------------
    /**
     * 歩いているエージェントの数
     * @return エージェントの数
     */
    public int numOfWalkingAgents() {
        return walkingAgentTable.size() ;
    }

    //------------------------------------------------------------
    /**
     * 避難完了したエージェントの数
     * @return エージェントの数
     */
    public int numOfEvacuatedAgents() {
        return evacuatedAgents.size() ;
    }

    //------------------------------------------------------------
    /**
     * スタック中のエージェントの数
     * @return エージェントの数
     */
    public int numOfStuckAgents() {
        return stuckAgents.size() ;
    }

    //------------------------------------------------------------
    /**
     * 生成されたすべてのエージェントの Collection を返す。
     * @return 全エージェントのリスト
     */
    public final Collection<AgentBase> getAllAgentCollection() {
        return agentTable.values() ;
    }

    //------------------------------------------------------------
    /**
     * 生成されたすべてのエージェントの数
     * @return 全エージェントの数
     */
    public int numOfAllAgents() {
        return agentTable.size() ;
    }


    //------------------------------------------------------------
    /**
     * 歩いているエージェントをテーブルに追加
     * @param agent : 追加するエージェント
     */
    public void addWalkingAgent(AgentBase agent) {
        walkingAgentTable.put(agent.ID, agent) ;
    }

    //------------------------------------------------------------
    /**
     * 歩いているエージェントテーブルより一人取り除く。
     * @param agent : 取り除くエージェント。
     * @return 取り除きに成功すれば true ;
     */
    public boolean removeWalkingAgent(AgentBase agent) {
        if(walkingAgentTable.containsKey(agent.ID)) {
            walkingAgentTable.remove(agent.ID) ;
            return true ;
        } else {
            Itk.logWarn("agent is not in walkingTable:", agent) ;
            return false ;
        }
    }

    /**
     * 最後のサイクルに避難完了したエージェントリストを返す.
     *
     * ※更新タイミングに注意
     */
    public ArrayList<AgentBase> getEvacuatedAgentsInStep() {
        return evacuatedAgentsInStep;
    }

    //------------------------------------------------------------
    /**
     * effective link のクリア
     */
    private void clearEffectiveLinkSet() {
        effectiveLinkSet.clear() ;
    }

    //------------------------------------------------------------
    /**
     * effective link を取得
     */
    private Set<MapLink> getEffectiveLinkSet() {
        return effectiveLinkSet ;
    }

    //------------------------------------------------------------
    /**
     * effective link の登録
     * @param link : 追加するリンク。
     */
    private void addEffectiveLink(MapLink link) {
        effectiveLinkSet.add(link) ;
    }

    //------------------------------------------------------------
    /**
     * effective link の削除
     * @param link : 削除するリンク。
     */
    private void removeEffectiveLink(MapLink link) {
        effectiveLinkSet.remove(link) ;
    }

    //------------------------------------------------------------
    /**
     * effective node のクリア
     */
    private void clearEffectiveNodeSet() {
        effectiveNodeSet.clear() ;
    }

    //------------------------------------------------------------
    /**
     * effective node を取得
     */
    private Set<MapNode> getEffectiveNodeSet() {
        return effectiveNodeSet ;
    }

    //------------------------------------------------------------
    /**
     * effective node の登録
     * @param node : 追加するリンク。
     */
    private void addEffectiveNode(MapNode node) {
        effectiveNodeSet.add(node) ;
    }

    //------------------------------------------------------------
    /**
     * effective node の削除
     * @param node : 削除するリンク。
     */
    private void removeEffectiveNode(MapNode node) {
        effectiveNodeSet.remove(node) ;
    }

    //------------------------------------------------------------
    // ログ関連
    //------------------------------------------------------------
    /**
     * ロガーの設定。パラメータなどを設定する。
     */
    public void setupSimulationLoggers() {
        setupAgentTrailLogger() ;
        setupAgentMovementHistoryLogger() ;
        setupIndividualPedestriansLogger() ;
        setupEvacuatedAgentsLogger() ;
    }
    //------------------------------------------------------------
    /**
     * ロガーの初期化処理。実際にファイルをオープンするなど。
     */
    public void initSimulationLoggers() {
        initAgentTrailLogger() ;
        initAgentMovementHistoryLogger() ;
        initIndividualPedestriansLogger() ;
        initEvacuatedAgentsLogger() ;
    }

    //------------------------------------------------------------
    /**
     * ロガーの終了処理
     */
    public void finalizeSimulationLoggers() {
        closeIndividualPedestriansLogger();
        closeAgentMovementHistorLogger();
        closeAgentTrailLogger();
        closeEvacuatedAgentsLogger();
    }

    //------------------------------------------------------------
    /**
     * ロガーの初期化。
     * @param name : ロガーを表すタグ。
     * @param level : ロギングレベル。
     * @param formatter : ログフォーマット。
     * @param filePath : ログファイル名。
     * @return ロガーを返す。
     */
    private Logger openLogger(String name, Level level, String filePath) {
        Logger logger = Logger.getLogger(name);
        java.util.logging.Formatter formatter =
            new java.util.logging.Formatter() {
                public String format(final LogRecord record) {
                    return formatMessage(record) + "\n";
                }
            } ;
        
        try {
            // Check parent directory exists.
            // If not exist, and Properties' flag is true,
            // create it.
            String dirname = (new File(filePath)).getParent() ;
            File dir = new File(dirname) ;
            if(!dir.exists() &&
               simulator.getProperties().doesCreateLogDirAutomatically()) {
                dir.mkdirs() ;
            }
            // 「ロックファイルが残っているとログファイルに連番が振られて増え続けてしまう」不具合を回避する
            File file = new File(filePath + ".lck");
            if (file.exists()) {
                Itk.logWarn("delete lock file", filePath + ".lck");
                file.delete();
            }

            FileHandler handler = new FileHandler(filePath);
            handler.setFormatter(formatter);
            logger.addHandler(handler);
        } catch (IOException e) {
            Itk.quitWithStackTrace(e);
        }
        logger.setLevel(level);
        logger.setUseParentHandlers(false); // コンソールには出力しない
        
        return logger;
    }

    //------------------------------------------------------------
    /**
     * ロガーの終了処理。
     * @param logger : 終了するロガー。
     */
    private void closeLogger(Logger logger) {
        if (logger != null) {
            for (Handler handler : logger.getHandlers()) {
                handler.close();
            }
        }
    }

    //------------------------------------------------------------
    // AgentTrailLog関係。
    //------------------------------------------------------------
    /**
     * AgentTrailLog の各種設定。
     * constructor で呼ばれる。 
     */
    private void setupAgentTrailLogger() {
        // Log Style
        agentTrailLogStyle =
            (JsonFormatter.OverallStyle)
            SetupFileInfo
            .fetchFallbackObjectViaLexicon(fallback,
                                           JsonFormatter.overallStyleLexicon,
                                           "agentTrailLogStyle",
                                           agentTrailLogStyle) ;
        agentTrailLogFormatter.setOverallStyle(agentTrailLogStyle) ;

        // setup log items (file の設定より先に必要)
        if(simulator.getProperties().hasKeyRecursive("agent_trail_log",
                                                     "members")) {
            Term logMembers =
                simulator.getProperties()
                .getTerm("agent_trail_log")
                .getArgTerm("members") ;
            agentTrailLogFormatter.setMembersByTerm(logMembers) ;
        }
        
        // trailAux の items (file の設定より先に必要)
        if(simulator.getProperties().hasKeyRecursive("agent_trail_log",
                                                     "trail_aux")) {
            Term auxMembers =
                simulator.getProperties()
                .getTerm("agent_trail_log")
                .getArgTerm("trail_aux") ;
            agentTrailLogTrailAuxFormatter.setMembersByTerm(auxMembers) ;
        }
    }
    
    //------------------------------------------------------------
    /**
     * AgentTrailLog 初期化。
     * properties file の中の設定法。(json形式)
     * <pre> {
     *   ...
     *   "agent_trail_log" : 
     *     {
     *       "file" : __LogFilePath__,
     *       "members": [__MemberName__, __MemberName__, ...]
     *     }
     * } </pre>
     */
    public void initAgentTrailLogger() {
        try {
            // setup log file
            if(simulator.getProperties().hasKeyRecursive("agent_trail_log",
                                                         "file")) {
                String agentTrailLogPath =
                    simulator.getProperties()
                    .getTerm("agent_trail_log")
                    .getArgString("file") ;
                if (agentTrailLogPath != null) {
                    // properties の相対パスを補う。
                    agentTrailLogPath =
                        simulator.getProperties()
                        .furnishPropertiesDirPath(agentTrailLogPath,
                                                  true, false) ;
                    openAgentTrailLogger("agent_trail_log", agentTrailLogPath);
                }
            }
        } catch(Exception e) {
            Itk.logError("can not setup AgentTrailLogger",e.getMessage()) ;
            Itk.quitWithStackTrace(e) ;
        }
    }
        
    //------------------------------------------------------------
    /**
     * AgentTrailLogger の初期化。
     * @param name : ロガーの名前。
     * @param filePath : ログファイル名。
     */
    private void openAgentTrailLogger(String name, String filePath) {
        agentTrailLogger =
            openLogger(name, Level.INFO, filePath);
        agentTrailLogFormatter
            .outputHeaderToLoggerInfo(agentTrailLogger) ;
    }

    //------------------------------------------------------------
    /**
     * AgentTrailLogger の終了
     */
    private void closeAgentTrailLogger() {
        agentTrailLogFormatter
            .outputTailerToLoggerInfo(agentTrailLogger) ;
        closeLogger(agentTrailLogger);
    }

    //------------------------------------------------------------
    /**
     * agent の Trail を記録するかどうか？
     */
    final public boolean doesRecordAgentTrail() {
        return agentTrailLogger != null ;
    }
    //------------------------------------------------------------
    /**
     * agentTrail のログ出力の本体。
     */
    final private void logAgentTrail(AgentBase agent, SimTime currentTime) {
        if (doesRecordAgentTrail()) {
            agentTrailLogFormatter
                .outputRecordToLoggerInfo(agentTrailLogger,
                                          agent, currentTime, this);
        }
    }

    //------------------------------------------------------------
    // individual pedestrian logger 関係。
    //------------------------------------------------------------
    /**
     * individual pedestrian logger の各種設定。
     * コンストラクタで呼ばれる。
     */
    private void setupIndividualPedestriansLogger() {
        Term logColumns =
            SetupFileInfo
            .fetchFallbackTerm(fallback,
                               "logColumnsOfIndividualPedestrians",
                               Term.newArrayTerm());
        for (int i = 0; i < logColumns.getArraySize(); i++) {
            Term columnName = logColumns.getNthTerm(i);
            logColumnsOfIndividualPedestrians.add(columnName.getString());
        }
        
        // tick size of individual pedestrian log
        tickIntervalForIndividualPedestriansLog =
            SetupFileInfo
            .fetchFallbackInt(fallback,
                              "tickIntervalForIndividualPedestriansLog",
                              tickIntervalForIndividualPedestriansLog) ;

        // Offset of individual pedestrian log
        offsetOfIndividualPedestriansLog = SetupFileInfo.fetchFallbackInt(fallback, "offsetOfIndividualPedestriansLog", offsetOfIndividualPedestriansLog);

        // 出力(非)対象エージェントの設定
        Term logAgentsFallback = SetupFileInfo.filterFallbackTerm(fallback, "logAgentsOfIndividualPedestrians");
        Term tags = SetupFileInfo.fetchFallbackTerm(logAgentsFallback, "tags", Term.newArrayTerm());
        for (int i = 0; i < tags.getArraySize(); i++) {
            Term tag = tags.getNthTerm(i);
            Matcher matcher = Pattern.compile("^\\/(.*)\\/$").matcher(tag.getString());
            if (matcher.find()) {
                // タグが正規表現だった場合
                Pattern tagPattern = Pattern.compile(matcher.group(1));
                regexpsForIndividualPedestriansLog.add(tagPattern);
            } else {
                tagsForIndividualPedestriansLog.add(tag.getString());
            }
        }
        excludeAgentsInIndividualPedestriansLog =
            SetupFileInfo.fetchFallbackBoolean(logAgentsFallback, "exclusion", false);
        if (tags.getArraySize() > 0) {
            Itk.logInfo("logAgentsOfIndividualPedestrians", tags.toJson(), "exclusion:" + excludeAgentsInIndividualPedestriansLog);
        }
    }

    //------------------------------------------------------------
    /**
     * individualPedestriansLogger の設定。
     */
    private void initIndividualPedestriansLogger() {
        try {
            String individualLogDir =
                simulator.getProperties()
                .getDirectoryPath("individual_pedestrians_log_dir", null);
            if (individualLogDir != null) {
                individualLogDir =
                    individualLogDir.replaceFirst("[/\\\\]+$", "");
            }

            if (individualLogDir != null) {
                openIndividualPedestriansLogger("individual_pedestrians_log",
                                                individualLogDir);
            }
        } catch(Exception e) {
            Itk.logError("can not setup Logger",e.getMessage()) ;
            Itk.quitWithStackTrace(e) ;
        }
    }
    
    //------------------------------------------------------------
    /**
     * individualPedestriansLogger の初期化。
     * @param name : ロガーの名前。
     * @param dirPath : ログファイルを格納するディレクトリ名。
     */
    private void openIndividualPedestriansLogger(String name, String dirPath) {
        individualPedestriansLogDir = dirPath;
        individualPedestriansLogger =
            openLogger(name, Level.INFO,
                       dirPath + "/log_individual_pedestrians.csv");
        
        individualPedestriansLoggerFormatter
            .setColumns(logColumnsOfIndividualPedestrians);
        
        individualPedestriansLoggerFormatter
            .outputHeaderToLoggerInfo(individualPedestriansLogger) ;

    }

    //------------------------------------------------------------
    /**
     * individualPedestriansLogger の終了
     */
    private void closeIndividualPedestriansLogger() {
        if (individualPedestriansLogger != null) {
            closeLogger(individualPedestriansLogger);
        }
    }

   //------------------------------------------------------------
    /**
     * check the currentTime is on a cycle of individualPedestriansLogger
     */
    final private boolean isLogIndividualPedestriansCycle(SimTime currentTime) {
        return (individualPedestriansLogger != null &&
                currentTime.getTickCount() >= offsetOfIndividualPedestriansLog &&
                (0 == ((currentTime.getTickCount() - offsetOfIndividualPedestriansLog)
                       % tickIntervalForIndividualPedestriansLog))) ;
    }

    //------------------------------------------------------------
    /**
     * individualPedestriansLogger への出力。
     */
    private void logIndividualPedestrians(AgentBase agent,
                                          SimTime currentTime) {
        if (! agent.isDead()) {
            if (! tagsForIndividualPedestriansLog.isEmpty() || ! regexpsForIndividualPedestriansLog.isEmpty()) {
                boolean applied = false;
                for (String tag : agent.getTags()) {
                    if (tagsForIndividualPedestriansLog.contains(tag)) {
                        applied = true;
                        break;
                    }
                    for (Pattern tagPattern : regexpsForIndividualPedestriansLog) {
                        if (tagPattern.matcher(tag).find()) {
                            applied = true;
                            break;
                        }
                    }
                    if (applied) {
                        break;
                    }
                }
                if (applied == excludeAgentsInIndividualPedestriansLog) {
                    return;
                }
            }
            individualPedestriansLoggerFormatter
                .outputValueToLoggerInfo(individualPedestriansLogger,
                                         agent, currentTime, this);
        }
    }

     //------------------------------------------------------------
    // AgentMovementHistoryLogger 関係
    //------------------------------------------------------------
    /**
     * AgentMovementHistoryLogger の設定。
     */
    private void setupAgentMovementHistoryLogger() {
        // currently, do nothing.  for the uniform setup of loggers.
    }
    
    //------------------------------------------------------------
    /**
     * AgentMovementHistoryLogger の初期化。
     */
    private void initAgentMovementHistoryLogger() {
        // 出力カラムにチェックポイント(ノード)の通過時刻を追加する
        String _checkpoints = simulator.getProperties().getString("checkpoints_of_agent_movement_history_log", "").trim();
        if (! _checkpoints.isEmpty()) {
            Term fallback = SetupFileInfo.filterFallbackTerm(simulator.getFallbackParameters(), "agent");
            double emptySpeed = SetupFileInfo.fetchFallbackDouble(fallback, "emptySpeed", 0.0);
            if (emptySpeed == 0.0) {
                Itk.logError("can not setup Logger", "fallback parameter error: /agent/emptySpeed");
                Itk.quitByError();
            }

            checkpoints = Arrays.asList(_checkpoints.split("\\s*,\\s*"));
            checkpointPassingTimes = new HashMap();
            for (String checkpoint : checkpoints) {
                // checkpoint が適切かどうかを検査する
                int counter = 0;
                for (MapNode node : simulator.getNodes()) {
                    if (node.hasTag(checkpoint)) {
                        counter++;
                        // このノードにつながったリンクが1ステップで通過可能な長さではないこと(tickUnitは1.0とする)
                        for (MapLink link : node.getLinks()) {
                            if (link.getLength() < emptySpeed) {
                                Itk.logError("can not setup Logger", "There is a link of a length that can passing in 1 step: " + checkpoint);
                                Itk.quitByError();
                            }
                        }
                    }
                }
                if (counter == 0) {
                    Itk.logError("can not setup Logger", "Checkpoint node does not exist: " + checkpoint);
                    Itk.quitByError();
                // } else if (counter > 1) {
                //     Itk.logError("can not setup Logger", "There are multiple checkpoint nodes: " + checkpoint);
                //     Itk.quitByError();
                }

                agentMovementHistoryLoggerFormatter.addColumn(
                    agentMovementHistoryLoggerFormatter.new Column(checkpoint) {
                        public String value(AgentBase agent, Object timeObj, Object agentHandlerObj) {
                            HashMap<String, SimTime> checkpointPassingTime = checkpointPassingTimes.get(agent);
                            if (checkpointPassingTime == null) {
                                return "";
                            }
                            SimTime passingTime = checkpointPassingTime.get(checkpoint);
                            return passingTime == null ? "" : "" + (int)passingTime.getRelativeTime();
                        }
                    }
                );
            }
        }

        try {
            String agentHistoryPath =
                simulator.getProperties()
                .getFilePath("agent_movement_history_file", null, false);
            if (agentHistoryPath != null) {
                openAgentMovementHistoryLogger("agent_movement_history",
                                               agentHistoryPath);
            }
        } catch(Exception e) {
            Itk.logError("can not setup Logger",e.getMessage()) ;
            Itk.quitWithStackTrace(e) ;
        }
    }
    
    //------------------------------------------------------------
    /**
     * AgentMovementHistoryLogger の初期化。
     * @param name : ロガーの名前。
     * @param filePath : ログファイル名。
     */
    public void openAgentMovementHistoryLogger(String name, String filePath) {
        agentMovementHistoryLogger =
            openLogger(name, Level.INFO, filePath);
        agentMovementHistoryLoggerFormatter
            .outputHeaderToLoggerInfo(agentMovementHistoryLogger) ;
    }

    //------------------------------------------------------------
    /**
     * AgentMovementHistoryLogger の終了
     */
    public void closeAgentMovementHistorLogger() {
        closeLogger(agentMovementHistoryLogger);
    }

    /**
     * AgentMovementHistoryLogger 用にチェックポイント通過時刻を記録する
     */
    private void savePassingTime(AgentBase agent, SimTime currentTime) {
        MapNode node = agent.getPrevNode();
        if (node == null) {
            return;
        }
        HashMap<String, SimTime> checkpointPassingTime = checkpointPassingTimes.get(agent);
        if (checkpointPassingTime == null) {
            checkpointPassingTime = new HashMap<String, SimTime>();
            checkpointPassingTimes.put(agent, checkpointPassingTime);
        }
        for (String checkpoint : checkpoints) {
            if (! checkpointPassingTime.containsKey(checkpoint)) {
                if (node.hasTag(checkpoint)) {
                    checkpointPassingTime.put(checkpoint, currentTime);
                    break;
                }
            }
        }
    }

    //------------------------------------------------------------
    /**
     * AgentMovementHistoryLogger の出力本体。
     */
    final private void logAgentMovementHistory(AgentBase agent,
                                               SimTime currentTime) {
      if (agentMovementHistoryLogger != null) {
            agentMovementHistoryLoggerFormatter
                .outputValueToLoggerInfo(agentMovementHistoryLogger,
                                         agent, currentTime, this);
        }
    }

    //------------------------------------------------------------
    // EvacuatedAgentsLogger 関係
    //------------------------------------------------------------
    /**
     * EvacuatedAgentsLogger の設定。
     */
    private void setupEvacuatedAgentsLogger() {
        // currently, do nothing.  for the uniform setup of loggers.
    }
    
    //------------------------------------------------------------
    /**
     * EvacuatedAgentsLogger の設定。
     */
    private void initEvacuatedAgentsLogger() {
        // ゴールの箇所の設定。
        // ゴールの場所がわかってないといけないので、
        // generation rule の読み込み後に行う必要がある。なので、ここで初期化。
        ArrayList<String> goalTags = getGoalTags();
        String nodeOrder = simulator.getProperties().getString("node_order_of_evacuated_agents_log", "").trim();
        if (nodeOrder.isEmpty()) {
            for (MapNode node : simulator.getNodes()) {
                for (String goalTag : goalTags) {
                    if (node.hasTag(goalTag)) {
                        evacuatedAgentsLoggerFormatter
                            .addColumn(evacuatedAgentsLoggerFormatter.new Column(node.getTagLabel()) {
                                public String value(HashMap<MapNode, Integer> agentCounter) {
                                    return agentCounter.get(node).toString();
                                }
                            });
                        evacuatedAgentsCountByExit.put(node, 0);
                        break;
                    }
                }
            }
        } else {
            // ※ nodeOrder にすべてのゴールノードが含まれるとは限らない
            for (String tag : nodeOrder.split("\\s*,\\s*")) {
                if (! goalTags.contains(tag)) {
                    Itk.logWarn("Unknown goal tag", "node_order_of_evacuated_agents_log: " + tag);
                }
                for (MapNode node : simulator.getNodes()) {
                    if (evacuatedAgentsCountByExit.containsKey(node)) {
                        continue;
                    }
                    if (node.hasTag(tag)) {
                        evacuatedAgentsLoggerFormatter
                            .addColumn(evacuatedAgentsLoggerFormatter.new Column(node.getTagLabel()) {
                                public String value(HashMap<MapNode, Integer> agentCounter) {
                                    return agentCounter.get(node).toString();
                                }
                            });
                        evacuatedAgentsCountByExit.put(node, 0);
                    }
                }
            }
        }

        // logfile
            
        try {
        // 出力カラムと初期値をセットする
            String evacuatedAgentsPath =
                simulator.getProperties()
                .getFilePath("evacuated_agents_log_file", null, false);

            // log setup
            if (evacuatedAgentsPath != null) {
                openEvacuatedAgentsLogger("evacuated_agents_log",
                                          evacuatedAgentsPath);
            }
        } catch(Exception e) {
            Itk.logError("can not setup Logger",e.getMessage()) ;
            Itk.quitWithStackTrace(e) ;
        }
    }

    //------------------------------------------------------------
    /**
     * EvacuatedAgentsLogger の初期化。
     * @param name : ロガーのタグ。
     * @param filePath : ログファイル名。
     */
    public void openEvacuatedAgentsLogger(String name, String filePath) {
        evacuatedAgentsLogger = openLogger(name, Level.INFO, filePath);

        evacuatedAgentsLoggerFormatter
            .outputHeaderToLoggerInfo(evacuatedAgentsLogger) ;
    }

    //------------------------------------------------------------
    /**
     * EvacuatedAgentsLogger の終了
     */
    public void closeEvacuatedAgentsLogger() {
        closeLogger(evacuatedAgentsLogger);
    }

    //------------------------------------------------------------
    /**
     * EvacuatedAgentsLogger 用のカウントアップ。
     */
    public void countEvacuatedAgentsForLogger(AgentBase agent,
                                              SimTime currentTime)
    {
        if (evacuatedAgentsLogger != null) {
            MapNode exitNode = agent.getPrevNode();
            Integer counter = evacuatedAgentsCountByExit.get(exitNode);
            if (counter == null) {
                finalizeSimulationLoggers();
                Itk.logError("Evacuated from unregistered goal", String.format("Time: %s Elapsed: %5.2fsec %s", currentTime.getAbsoluteTimeString(), currentTime.getRelativeTime(), exitNode.toShortInfo()));
                Itk.quitByError();
            }
            counter += 1;
            evacuatedAgentsCountByExit.put(exitNode, counter);
        }
    }
    
    //------------------------------------------------------------
    // Ruby 対応
    //------------------------------------------------------------
    /**
     * AgentFactoryByRuby への rubyEngine のリンク。
     * @param rubyEngine : rubyEngine
     */
    public void setupAgentFactoryByRuby(ItkRuby rubyEngine) {
        for(AgentFactory factory : agentFactoryList) {
            if(factory instanceof AgentFactoryByRuby) {
                ((AgentFactoryByRuby)factory).setupRubyEngine(rubyEngine) ;
            }
        }
    }
}
