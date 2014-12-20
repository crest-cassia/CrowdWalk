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

import com.opencsv.CSVParser ;

import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Link.MapLinkTable;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.NetworkParts.Node.MapNodeTable;
import nodagumi.ananPJ.Agents.RunningAroundPerson.SpeedCalculationModel;

import nodagumi.Itk.*;

/** Generate agents depending on a generation file.
 * format of generation file of one line:
 * [RULE_STRING,]TAG,START_TIME,DURATION,TOTAL,EXIT_TAG[,ROUTE...]
 * STAFF,TAG,START_TIME,DURATION,TOTAL[,EXIT_TAG,NAVIGATED_LINK_TAG]*
 *
 * descriptions:
 *  RULE_STRING:    EACH or RANDOM or EACHRANDOM RANDOMALL
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
 * example4) STAFF,NODE_TAG,14:00:00,1,1,EXIT_2,LINK_1,EXIT_3,LINK_2
 * example5) RANDOMALL,NODE_TAG,14:00:00,1,1,EXIT_2,LINK_1,EXIT_3,LINK_2
 */
public class AgentGenerationFile extends ArrayList<GenerateAgent> 
    implements Serializable {
    private static final long serialVersionUID = 2334273513164226078L;
    private static final long MAX_LOOP_COUNTER = 1000;
    private static final String randomAllNodePrefixTag = "ROOT-N";
    private Random random = null;
    private double liner_generate_agent_ratio = 1.0;
    private LinkedHashMap<String, ArrayList<String>> definitionErrors = new LinkedHashMap();

    /**
     * enum FileFormat Version
     */
    public enum FileFormat { Ver0, Ver1 }

    /**
     * ファイルフォーマットのバージョン
     */
    public FileFormat fileFormat = FileFormat.Ver0;

    /**
     * mode を格納している Map
     */
    public Map<String,Object> modeMap ;

    /**
     * CSVのカラムのシフトをちゃんと扱うためのクラス。
     */
    private class ShiftingColumns {
        /**
         * 現在位置
         */
        private int index = 0 ;

        /**
         * カラムの列
         */
        private String[] columnList = null ;

        /**
         * コンストラクタ
         */
        public ShiftingColumns(String[] _columnList) {
            index = 0 ;
            columnList = _columnList ;
        }

        /**
         * shift: index をひとつずらす
         */
        public int shift() {
            return shift(1) ;
        }

        /**
         * shift: index を n ずらす
         */
        public int shift(int n) {
            index += n ;
            return index ;
        }

        /**
         * 残りの長さ
         */
        public int length() {
            return totalLength() - index ;
        }

        /**
         * もとの長さ
         */
        public int totalLength() {
            return columnList.length ;
        }

        /**
         * 空かどうかのチェック
         */
        public boolean isEmpty() {
            return length() <= 0 ;
        }

        /**
         * 現在の先頭を見る。（取り除かない）
         */
        public String top() {
            return top(0) ;
        }

        /**
         * 現在のindexよりn番目を取り出す。
         */
        public String top(int n) {
            return columnList[index + n] ;
        }

        /**
         * 現在の先頭を見る。（取り除く）
         */
        public String get() {
            String column = top() ;
            shift() ;
            return column ;
        }
    }

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
            Pattern rulepat;
            rulepat = Pattern.compile("EACH|RANDOM|EACHRANDOM|STAFF|" +
                    "RANDOMALL|TIMEEVERY|LINER_GENERATE_AGENT_RATIO");

            // 各行をCSVとして解釈するためのパーザ
            CSVParser csvParser = new CSVParser(',','"','\\') ;

            // [I.Noda] 先頭行を判定するための行カウンター
            int lineCount = 0 ;
            while ((line = br.readLine()) != null) {
                if(lineCount == 0) scanModeLine(line) ;
                lineCount++ ;
                if (line.startsWith("#")) continue;
                if (line.startsWith(",")) continue;

                // 生成の設定情報を以下にまとめて保持。
                GenerateAgent.Config genConfig = new GenerateAgent.Config() ;

                // おそらくいらないので、排除
                //String orgLine = line;
                // [2014/12/15 I.Noda]
                // 情報が失われるので、toUpperCase を使わない。
                // もしcase sensitive でないようにするなら、
                // String class の insensitive な比較メソッドを使うこと。
                //                line = line.toUpperCase();

                // [2014/12/15 I.Noda]
                // CSV Parser を使うように変更。
                // さらに、ShiftingColumns を使うよにする。
                //String items[] = line.split(",");
                //String items[] = csvParser.parseLine(line) ;
                //int index = 0;
                ShiftingColumns columns = 
                    new ShiftingColumns(csvParser.parseLine(line)) ;

                /* [I.Noda] Ver1 以降は、先頭はエージェントクラス名 */
                if(fileFormat == FileFormat.Ver1) {
                    genConfig.agentClassName = columns.top(0) ;
                    genConfig.agentConfString = columns.top(1) ;
                    columns.shift(2) ;
                }

                if (columns.length() < 5 && columns.length() != 2) {
                    System.err.println("malformed line: " + line);
                    continue;
                }

                // check rule strings
                /* [2014.12.20 I.Noda] should obsolete
                 * rule_tag が指定されないことがあるのか？
                 * これは、特に CSV の場合、致命的なバグの原因となる。
                 */
                String rule_tag = columns.top() ;
                Matcher rule_match = rulepat.matcher(rule_tag);
                if (rule_match.matches())
                    columns.shift() ;
                else {
                    // if no rule tag, default tag "EACH" is applied.
                    rule_tag = "EACH";
                }

                // LINER_GENERATE_AGENT_RATIO の場合、
                // lga_ratio が次に来る。
                // で、読み込んだら次の行。（エージェント生成しない）
                if (rule_tag.equals("LINER_GENERATE_AGENT_RATIO")) {
                    double lga_ratio = 0;
                    lga_ratio = Double.parseDouble(columns.get()) ;
                    if (lga_ratio > 0)
                        liner_generate_agent_ratio = lga_ratio;
                    continue;
                }
                // read start link
                StartInfo startInfo 
                    = scanStartLinkTag(columns.get(), nodes, links, rule_tag) ;
                if(startInfo.continueP) continue ;
                genConfig.conditions = startInfo.agentConditions ;
                // startInfo の startNodes, startLinks は別で利用するのでそのまま

                // time
                try {
                    genConfig.startTime = scanTimeString(columns.get()) ;
                } catch(Exception ex) {
                    continue ;
                }

                int every_seconds = 0;
                int every_end_time = 0;
                if (rule_tag.equals("TIMEEVERY")) {
                    try {
                        every_end_time = scanTimeString(columns.get()) ;
                    } catch(Exception ex) {
                        continue ;
                    }
                    every_seconds = Integer.parseInt(columns.get()) ;
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
                String speedModelString = columns.top() ;
                genConfig.speedModel =
                    SpeedCalculationModel.LaneModel;
                if (speedModelString.equals("LANE")) {
                    genConfig.speedModel = SpeedCalculationModel.LaneModel;
                    columns.shift() ;
                } else if (speedModelString.equals("DENSITY")) {
                    genConfig.speedModel = SpeedCalculationModel.DensityModel;
                    columns.shift() ;
                } else if (speedModelString.equals("EXPECTED")) {
                    genConfig.speedModel = SpeedCalculationModel.ExpectedDensityModel;
                    columns.shift() ;
                }

                // EACHRANDOM
                int each = 0;
                if (rule_tag.equals("EACHRANDOM")) {
                    each = Integer.parseInt(columns.get()) ;
                }

                // goal list which staff navigates
                ArrayList<String> navigationGoal = new ArrayList<String>();
                // navigated link for above goal
                ArrayList<String> navigatedLink = new ArrayList<String>();
                //ArrayList<String> planned_route = new ArrayList<String>();
                genConfig.plannedRoute = new ArrayList<String>();
                ArrayList<String> planned_route_key = new ArrayList<String>();
                genConfig.goal = columns.top() ;
                if (rule_tag.equals("RANDOMALL")) {
                    if (genConfig.goal == null) {
                        System.err.println("no matching link:" + columns.top() +
                                " while reading agent generation rule.");
                        continue;
                    }
                    // pick up nodes which contains specified tag
                    ArrayList<String> goal_candidates =
                        nodes.findPrefixedTagsOfTaggedNodes(genConfig.goal,
                                                            randomAllNodePrefixTag);
                    // and choose randomly
                    if (goal_candidates.size() > 0) {
                        genConfig.goal = goal_candidates.get(random.nextInt(
                                    goal_candidates.size() - 1));
                    }
                }
                if (rule_tag.equals("STAFF")) {
                    while(!columns.isEmpty()) {
                        if (columns.length() > 1) {
                            navigationGoal.add(columns.get()) ;
                            navigatedLink.add(columns.get()) ;
                        }
                    }
                    int numNavigation = navigationGoal.size();
                    if (navigationGoal.size() != navigatedLink.size())
                        numNavigation = Math.min(navigationGoal.size(),
                                navigatedLink.size());
                    for (int i = 0; i < numNavigation; i++) {
                        genConfig.plannedRoute.add(navigatedLink.get(i));
                    }
                } else if (rule_tag.equals("RANDOMALL")) {
                    if (genConfig.goal == null) {
                        System.err.println("no matching link:" + columns.top() +
                                " while reading agent generation rule.");
                        continue;
                    }
                    columns.shift() ;

                    ArrayList<String> route_tags = new ArrayList<String>();
                    while(! columns.isEmpty()) {
                        if (columns.top() != null &&
                            !columns.top().equals("")) {
                            route_tags.add(columns.top()) ;
                        }
                        columns.shift() ;
                    }
                    // Pick up nodes which contains specified tag to select
                    // route candidates from route tags.
                    for (int i = 0; i < route_tags.size(); i++) {
                        ArrayList<String> route_candidate =
                            nodes.findPrefixedTagsOfTaggedNodes(route_tags.get(i),
                                                                randomAllNodePrefixTag) ;
                        if (route_candidate.size() > 1)
                            genConfig.plannedRoute.add(route_candidate.get(
                                    random.nextInt(route_candidate.size() - 1)
                                ));
                    }
                    // And choose randomly. It's same with RANDOM.
                } else {
                    // goal and route plan
                    //String goal = items[index];
                    if (genConfig.goal == null) {
                        System.err.println("no matching link:" + columns.top() +
                                " while reading agent generation rule.");
                        continue;
                    }
                    columns.shift() ;

                    while(!columns.isEmpty()) {
                        if (columns.top() != null &&
                            !columns.top().equals("")) {
                            genConfig.plannedRoute.add(columns.top());
                        }
                        columns.shift() ;
                    }
                }

                // 経路情報に未定義のタグが使用されていないかチェックする
                ArrayList<String> routeErrors = checkPlannedRoute(nodes, links, genConfig.plannedRoute);
                if (! routeErrors.isEmpty()) {
                    definitionErrors.put(line, routeErrors);
                }

                // ここから、エージェント生成が始まる。
                if (rule_tag.equals("EACH")) {
                    doGenerationForEach(nodes, links, genConfig,
                                        genConfig.agentClassName,
                                        genConfig.agentConfString,
                                        startInfo,
                                        genConfig.goal,
                                        genConfig.plannedRoute,
                                        genConfig.startTime,
                                        genConfig.total,
                                        genConfig.duration,
                                        genConfig.speedModel,
                                        genConfig.originalInfo) ;
                    //} else if (rule_tag.equals("RANDOM")) {
                } else if (rule_tag.equals("RANDOM") ||
                           rule_tag.equals("RANDOMALL")) {
                    doGenerationForRandom(nodes, links, genConfig,
                                          genConfig.agentClassName,
                                          genConfig.agentConfString,
                                          startInfo,
                                          genConfig.goal,
                                          genConfig.plannedRoute,
                                          genConfig.startTime,
                                          genConfig.total,
                                          genConfig.duration,
                                          genConfig.speedModel,
                                          genConfig.originalInfo) ;
                } else if (rule_tag.equals("EACHRANDOM")) {
                    doGenerationForEachRandom(nodes, links, genConfig,
                                              startInfo,
                                              each,
                                              genConfig.total) ;
                } else if (rule_tag.equals("TIMEEVERY")) {
                    doGenerationForTimeEvery(nodes, links, genConfig,
                                             startInfo,
                                             every_end_time,
                                             every_seconds) ;
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

    /**
     * 時間・時刻表示の解析用パターン
     */
    private Pattern timepat = Pattern.compile("(\\d?\\d):(\\d?\\d):?(\\d?\\d)?");
    private Pattern timepat2 = Pattern.compile("(\\d?\\d):(\\d?\\d):(\\d?\\d)");
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

    /**
     * start_link_tag に含まれている情報
     */
    private class StartInfo {
        public String[] agentConditions = null ;
        public MapNodeTable startNodes = new MapNodeTable() ;
        public MapLinkTable startLinks = new MapLinkTable() ;
        public boolean continueP = false ;
    }

    /**
     * start_link_tag の解析パターン
     */
    static private Pattern startpat = Pattern.compile("(.+)\\((.+)\\)");

    /**
     * start_link_tag の解析
     */
    private StartInfo scanStartLinkTag(String start_link_tag,
                                       MapNodeTable nodes,
                                       MapLinkTable links,
                                       String rule_tag) {
        StartInfo startInfo = new StartInfo() ;

        Matcher tag_match = startpat.matcher(start_link_tag);
        if (tag_match.matches()) {
            start_link_tag = tag_match.group(1);
            startInfo.agentConditions = tag_match.group(2).split(";");
        }

        /* get all links with the start_link_tag */
        links.findTaggedLinks(start_link_tag, startInfo.startLinks) ;

        for (MapNode node : nodes) {
            if (node.hasTag(start_link_tag)) {
                startInfo.startNodes.add(node);
            }
        }

        if (rule_tag.equals("TIMEEVERY")) {
            /* [2014.12.18 I.Noda] 多分これで会っているはずなのだが。
            for (MapLink link : links) {
                ArrayList<String> tags = link.getTags();
                for (String tag : tags) {
                    // タグの比較を厳密化する
                    // if (tag.contains(start_link_tag)) {
                    if (tag.equals(start_link_tag)) {
                        startInfo.startLinks.add(link);
                        break;
                    }
                }
            }
            */
            links.findTaggedLinks(start_link_tag, startInfo.startLinks) ;
            if (startInfo.startLinks.size() <= 0)
                startInfo.continueP = true ;
        }
        if (startInfo.startLinks.size() == 0 &&
            startInfo.startNodes.size() == 0) {
            System.err.println("no matching start:" + start_link_tag);
            startInfo.continueP = true ;
        }
        return startInfo ;
    }

    /**
     * EACH 用生成ルーチン
     */
    private void doGenerationForEach(MapNodeTable nodes,
                                     MapLinkTable links,
                                     GenerateAgent.Config genConfig,
                                     String className,
                                     String agentConf,
                                     StartInfo startInfo,
                                     String goal,
                                     ArrayList<String> planned_route,
                                     double start_time,
                                     int total,
                                     double duration,
                                     SpeedCalculationModel speed_model,
                                     String line) {
        for (final MapLink start_link : startInfo.startLinks) {
            this.add(new GenerateAgentFromLink(className,
                                               agentConf,
                                               start_link,
                                               startInfo.agentConditions,
                                               goal,
                                               planned_route,
                                               start_time,
                                               duration,
                                               total,
                                               speed_model,
                                               random,
                                               line));
        }
        for (final MapNode start_node : startInfo.startNodes) {
            this.add(new GenerateAgentFromNode(className,
                                               agentConf,
                                               start_node,
                                               startInfo.agentConditions,
                                               goal,
                                               planned_route,
                                               start_time,
                                               duration,
                                               total,
                                               speed_model,
                                               random,
                                               line));
        }
    }

    /**
     * RANDOM/RANDOMALL 用生成ルーチン
     */
    private void doGenerationForRandom(MapNodeTable nodes,
                                       MapLinkTable links,
                                       GenerateAgent.Config genConfig,
                                       String className,
                                       String agentConf,
                                       StartInfo startInfo,
                                       String goal,
                                       ArrayList<String> planned_route,
                                       double start_time,
                                       int total,
                                       double duration,
                                       SpeedCalculationModel speed_model,
                                       String line) {
        int links_size = startInfo.startLinks.size();
        int size = links_size + startInfo.startNodes.size();
        int[] chosen_links = new int[startInfo.startLinks.size()];
        int[] chosen_nodes = new int[startInfo.startNodes.size()];
        for (int i = 0; i < total; i++) {
            int chosen_index = random.nextInt(size);
            if (chosen_index + 1 > links_size)
                chosen_nodes[chosen_index - links_size] += 1;
            else
                chosen_links[chosen_index] += 1;
        }
        for (int i = 0; i < startInfo.startLinks.size(); i++) {
            if (chosen_links[i] > 0)
                this.add(new GenerateAgentFromLink(className,
                                                   agentConf,
                                                   startInfo.startLinks.get(i),
                                                   startInfo.agentConditions,
                                                   goal,
                                                   planned_route,
                                                   start_time,
                                                   duration,
                                                   chosen_links[i],
                                                   speed_model,
                                                   random,
                                                   line));
        }
        for (int i = 0; i < startInfo.startNodes.size(); i++) {
            if (chosen_nodes[i] > 0)
                this.add(new GenerateAgentFromNode(className,
                                                   agentConf,
                                                   startInfo.startNodes.get(i),
                                                   startInfo.agentConditions,
                                                   goal,
                                                   planned_route,
                                                   start_time,
                                                   duration,
                                                   chosen_nodes[i],
                                                   speed_model,
                                                   random,
                                                   line));
        }
    }

    /**
     * EACH RANDOM 用生成ルーチン
     */
    private void doGenerationForEachRandom(MapNodeTable nodes,
                                           MapLinkTable links,
                                           GenerateAgent.Config genConfig,
                                           StartInfo startInfo,
                                           int each,
                                           int total) {
        int links_size = startInfo.startLinks.size();
        int size = links_size + startInfo.startNodes.size();
        int[] chosen_links = new int[startInfo.startLinks.size()];
        int[] chosen_nodes = new int[startInfo.startNodes.size()];
        for (int i = 0; i < total; i++) {
            int counter = 0;
            while (true) {
                int chosen_index = random.nextInt(size);
                if (chosen_index + 1 > links_size &&
                    chosen_nodes[chosen_index - links_size] < each) {
                    chosen_nodes[chosen_index - links_size] += 1;
                    break;
                } else if (chosen_links[chosen_index] < each) {
                    chosen_links[chosen_index] += 1;
                    break;
                } else if (counter > MAX_LOOP_COUNTER)
                    break;
                counter++;
            }
        }
        for (int i = 0; i < startInfo.startLinks.size(); i++) {
            if (chosen_links[i] > 0)
                this.add(new GenerateAgentFromLink(genConfig.agentClassName,
                                                   genConfig.agentConfString,
                                                   startInfo.startLinks.get(i),
                                                   genConfig.conditions,
                                                   genConfig.goal,
                                                   genConfig.plannedRoute,
                                                   genConfig.startTime,
                                                   genConfig.duration,
                                                   chosen_links[i],
                                                   genConfig.speedModel,
                                                   random,
                                                   genConfig.originalInfo)) ;

        }
        for (int i = 0; i < startInfo.startNodes.size(); i++) {
            if (chosen_nodes[i] > 0)
                this.add(new GenerateAgentFromNode(genConfig.agentClassName,
                                                   genConfig.agentConfString,
                                                   startInfo.startNodes.get(i),
                                                   genConfig.conditions,
                                                   genConfig.goal,
                                                   genConfig.plannedRoute,
                                                   genConfig.startTime,
                                                   genConfig.duration,
                                                   chosen_nodes[i],
                                                   genConfig.speedModel,
                                                   random,
                                                   genConfig.originalInfo)) ;
        }
    }

    /**
     * TIME EVERY 用生成ルーチン
     */
    private void doGenerationForTimeEvery(MapNodeTable nodes,
                                          MapLinkTable links,
                                          GenerateAgent.Config genConfig,
                                          StartInfo startInfo,
                                          int every_end_time,
                                          int every_seconds) {
        // [I.Noda] startPlace は下で指定。
        genConfig.startPlace = null ;
        // [I.Noda] ここでは、goal は特別な意味（ただし、あやしい）
        String goal = genConfig.goal ;
        genConfig.goal = null ;
        // [I.Noda] startTime も特別な意味
        int start_time = (int)genConfig.startTime ;
        genConfig.startTime = 0.0 ;
        // [I.Noda] total も特別な意味
        int total = genConfig.total ;
        // [I.Noda] plannedRoute も特別(ただし、あやしい)
        ArrayList<String> planned_route = genConfig.plannedRoute ;

        int step_time = start_time;
        /* let's assume start & goal & planned_route candidates
         * are all MapLink!
         */

        ArrayList<String> goalCandidates = new ArrayList<String>();
        for (MapNode node : nodes) {
            for (String tag : node.getTags()) {
                // タグの比較を厳密化する
                // if (tag.contains(goal))
                if (tag.equals(goal))
                    goalCandidates.add(tag);
                // [2014.12.18 I.Noda] should obsolete
                // この上、おかしくないか？
                // これだと、goalCandidates には、goal と
                // おなじ文字列しか入らないことになる。
            }
        }
        while (step_time <= every_end_time) {
            for (int i = 0; i < total; i++) {
                // 2012.12.26 tkokada update
                // MapLink start_link = startInfo.startLinks.get(
                // random.nextInt(startInfo.startLinks.size()));
                MapLink start_link = null;
                while (start_link == null) {
                    MapLink tmp_link =
                        startInfo.startLinks.get(random.nextInt(startInfo.startLinks.size()));
                    boolean invalid_tag = false;
                    for (String tag : tmp_link.getTags()) {
                        /* [2014.12.18 I.Noda] should obsolete
                         * なんじゃこりゃ？
                         * こんなところに隠しコマンド
                         */
                        if (tag.contains("INVALID")) {
                            invalid_tag = true;
                            break;
                        }
                    }
                    if (!invalid_tag) {
                        start_link = tmp_link;
                    }
                }
                if (goalCandidates.size() <= 0) {
                    System.err.println("AgentGenerationFile " +
                                       "no match goals for the tag: " +
                                       goal);
                }
                String goal_node =
                    goalCandidates.get(random.nextInt(goalCandidates.size()));
                ArrayList<String> plannedRoute =
                    new ArrayList<String>();
                for (String pr : planned_route) {
                    /* [2014.12.18 I.Noda] should obsolete
                     * 以下のアルゴリズム、おかしくないか？
                     * タグの比較を厳密化することにより、
                     * plannedRouteCandidates には、pr と等しい
                     * tag しか入らない。つまり、pr と同じ文字列しか
                     * 格納されない。
                     * 単に、その pr が存在するかどうか、なら、
                     * 意味はある。
                     * しかし、pr が tag に存在した場合、
                     * そこからランダム(random.nextInt())に選ぶ
                     * 理由が全く不明。
                     * なぜなら、それは、pr のみだから。
                     * つまり、pllanedRoute には、planned_route と
                     * 同じ物が入るだけ。
                     */
                    ArrayList<String> plannedRouteCandidates =
                        new ArrayList<String>();
                    for (MapNode node : nodes) {
                        for (String tag : node.getTags()) {
                            // タグの比較を厳密化する
                            // if (tag.contains(pr)) {
                            if (tag.equals(pr)) {
                                plannedRouteCandidates.add(tag);
                                break;
                            }
                        }
                    }
                    if (plannedRouteCandidates.isEmpty()) {
                        // 該当するノードタグが見つからない場合は WAIT_FOR/WAIT_UNTIL として扱う
                        plannedRoute.add(pr);
                    } else {
                        int chosenIndex = random.nextInt(plannedRouteCandidates.size());
                        plannedRoute.add(plannedRouteCandidates.get(chosenIndex));
                    }
                }
                genConfig.startPlace = start_link ;
                genConfig.goal = goal_node ;
                genConfig.plannedRoute = plannedRoute ;
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
