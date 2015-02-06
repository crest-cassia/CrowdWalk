// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.misc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List ;
import java.util.Iterator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.lang.Exception;
import java.lang.Integer;

import javax.swing.JOptionPane;

import net.arnx.jsonic.JSON ;

import nodagumi.ananPJ.NetworkMapBase;
import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Link.MapLinkTable;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.NetworkParts.Node.MapNodeTable;
import nodagumi.ananPJ.Agents.WalkAgent.SpeedCalculationModel;
import nodagumi.ananPJ.Agents.AwaitAgent.WaitDirective;
import nodagumi.ananPJ.misc.GenerateAgent ;
import nodagumi.Itk.*;

/** Generate agents depending on a generation file.
 * format of generation file of one line:
 * [RULE_STRING,][AgentClass,AgentConf,]TAG,START_TIME,DURATION,TOTAL,EXIT_TAG[,ROUTE...]
 * TAG,START_TIME,DURATION,TOTAL[,EXIT_TAG,NAVIGATED_LINK_TAG]*
 *  (memo: [AgentClass,AgentConf,] Part is only for Ver.1 format.
 *  (memo: STAFF は、2014.12.24 に排除することに決定。)
 *
 * descriptions:
 *  RULE_STRING:    EACH or RANDOM or EACHRANDOM 
 *  AgentClass:	    class name of agents (short name or full path name)
 *  AgentConf:      configuration for the agents. JSON format string
 *  TAG:            agents are generated on the links or nodes with this tag.
 *  START_TIME:     starting time which agents are generated
 *  TOTAL:          total number of generated agents
 *  DURATION:       duration time to finish generating agents from START_TIME
 *  EXIT_TAG:       set the goal of generated agents
 *  ROUTE:          routing point
 *  NAVIGATED_LINK_TAG:
 *                  navigated link which agent meets with the staff
 * example1) EACH,LINK_TAG_1,14:00:00,10,1,EXIT_TAG_2
 * example2) RANDOM,LINK_TAG_2,09:00:00,1,10,EXIT_TAG_3
 * example3) EACHRANDOM,LINK_TAG_3,23:44:12,10,140,1,EXIT_TAG4,STATION
 * example6) TIMEEVERY,NaiveAgent,"{}",LINK_TAG_1,18:00:00,18:00:00,60,60,100,LANE,EXIT_1,EXIT_2,EXIT_3
 */
