// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.misc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.lang.Exception;

import javax.swing.JOptionPane;

import net.arnx.jsonic.JSON ;

import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Link.MapLinkTable;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.NetworkParts.Node.MapNodeTable;
import nodagumi.ananPJ.Agents.RunningAroundPerson.SpeedCalculationModel;

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

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * enum for generation rule type
     */
    public enum Rule {
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
             {"DENSITY", SpeedCalculationModel.DensityModel},
             {"EXPECTED", SpeedCalculationModel.ExpectedDensityModel}
            }) ;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * enum FileFormat Version
     */
    public enum FileFormat { Ver0, Ver1 }

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
         * 生成リンクリスト
         */
        public MapLinkTable startLinks = new MapLinkTable() ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 生成ノードリスト
         */
        public MapNodeTable startNodes = new MapNodeTable() ;
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
    }

    //============================================================
    /**
     * 生成ルール情報格納用クラス(TimeEvery 用)
     */
    static private class GenerationConfigForTimeEvery
        extends GenerationConfigBase {
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * ???
         */
        public int everyEndTime = 0 ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * ???
         */
        public int everySeconds = 0 ;
    }

    //------------------------------------------------------------
    /**
     * コンストラクタ兼設定解析ルーチン
     */
    public AgentGenerationFile(final String filename,
            MapNodeTable nodes,
            MapLinkTable links,
            boolean display,
            double linerGenerateAgentRatio,
            Random _random) throws Exception {
        if (filename == null || filename.isEmpty()) {
            return;
        }
        liner_generate_agent_ratio = linerGenerateAgentRatio;
        random = _random;

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filename));
        } catch (IOException e) {
            System.err.println(e);
            if (display) {
                JOptionPane.showMessageDialog(null,
                e.toString(),
                "Fail to open a generation file.",
                JOptionPane.ERROR_MESSAGE);
            }
            return;
        }
        String line = null;
        try {
            // 各行をCSVとして解釈するためのパーザ
            // quotation にはシングルクォート(')を用いる。
            // これは、JSON の文字列がダブルクォートのため。
            //[2014.12.23 I.Noda] csvParser は、ShiftingStringList の中へ。
            ShiftingStringList.setCsvSpecialChars(',','\'','\\') ;

            // [I.Noda] 先頭行を判定するための行カウンター
            int lineCount = 0 ;
            while ((line = br.readLine()) != null) {
                // モードライン読み込み
                if(lineCount == 0) scanModeLine(line) ;
                lineCount++ ;

                //コメント行読み飛ばし
                if (line.startsWith("#")) continue;
                if (line.startsWith(",")) continue;

                // おそらくいらないので、排除
                //String orgLine = line;
                // [2014/12/15 I.Noda]
                // 情報が失われるので、toUpperCase を使わない。
                // もしcase sensitive でないようにするなら、
                // String class の insensitive な比較メソッドを使うこと。
                //                line = line.toUpperCase();

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
                        continue;
                    }
                } else {
                    if (columns.length() < 5 && columns.length() != 2) {
                        System.err.println("malformed line: " + line);
                        continue;
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
                    continue;
                }

                // 生成条件の格納用データ
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

                // 生成の設定情報を以下にまとめて保持。
                genConfig.originalInfo = line ;

                /* [I.Noda] Ver1 以降は、rule_tag の直後はエージェントクラス名 */
                if(fileFormat == FileFormat.Ver1) {
                    genConfig.agentClassName = columns.top(0) ;
                    genConfig.agentConfString = columns.top(1) ;
                    columns.shift(2) ;
                }

                // read start link
                // もし start link の解析に失敗したら、次の行へ。
                if(! scanStartLinkTag(columns.get(), nodes, links, genConfig))
                    continue ;

                // 出発時刻
                try {
                    genConfig.startTime = scanTimeString(columns.get()) ;
                } catch(Exception ex) {
                    continue ;
                }

                // TIMEEVERYの場合は、出発時刻間隔
                if (rule_tag == Rule.TIMEEVERY) {
                    try {
                        ((GenerationConfigForTimeEvery)genConfig).everyEndTime =
                            scanTimeString(columns.get()) ;
                    } catch(Exception ex) {
                        continue ;
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
                if (rule_tag == Rule.EACHRANDOM) {
                    ((GenerationConfigForEachRandom)genConfig).maxFromEachPlace =
                        Integer.parseInt(columns.get()) ;
                }

                // 次はおそらく使われていない。
                //ArrayList<String> planned_route_key = new ArrayList<String>();

                // goal を scan
                genConfig.goal = columns.top() ;

                // ゴールより後ろの読み取り。
                if(!scanRestColumns(columns, nodes, links, genConfig))
                    continue ;

                // 経路情報に未定義のタグが使用されていないかチェックする
                ArrayList<String> routeErrors = checkPlannedRoute(nodes, links, genConfig.plannedRoute);
                if (! routeErrors.isEmpty()) {
                    definitionErrors.put(line, routeErrors);
                }

                // ここから、エージェント生成が始まる。
                if (rule_tag == Rule.EACH) {
                    doGenerationForEach(nodes, links, genConfig) ;
                } else if (rule_tag == Rule.RANDOM) {
                    doGenerationForRandom(nodes, links, genConfig) ;
                } else if (rule_tag == Rule.EACHRANDOM) {
                    doGenerationForEachRandom(nodes, links, 
                                              ((GenerationConfigForEachRandom)
                                               genConfig)) ;
                } else if (rule_tag == Rule.TIMEEVERY) {
                    doGenerationForTimeEvery(nodes, links,
                                             ((GenerationConfigForTimeEvery)
                                              genConfig)) ;
                } else {
                    System.err.println("AgentGenerationFile invalid rule " +
                                       "type in generation file!");
                }
            }
        } catch (Exception e) {
            System.err.println("Error in agent generation.");
            System.err.println(line);
            System.err.println(e.getMessage());
            e.printStackTrace();
        }

        // 経路情報に未定義のタグが使用されていたら例外を発生させる
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

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 時間・時刻表示の解析用パターン
     */
    static private Pattern timepat = 
        Pattern.compile("(\\d?\\d):(\\d?\\d):?(\\d?\\d)?");
    static private Pattern timepat2 = 
        Pattern.compile("(\\d?\\d):(\\d?\\d):(\\d?\\d)");

    //------------------------------------------------------------
    /**
     * 時間・時刻表示の解析
     * もし解析できなければ、Exception を throw。
     * @param timeStr 時間・時刻の文字列
     * @return 時刻・時間を返す。
     */
    public int scanTimeString(String timeStr) throws Exception {
        Matcher m2 = timepat2.matcher(timeStr) ;
        Matcher m = timepat.matcher(timeStr) ;

        int timeVal = 0 ;
        if (m2.matches()) {
            timeVal = 3600 * Integer.parseInt(m2.group(1)) +
                60 * Integer.parseInt(m2.group(2)) +
                Integer.parseInt(m2.group(3))
                ;
        } else if (m.matches()) {
            timeVal = 3600 * Integer.parseInt(m.group(1)) +
                60 * Integer.parseInt(m.group(2));
        } else {
            System.err.println("no matching item:" + timeStr +
                               " while reading agent generation rule.");
            throw new Exception("Illegal time format:" + timeStr) ;
        }
        return timeVal ;
    }

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
            if(versionString != null && versionString.equals("1")) {
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

    public void setLinerGenerateAgentRatio(double _liner_generate_agent_ratio) {
        liner_generate_agent_ratio = _liner_generate_agent_ratio;
    }

    public void setRandom(Random _random) {
        random = _random;
        for (GenerateAgent ga : this) {
            ga.setRandom(_random);
        }
    }

    // 経路情報に未定義のタグが使用されていたらその内容を返す
    public ArrayList<String> checkPlannedRoute(MapNodeTable nodes, MapLinkTable links, ArrayList<String> planned_route) {
        ArrayList<String> linkTags = new ArrayList();
        ArrayList<String> nodeTags = new ArrayList();
        int index = 0;
        while (index < planned_route.size()) {
            String candidate = planned_route.get(index);
            if (candidate.startsWith("WAIT_UNTIL")) {
                linkTags.add(candidate.substring(11));
                index += 3;
            } else if (candidate.startsWith("WAIT_FOR")) {
                linkTags.add(candidate.substring(9));
                index += 3;
            } else {
                nodeTags.add(candidate);
                index += 1;
            }
        }

        ArrayList<String> result = new ArrayList();
        for (String nodeTag : nodeTags) {
            boolean found = false;
            for (MapNode node : nodes) {
                if (node.hasTag(nodeTag)) {
                    found = true;
                    break;
                }
            }
            if (! found) {
                result.add("Undefined Node Tag: " + nodeTag);
            }
        }
        for (String linkTag : linkTags) {
            if (! links.tagExistP(linkTag))
                result.add("Undefined Link Tag: " + linkTag);
        }
        return result;
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
                                     MapNodeTable nodes,
                                     MapLinkTable links,
                                     GenerationConfigBase genConfig) {
        Matcher tag_match = startpat.matcher(start_link_tag);
        if (tag_match.matches()) {
            start_link_tag = tag_match.group(1);
            genConfig.conditions = tag_match.group(2).split(";");
        }

        /* get all links with the start_link_tag */
        links.findTaggedLinks(start_link_tag, genConfig.startLinks) ;

        for (MapNode node : nodes) {
            if (node.hasTag(start_link_tag)) {
                genConfig.startNodes.add(node);
            }
        }

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
                                    MapNodeTable nodes,
                                    MapLinkTable links,
                                    GenerationConfigBase genConfig) {
        //ArrayList<String> planned_route = new ArrayList<String>();
        genConfig.plannedRoute = new ArrayList<String>();

        // goal and route plan
        //String goal = items[index];
        if (genConfig.goal == null) {
            System.err.println("no matching link:" + columns.top() +
                               " while reading agent generation rule.");
            return false ;
        }
        columns.shift() ;
        while(!columns.isEmpty()) {
            if (columns.top() != null &&
                !columns.top().equals("")) {
                genConfig.plannedRoute.add(columns.top());
            }
            columns.shift() ;
        }
        return true ;
    }

    //------------------------------------------------------------
    /**
     * EACH 用生成ルーチン
     * 各々の link, node で total 個ずつのエージェントが生成。
     */
    private void doGenerationForEach(MapNodeTable nodes,
                                     MapLinkTable links,
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
    private void doGenerationForRandom(MapNodeTable nodes,
                                       MapLinkTable links,
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
    private void doGenerationForEachRandom(MapNodeTable nodes,
                                           MapLinkTable links,
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
        Itk.dbgMsg("chosen_nodes",chosen_nodes) ;
        Itk.dbgMsg("chosen_links",chosen_links) ;

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
    private void doGenerationForTimeEvery(MapNodeTable nodes,
                                          MapLinkTable links,
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
                // 2012.12.26 tkokada update
                // MapLink start_link = genConfig.startLinks.get(
                // random.nextInt(genConfig.startLinks.size()));
                MapLink start_link = null;
                while (start_link == null) {
                    MapLink tmp_link =
                        genConfig.startLinks.get(random.nextInt(genConfig.startLinks.size()));
                    start_link = tmp_link;
                }

                genConfig.startPlace = start_link ;
                genConfig.startTime = step_time ;
                genConfig.total = 1 ;
                this.add(new GenerateAgentFromLink(genConfig, random)) ;
            }
            step_time += every_seconds;
        }
    }

}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