public class AgentGenerationFile extends ArrayList<GenerateAgent> 
    implements Serializable {
    private static final long serialVersionUID = 2334273513164226078L;
    private Random random = null;
    private double liner_generate_agent_ratio = 1.0;
    private LinkedHashMap<String, ArrayList<String>> definitionErrors = new LinkedHashMap();

    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * enum for generation rule type
     */
    static public enum Rule {
        EACH,
        RANDOM,
        EACHRANDOM,
        TIMEEVERY,
        LINER_GENERATE_AGENT_RATIO
    }
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * Lexicon for Generation Rule
     */
    static Lexicon ruleLexicon = new Lexicon() ;
    static {
        // Rule で定義された名前をそのまま文字列で Lexicon を
        // 引けるようにする。
        // 例えば、 Rule.EACH は、"EACH" で引けるようになる。
        ruleLexicon.registerEnum(Rule.class) ;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * Lexicon for SpeedCalculationModel
     */
    static private Lexicon speedModelLexicon = new Lexicon() ;
    static {
        speedModelLexicon.registerMulti(new Object[][]
            {{"LANE", SpeedCalculationModel.LaneModel},
             {"STRAIT",SpeedCalculationModel.StraitModel}
            }) ;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * enum FileFormat Version
     */
    public enum FileFormat { Ver0, Ver1, Ver2 }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * ファイルフォーマットのバージョン
     */
    public FileFormat fileFormat = FileFormat.Ver0;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * mode を格納している Map
     */
    public Map<String,Object> modeMap ;

    //============================================================
    /**
     * 生成ルール情報格納用クラス(Base)
     */
    static private class GenerationConfigBase extends GenerateAgent.Config {
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 生成ルールのタイプ
         */
        public Rule ruleTag ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 生成リンクタグ
         */
        public String startLinkTag = null ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 生成リンクリスト
         */
        public MapLinkTable startLinks = new MapLinkTable() ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 生成ノードリスト
         */
        public MapNodeTable startNodes = new MapNodeTable() ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * fallback （指定がなかった場合の設定値の既定値を集めたもの）
         */
        public Term fallback ;

        //----------------------------------------
        /**
         * JSON への変換
         */
        public String toJson(boolean pprintP){
            return toTerm().toJson(pprintP) ;
        }

        //----------------------------------------
        /**
         * JSON Object への変換
         */
        public Term toTerm(){
            Term jObject = super.toTerm() ;

            jObject.setArg("rule",ruleLexicon.lookUpByMeaning(ruleTag).get(0));
            if(startLinkTag != null)
                jObject.setArg("startPlace", startLinkTag) ;
            jObject.setArg("speedModel", 
                           speedModelLexicon.lookUpByMeaning(speedModel).get(0));

            return jObject ;
        }
    }

    //============================================================
    /**
     * 生成ルール情報格納用クラス(EachRandom 用)
     */
    static private class GenerationConfigForEachRandom 
        extends GenerationConfigBase {
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 各リンク・ノードにおける発生上限数
         */
        public int maxFromEachPlace = 0 ;

        //----------------------------------------
        /**
         * JSON Object への変換
         */
        public Term toTerm(){
            Term jObject = super.toTerm() ;

            jObject.setArg("maxFromEach",maxFromEachPlace) ;

            return jObject ;
        }
    }

    //============================================================
    /**
     * 生成ルール情報格納用クラス(TimeEvery 用)
     */
    static private class GenerationConfigForTimeEvery
        extends GenerationConfigBase {
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 生成の終了時刻
         */
        public int everyEndTime = 0 ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 生成のインターバル
         */
        public int everySeconds = 0 ;

        //----------------------------------------
        /**
         * JSON Object への変換
         */
        public Term toTerm(){
            Term jObject = super.toTerm() ;

            jObject.setArg("everyEndTime",Itk.formatSecTime(everyEndTime)) ;
            jObject.setArg("everySeconds",everySeconds) ;

            return jObject ;
        }
    }

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public AgentGenerationFile(final String filename,
                               NetworkMapBase map,
                               Term fallbackConfig,
                               boolean display,
                               double linerGenerateAgentRatio,
                               Random _random)
        throws Exception 
    {
        if (filename == null || filename.isEmpty()) {
            return;
        }
        setLinerGenerateAgentRatio(linerGenerateAgentRatio);
        setRandom(_random);

        scanFile(filename, map, fallbackConfig, display) ;
    }

    //------------------------------------------------------------
    /**
     * 設定解析ルーチン
     */
    public void scanFile(final String filename,
                         NetworkMapBase map,
                         Term fallbackConfig,
                         boolean display)
        throws Exception
    {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filename));
        } catch (IOException e) {
            Itk.dbgErr(e) ;
            if (display) {
                JOptionPane.showMessageDialog(null,
                e.toString(),
                "Fail to open a generation file.",
                JOptionPane.ERROR_MESSAGE);
            }
            return;
        }

        // モードラインの読み込みを試す。
        // 呼んだ後、read pointer は先頭へ。
        tryScanModeLine(br) ;

        switch(fileFormat) {
        case Ver0:
        case Ver1:
            scanCsvFile(br, map, fallbackConfig) ;
            break ;
        case Ver2:
            scanJsonFile(br, map, fallbackConfig) ;
            break ;
        default:
            Itk.dbgErr("Unknown Format Version" + fileFormat.toString() +
                       "(file=" + filename + ")") ;
        }
        return ;
    }

    //------------------------------------------------------------
    /**
     * try to check mode line
     * [example]
     *   # { 'version' : '1' }
     * @param modeline 最初の行
     * @return modelineの形式であれば true を返す。
     */
    public boolean tryScanModeLine(BufferedReader reader) {
        if(!reader.markSupported()) {
            Itk.dbgWrn("This reader does not support mark():" + reader) ;
            return false ;
        } else {
            try {
                reader.mark(BufferedReadMax) ;
                String line = reader.readLine() ;
                if(line == null) {
                    Itk.dbgWrn("This file is empty:" + reader) ;
                    return false ;
                } else {
                    boolean scanned = scanModeLine(line) ;
                    if(!scanned) reader.reset() ;
                    return scanned ;
                }
            } catch (Exception ex) {
                ex.printStackTrace() ;
                Itk.dbgErr("something wrong to set mark for:" + reader) ;
                Itk.dbgMsg("BufferedReadMax", BufferedReadMax) ;
                return false ;
            }
        }
    }
    static private int BufferedReadMax = 1024 ; // 最大の1行のサイズ

    //------------------------------------------------------------
    /**
     * mode line check
     * [example]
     *   # { 'version' : '1' }
     * @param modeline 最初の行
     * @return modelineの形式であれば true を返す。
     */
    public boolean scanModeLine(String modeline) {
        if(modeline.startsWith("#")) {
            // 先頭の '#' をカット
            String modeString = modeline ;
            while(modeString.startsWith("#")) modeString = modeString.substring(1) ;
            // のこりを JSON として解釈
            modeMap = (Map<String, Object>)JSON.decode(modeString) ;
            String versionString = modeMap.get("version").toString() ;
            if(versionString != null && versionString.equals("2")) {
                fileFormat = FileFormat.Ver2 ;
            } else if(versionString != null && versionString.equals("1")) {
                fileFormat = FileFormat.Ver1 ;
            } else {
                fileFormat = FileFormat.Ver0 ;
            }
            return true ;
        } else {
            fileFormat = FileFormat.Ver0 ;
            return false ;
        }
    }

    //------------------------------------------------------------
    /**
     * 設定解析ルーチン (CSV file) (Ver.0, Ver.1 file format)
     */
    public void scanCsvFile(BufferedReader br,
                            NetworkMapBase map,
                            Term fallbackConfig)
        throws Exception
    {
        String line = null;
        try {
            // 各行をCSVとして解釈するためのパーザ
            // quotation にはシングルクォート(')を用いる。
            // これは、JSON の文字列がダブルクォートのため。
            //[2014.12.23 I.Noda] csvParser は、ShiftingStringList の中へ。
            ShiftingStringList.setCsvSpecialChars(',','\'','\\') ;

            while ((line = br.readLine()) != null) {
                //一行解析
                GenerationConfigBase genConfig =
                    scanCsvFileOneLine(line, map) ;

                if(genConfig == null) continue ;

                // 経路情報に未定義のタグが使用されていないかチェックする
                checkPlannedRouteInConfig(map, genConfig, line) ;

                // ここから、エージェント生成が始まる。
                doGenerationByConfig(map, genConfig, fallbackConfig) ;
            }
        } catch (Exception e) {
            System.err.println("Error in agent generation.");
            System.err.println(line);
            System.err.println(e.getMessage());
            e.printStackTrace();
        }

        // 経路情報に未定義のタグが使用されていたら例外を発生させる
        raiseExceptionRouteDefinitionError() ;
    }

    //------------------------------------------------------------
    /**
     * scan one line of CSV file
     */
    private GenerationConfigBase scanCsvFileOneLine(String line,
                                                    NetworkMapBase map)
        throws IOException
    {
        //コメント行読み飛ばし
        if (line.startsWith("#")) return null ;
        if (line.startsWith(",")) return null ;

        // カラムに分割
        // [2014/12/15 I.Noda]
        // CSV Parser を使うように変更。
        // さらに、ShiftingColumns を使うよにする。
        //String items[] = line.split(",");
        //String items[] = csvParser.parseLine(line) ;
        //int index = 0;
        ShiftingStringList columns =
            ShiftingStringList.newFromCsvRow(line) ;

        // 行の長さチェック
        if(fileFormat == FileFormat.Ver1) {
            if (columns.length() < 7 && columns.length() != 4) {
                System.err.println("malformed line: " + line);
                return null ;
            }
        } else {
            if (columns.length() < 5 && columns.length() != 2) {
                System.err.println("malformed line: " + line);
                return null ;
            }
        }

        // check rule strings
        /* [2014.12.20 I.Noda] should obsolete
         * rule_tag が指定されないことがあるのか？
         * これは、特に CSV の場合、致命的なバグの原因となる。
         */
        Rule rule_tag = (Rule)ruleLexicon.lookUp(columns.top()) ;
        if (rule_tag == null)
            // if no rule tag, default tag "EACH" is applied.
            rule_tag = Rule.EACH ;
        else
            columns.shift() ;

        // LINER_GENERATE_AGENT_RATIO の場合、
        // lga_ratio が次に来る。
        // で、読み込んだら次の行。（エージェント生成しない）
        if (rule_tag == Rule.LINER_GENERATE_AGENT_RATIO) {
            double lga_ratio = 0;
            lga_ratio = Double.parseDouble(columns.get()) ;
            if (lga_ratio > 0)
                liner_generate_agent_ratio = lga_ratio;
            return null ;
        }

        // 生成条件の格納用データ
        GenerationConfigBase genConfig = newConfigForRuleTag(rule_tag) ;

        // 生成の設定情報を以下にまとめて保持。
        genConfig.originalInfo = line ;

        /* [I.Noda] Ver1 以降は、rule_tag の直後はエージェントクラス名 */
        if(fileFormat == FileFormat.Ver1) {
            genConfig.agentClassName = columns.nth(0) ;
            genConfig.agentConf = Term.newByJson(columns.nth(1)) ;
            columns.shift(2) ;
        } else {
            /* [2014.12.29 I.Noda]
             * agentClassName を埋めておかないと、directive の処理が出来ない。
             * なので、GenerateAgent に指定する規定値を埋める。
             */
            genConfig.agentClassName = GenerateAgent.DefaultAgentClassName ;
        }

        // read start link
        // もし start link の解析に失敗したら、次の行へ。
        if(! scanStartLinkTag(columns.get(), map, genConfig))
            return null ;

        // 出発時刻
        try {
            genConfig.startTime = Itk.scanTimeStringToInt(columns.get()) ;
        } catch(Exception ex) {
            return null ;
        }

        // TIMEEVERYの場合は、出発時刻間隔
        if (rule_tag == Rule.TIMEEVERY) {
            try {
                ((GenerationConfigForTimeEvery)genConfig).everyEndTime =
                    Itk.scanTimeStringToInt(columns.get()) ;
            } catch(Exception ex) {
                return null ;
            }
            ((GenerationConfigForTimeEvery)genConfig).everySeconds =
                Integer.parseInt(columns.get()) ;
        }

        // duration
        genConfig.duration = Double.parseDouble(columns.get()) ;

        // total number of generated agents
        genConfig.total = Integer.parseInt(columns.get());
        if (liner_generate_agent_ratio > 0) {
            System.err.println("GenerateAgentFile total: " +
                               genConfig.total +
                               ", ratio: " + liner_generate_agent_ratio);
            genConfig.total = (int) (genConfig.total * liner_generate_agent_ratio);
            System.err.println("GenerateAgentFile total: " + genConfig.total);
        }

        // speed model
        /* [2014.12.20 I.Noda] should obsolete
         * speed_model を指定しないことはあるのか？
         * 少なくとも CSV で指定する場合、
         * 混乱の原因以外の何物でもない
         * 一番問題なのは、speed_model に相当するタグでなければ、
         * columns を shift しないところ。
         */
        genConfig.speedModel =
            (SpeedCalculationModel)speedModelLexicon.lookUp(columns.top()) ;
        if(genConfig.speedModel == null) {
            genConfig.speedModel = SpeedCalculationModel.LaneModel;
        } else {
            columns.shift() ;
        }

        // EACHRANDOM
        if (genConfig.ruleTag == Rule.EACHRANDOM) {
            ((GenerationConfigForEachRandom)genConfig).maxFromEachPlace =
                Integer.parseInt(columns.get()) ;
        }

        // 次はおそらく使われていない。
        //ArrayList<String> planned_route_key = new ArrayList<String>();

        // goal を scan
        genConfig.goal = new Term(columns.top()) ;

        // ゴールより後ろの読み取り。
        if(!scanRestColumns(columns, map, genConfig))
            return null ;

        return genConfig ;
    }

    //------------------------------------------------------------
    /**
     * new Generation Config for rule_tag
     */
    private GenerationConfigBase newConfigForRuleTag(Rule rule_tag) {
        GenerationConfigBase genConfig ;
        switch(rule_tag) {
        case EACHRANDOM:
            genConfig = new GenerationConfigForEachRandom() ;
            break ;
        case TIMEEVERY:
            genConfig = new GenerationConfigForTimeEvery() ;
            break ;
        default:
            genConfig = new GenerationConfigBase() ;
        }
        genConfig.ruleTag = rule_tag ;
        return genConfig ;
    }

    //------------------------------------------------------------
    /**
     */
    public void setLinerGenerateAgentRatio(double _liner_generate_agent_ratio) {
        liner_generate_agent_ratio = _liner_generate_agent_ratio;
    }

    //------------------------------------------------------------
    /**
     */
    public void setRandom(Random _random) {
        random = _random;
        for (GenerateAgent ga : this) {
            ga.setRandom(_random);
        }
    }

    //------------------------------------------------------------
    /**
     * start_link_tag の解析パターン
     */
    static private Pattern startpat = Pattern.compile("(.+)\\((.+)\\)");

    //------------------------------------------------------------
    /**
     * start_link_tag の解析
     * start_link_tag は、
     *    Tag | Tag(Cond;Cond;...) 
     * という形式らしい。
     */
    private boolean scanStartLinkTag(String start_link_tag,
                                     NetworkMapBase map,
                                     GenerationConfigBase genConfig) {
        Matcher tag_match = startpat.matcher(start_link_tag);
        if (tag_match.matches()) {
            start_link_tag = tag_match.group(1);
            genConfig.conditions = tag_match.group(2).split(";");
        }

        genConfig.startLinkTag = start_link_tag ;

        /* get all links with the start_link_tag */
        map.getLinks().findTaggedLinks(start_link_tag, genConfig.startLinks) ;

        /* get all nodes with the start_link_tag */
        map.getNodes().findTaggedNodes(start_link_tag, genConfig.startNodes) ;

        if (genConfig.startLinks.size() == 0 &&
            genConfig.startNodes.size() == 0) {
            System.err.println("no matching start:" + start_link_tag);
            return false ;
        } else {
            return true ;
        }
    }

    //------------------------------------------------------------
    /**
     * 残りの column の読み込み
     */
    private boolean scanRestColumns(ShiftingStringList columns,
                                    NetworkMapBase map,
                                    GenerationConfigBase genConfig) {
        //ArrayList<String> planned_route = new ArrayList<String>();
        genConfig.plannedRoute = new ArrayList<Term>();

        // goal and route plan
        //String goal = items[index];
        if (genConfig.goal.isNull()) {
            System.err.println("no matching link:" + columns.top() +
                               " while reading agent generation rule.");
            return false ;
        }
        columns.shift() ;
        while(!columns.isEmpty()) {
            String tag = columns.get() ;
            if (tag != null &&
                !tag.equals("")) {
                Term tagTerm = 
                    tryScanDirectiveAndMakeTerm(tag, columns) ;
                genConfig.plannedRoute.add(tagTerm) ;
            }
        }
        return true ;
    }

    //------------------------------------------------------------
    /**
     * WAIT directive の解釈
     * [2014.12.29 I.Noda]
     * ここだけからはどうしても、WAIT_* 系の処理を、
     * WaitRunningAroundAgent に局所化出来ない。
     * CSV である限り、括弧"()"の位置とか、
     * 一般的に扱う処理方法が見当たらない。
     */
    private Term tryScanDirectiveAndMakeTerm(String head,
                                             ShiftingStringList columns) {
        try {
            Matcher matchFull = 
                WaitDirective.FullPattern.matcher(head) ;
            if(matchFull.matches()) {
                // 引数込みですでにheadに含まれている。
                // directive 用の Term に変換する。
                WaitDirective directive =
                    WaitDirective.scanDirective(head) ;
                if(directive != null) {
                    return directive.toTerm() ;
                } else {
                    return new Term(head) ;
                }
            }
            Matcher matchHead =
                WaitDirective.HeadPattern.matcher(head) ;
            if(matchHead.matches()) {
                // CSV 解釈で、カンマで引数が分断されている場合。
                String fullForm = 
                    head + "," + columns.nth(0) + "," + columns.nth(1) ;
                Matcher matchFull2 = 
                    WaitDirective.FullPattern.matcher(fullForm) ;
                if(matchFull2.matches()) {
                    String directive = matchFull2.group(1) ;
                    WaitDirective.Type wait =
                        (WaitDirective.Type)
                        WaitDirective.lexicon.lookUp(directive) ;
                    if(wait != null) {
                        //正しい 3引数 wait directive は、まとめたものを返す。
                        columns.shift(2) ;
                        // 再帰呼び出し
                        return tryScanDirectiveAndMakeTerm(fullForm,
                                                           columns) ;
                    }
                }
                // ここで head に match しているとするならおかしい。
                Itk.dbgWrn("strange tag form in planned route:" + head) ;
            }
            // それ以外の場合は、もとの head をTerm化して返す。
            return new Term(head) ;
        } catch (Exception ex) {
            ex.printStackTrace() ;
            return new Term(head) ;
        }
    }

    //------------------------------------------------------------
    /**
     * 設定解析ルーチン (JSON file) (Ver.2 file format)
     */
    public void scanJsonFile(BufferedReader br,
                             NetworkMapBase map,
                             Term fallbackConfig)
        throws Exception
    {
        Term json = Term.newByScannedJson(JSON.decode(br),true) ;
        if(json.isArray()) {
            for(Object _item : json.getArray()) {
                Term item = (Term)_item ;
                if(item.isObject()) {
                    GenerationConfigBase genConfig = 
                        scanJsonFileOneItem(item, map) ;

                    if(genConfig == null) continue ;

                    //Itk.dbgMsg("genConfig",genConfig.toJson(false)) ;

                    // 経路情報に未定義のタグが使用されていないかチェックする
                    checkPlannedRouteInConfig(map, genConfig, item.toJson()) ;

                    // ここから、エージェント生成が始まる。
                    doGenerationByConfig(map, genConfig, fallbackConfig) ;
                } else {
                    Itk.dbgErr("wrong json for generation rule:",item.toJson()) ;
                    continue ;
                }
            }
        } else {
            Itk.dbgErr("wrong json for generation file:",json.toJson()) ;
            throw new Exception("wrong json for generation file:" + json.toJson()) ;
        }
    }

    //------------------------------------------------------------
    /**
     * 設定解析ルーチン (JSON one line) (Ver.2 file format)
     */
    public GenerationConfigBase scanJsonFileOneItem(Term json,
                                                    NetworkMapBase map)
    {
        // ignore が true なら、項目を無視する。
        if(json.getArgBoolean("ignore")) { return null ; }

        Rule ruleTag = 
            (Rule)ruleLexicon.lookUp(json.getArgString("rule")) ;
        GenerationConfigBase genConfig = newConfigForRuleTag(ruleTag) ;

        genConfig.originalInfo = json.toJson() ;

        Term agentType = json.getArgTerm("agentType") ;
        genConfig.agentClassName = agentType.getArgString("className") ;

        genConfig.agentConf = agentType.getArgTerm("config") ;

        if(!scanStartLinkTag(json.getArgString("startPlace"), map, genConfig))
            return null ;

        try {
            genConfig.startTime =
                Itk.scanTimeStringToInt(json.getArgString("startTime")) ;
        } catch(Exception ex) {
            return null ;
        }

        if(genConfig.ruleTag == Rule.TIMEEVERY) {
            try {
                String endTimeStr = json.getArgString("everyEndTime") ;
                ((GenerationConfigForTimeEvery)genConfig).everyEndTime =
                    Itk.scanTimeStringToInt(endTimeStr) ;
            } catch(Exception ex) {
                return null ;
            }
            ((GenerationConfigForTimeEvery)genConfig).everySeconds =
                json.getArgInt("everySeconds") ;
        }

        genConfig.duration = json.getArgDouble("duration") ;

        genConfig.total = json.getArgInt("total") ;
        if (liner_generate_agent_ratio > 0) {
            /*
            Itk.dbgMsg("GenerateAgentFile total: " +
                       genConfig.total +
                       ", ratio: " + liner_generate_agent_ratio);
            */
            genConfig.total = (int) (genConfig.total * liner_generate_agent_ratio);
            Itk.dbgMsg("GenerateAgentFile total: " + genConfig.total);
        }

        genConfig.speedModel =
            (SpeedCalculationModel)
            speedModelLexicon.lookUp(json.getArgString("speedModel")) ;
        if(genConfig.speedModel == null)
            genConfig.speedModel = SpeedCalculationModel.LaneModel;

        if (genConfig.ruleTag == Rule.EACHRANDOM) {
            ((GenerationConfigForEachRandom)genConfig).maxFromEachPlace =
                json.getArgInt("maxFromEach") ;
        }

        genConfig.goal = json.getArgTerm("goal") ;

        Term plannedRouteTerm = json.getArgTerm("plannedRoute") ;
        genConfig.plannedRoute =
            (plannedRouteTerm == null ?
             new ArrayList<Term>() :
             plannedRouteTerm.<Term>getTypedArray()) ;

        return genConfig ;
    }

    //------------------------------------------------------------
    /**
     * 経路情報に未定義のタグが使用されていないかチェックする
     */
    private void checkPlannedRouteInConfig(NetworkMapBase map,
                                           GenerationConfigBase genConfig,
                                           String where) {
        ArrayList<String> routeErrors =
            checkPlannedRoute(genConfig.agentClassName,
                              map, genConfig.plannedRoute);
        if (! routeErrors.isEmpty()) {
            definitionErrors.put(where, routeErrors);
        }
    }

    //------------------------------------------------------------
    /**
     * 経路情報に未定義のタグが使用されていたらその内容を返す
     */
    public ArrayList<String> checkPlannedRoute(String agentClassName,
                                               NetworkMapBase map,
                                               List<Term> planned_route) {
        ArrayList<Term> linkTags = new ArrayList();
        ArrayList<Term> nodeTags = new ArrayList();
        int index = 0;
        while (index < planned_route.size()) {
            Term candidate = planned_route.get(index);

            if(GenerateAgent
               .isKnownDirectiveInAgentClass(agentClassName, candidate)) {
                GenerateAgent
                    .pushPlaceTagInDirectiveByAgentClass(agentClassName,
                                                         candidate,
                                                         linkTags) ;
            } else {
                nodeTags.add(candidate);
            }
            index += 1 ;
        }

        ArrayList<String> result = new ArrayList();
        for (Term nodeTag : nodeTags) {
            boolean found = false;
            for (MapNode node : map.getNodes()) {
                if (node.hasTag(nodeTag)){
                    found = true;
                    break;
                }
            }
            if (! found) {
                result.add("Undefined Node Tag: " + nodeTag);
            }
        }
        for (Term linkTag : linkTags) {
            if (! map.getLinks().tagExistP(linkTag.getString()))
                result.add("Undefined Link Tag: " + linkTag);
        }
        return result;
    }

    //------------------------------------------------------------
    /**
     * 経路情報に未定義のタグが使用されていたら例外を発生させる
     * エラー情報は、checkPlannedRouteInConfig() で調べて蓄えられている。
     */
    private void raiseExceptionRouteDefinitionError()
        throws Exception
    {
        if (! definitionErrors.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder();
            //definitionErrors.forEach((_line, messages) -> {
            //    errorMessage.append("line: ").append(_line).append("\n");
            //    messages.forEach(message -> errorMessage.append("    ").append(message).append("\n"));
            //});
            Iterator it = definitionErrors.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, ArrayList<String>> entry = (Map.Entry)it.next();
                String _line = entry.getKey();
                ArrayList<String>messages = entry.getValue();
                errorMessage.append("line: ").append(_line).append("\n");
                for (String message: messages) {
                    errorMessage.append("    ").append(message).append("\n");
                }
            }
            throw new Exception(errorMessage.toString());
        }
    }

    //------------------------------------------------------------
    /**
     * エージェント生成
     */
    private void doGenerationByConfig(NetworkMapBase map,
                                      GenerationConfigBase genConfig,
                                      Term fallbackConfig) {
        genConfig.fallback = fallbackConfig ;
        switch(genConfig.ruleTag) {
        case EACH:
            doGenerationForEach(map, genConfig) ;
            break ;
        case RANDOM:
            doGenerationForRandom(map, genConfig) ;
            break ;
        case EACHRANDOM:
            doGenerationForEachRandom(map,
                                      ((GenerationConfigForEachRandom)
                                       genConfig)) ;
            break ;
        case TIMEEVERY:
            doGenerationForTimeEvery(map,
                                     ((GenerationConfigForTimeEvery)
                                      genConfig)) ;
            break ;
        default:
            Itk.dbgErr("AgentGenerationFile invalid rule " +
                       "type in generation file!") ;
        }
    }

    //------------------------------------------------------------
    /**
     * EACH 用生成ルーチン
     * 各々の link, node で total 個ずつのエージェントが生成。
     */
    private void doGenerationForEach(NetworkMapBase map,
                                     GenerationConfigBase genConfig) {
        for (final MapLink start_link : genConfig.startLinks) {
            genConfig.startPlace = start_link ;
            this.add(new GenerateAgentFromLink(genConfig, random)) ;
        }
        for (final MapNode start_node : genConfig.startNodes) {
            genConfig.startPlace = start_node ;
            this.add(new GenerateAgentFromNode(genConfig, random)) ;
        }
    }

    //------------------------------------------------------------
    /**
     * RANDOM 用生成ルーチン
     * 指定された link, node において、
     * 合計で total 個のエージェントが生成。
     */
    private void doGenerationForRandom(NetworkMapBase map,
                                       GenerationConfigBase genConfig) {
        int total = genConfig.total ;

        int links_size = genConfig.startLinks.size();
        int size = links_size + genConfig.startNodes.size();// linkとnodeの合計
        int[] chosen_links = new int[genConfig.startLinks.size()];
        int[] chosen_nodes = new int[genConfig.startNodes.size()];
        for (int i = 0; i < total; i++) {
            int chosen_index = random.nextInt(size);
            if (chosen_index + 1 > links_size)
                chosen_nodes[chosen_index - links_size] += 1;
            else
                chosen_links[chosen_index] += 1;
        }
        for (int i = 0; i < genConfig.startLinks.size(); i++) {
            if (chosen_links[i] > 0) {
                genConfig.startPlace = genConfig.startLinks.get(i) ;
                genConfig.total = chosen_links[i] ;
                this.add(new GenerateAgentFromLink(genConfig, random)) ;
            }
        }
        for (int i = 0; i < genConfig.startNodes.size(); i++) {
            if (chosen_nodes[i] > 0) {
                genConfig.startPlace = genConfig.startNodes.get(i) ;
                genConfig.total = chosen_nodes[i] ;
                this.add(new GenerateAgentFromNode(genConfig, random)) ;
            }
        }
    }

    //------------------------------------------------------------
    /**
     * EACH RANDOM 用生成ルーチン
     * RANDOM に、1箇所での生成数の上限を入れたもの。
     * 合計で total 個のエージェントが生成。
     */
    private void doGenerationForEachRandom(NetworkMapBase map,
                                           GenerationConfigForEachRandom genConfig) {
        int maxFromEachPlace = genConfig.maxFromEachPlace ;
        int total = genConfig.total ;

        int links_size = genConfig.startLinks.size() ;
        int nodes_size = genConfig.startNodes.size() ;
        int size = links_size + nodes_size ; // linkとnodeの合計
        int[] chosen_links = new int[genConfig.startLinks.size()];
        int[] chosen_nodes = new int[genConfig.startNodes.size()];

        /* [2014.12.24 I.Noda]
         * アルゴリズムがあまりにまずいので、修正。
         */
        if(total > 0) {
            int population = 0 ;
            //とりあえず、maxFromEachPlace で埋める。
            for(int i = 0 ; i < links_size ; i++) {
                chosen_links[i] = maxFromEachPlace ;
                population += maxFromEachPlace ;
            }
            for(int i = 0 ; i < nodes_size ; i++) {
                chosen_nodes[i] = maxFromEachPlace ;
                population += maxFromEachPlace ;
            }
            //減らしていく。
            while(population > total){
                int chosen_index = random.nextInt(size) ;
                if(chosen_index < links_size) {
                    if(chosen_links[chosen_index] > 0) {
                        chosen_links[chosen_index] -= 1 ;
                        population -= 1 ;
                    }
                } else {
                    if(chosen_nodes[chosen_index - links_size] > 0) {
                        chosen_nodes[chosen_index - links_size] -= 1;
                        population -= 1 ;
                    }
                }
            }
        }

        for (int i = 0; i < genConfig.startLinks.size(); i++) {
            if (chosen_links[i] > 0) {
                genConfig.startPlace = genConfig.startLinks.get(i) ;
                genConfig.total = chosen_links[i] ;
                this.add(new GenerateAgentFromLink(genConfig, random)) ;
            }

        }
        for (int i = 0; i < genConfig.startNodes.size(); i++) {
            if (chosen_nodes[i] > 0) {
                genConfig.startPlace = genConfig.startNodes.get(i) ;
                genConfig.total = chosen_nodes[i] ;
                this.add(new GenerateAgentFromNode(genConfig,random)) ;
            }
        }
    }

    //------------------------------------------------------------
    /**
     * TIME EVERY 用生成ルーチン
     * [2014.12.24 I.Noda]
     * GOAL の部分の処理は他と同じはずなので、
     * 特別な処理をしないようにする。
     * 合計で (total * 生成回数) 個のエージェントが生成。
     */
    private void doGenerationForTimeEvery(NetworkMapBase map,
                                          GenerationConfigForTimeEvery genConfig) {
        int every_end_time = genConfig.everyEndTime ;
        int every_seconds = genConfig.everySeconds ;
        int total = genConfig.total ;

        // [I.Noda] startPlace は下で指定。
        genConfig.startPlace = null ;
        // [I.Noda] startTime も特別な意味
        int start_time = (int)genConfig.startTime ;
        genConfig.startTime = 0.0 ;

        int step_time = start_time;
        /* let's assume start & goal & planned_route candidates
         * are all MapLink!
         */

        while (step_time <= every_end_time) {
            for (int i = 0; i < total; i++) {
                genConfig.startTime = step_time ;
                genConfig.total = 1 ;
                if(genConfig.startLinks.size() > 0) {
                    MapLink start_link = 
                        genConfig.startLinks.chooseRandom(random) ;
                    genConfig.startPlace = start_link ;
                    this.add(new GenerateAgentFromLink(genConfig, random)) ;
                } else if (genConfig.startNodes.size() > 0) {
                    MapNode start_node = 
                        genConfig.startNodes.chooseRandom(random) ;
                    genConfig.startPlace = start_node ;
                    this.add(new GenerateAgentFromNode(genConfig, random)) ;
                } else {
                    Itk.dbgErr("no starting place for generation.") ;
                    Itk.dbgMsg("config",genConfig) ;
                }
            }
            step_time += every_seconds;
        }
    }

}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
