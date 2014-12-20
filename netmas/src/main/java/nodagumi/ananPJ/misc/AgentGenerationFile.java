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
            Pattern timepat;
            timepat = Pattern.compile("(\\d?\\d):(\\d?\\d):?(\\d?\\d)?");
            Pattern timepat2;
            timepat2 = Pattern.compile("(\\d?\\d):(\\d?\\d):(\\d?\\d)");
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

                String orgLine = line;
                // [2014/12/15 I.Noda]
                // 情報が失われるので、toUpperCase を使わない。
                // もしcase sensitive でないようにするなら、
                // String class の insensitive な比較メソッドを使うこと。
                //                line = line.toUpperCase();

                // [2014/12/15 I.Noda]
                // CSV Parser を使うように変更。
                //String items[] = line.split(",");
                String items[] = csvParser.parseLine(line) ;

                /* [I.Noda] Ver1 以降は、先頭はエージェントクラス名 */
                String className = null;
                String agentConf = null;
                if(fileFormat == FileFormat.Ver1) {
                    className = items[0] ;
                    agentConf = items[1] ;
                    String[] newItems = new String[items.length-2] ;
                    System.arraycopy(items, 2, newItems, 0, items.length-2) ;
                    items = newItems ;
                }

                if (items.length < 5 && items.length != 2) {
                    System.err.println("malformed line: " + line);
                    continue;
                }

                // check rule strings
                int index = 0;
                String rule_tag = items[index];
                Matcher rule_match = rulepat.matcher(rule_tag);
                if (rule_match.matches())
                    index += 1;
                else {
                    // if no rule tag, default tag "EACH" is applied.
                    rule_tag = "EACH";
                }
                if (rule_tag.equals("LINER_GENERATE_AGENT_RATIO")) {
                    double lga_ratio = 0;
                    lga_ratio = Double.parseDouble(items[index]);
                    if (lga_ratio > 0)
                        liner_generate_agent_ratio = lga_ratio;
                    continue;
                }
                // read start link
                StartInfo startInfo 
                    = scanStartLinkTag(items[index], nodes, links, rule_tag) ;
                if(startInfo.continueP) continue ;
                index += 1;

                // time
                Matcher m2 = timepat2.matcher(items[index]);
                Matcher m = timepat.matcher(items[index]);
                int start_time;
                if (m2.matches()) {
                    start_time = 3600 * Integer.parseInt(m2.group(1)) +
                    60 * Integer.parseInt(m2.group(2)) +
                     Integer.parseInt(m2.group(3)) 
                    ;
                } else if (m.matches()) {
                    start_time = 3600 * Integer.parseInt(m.group(1)) +
                    60 * Integer.parseInt(m.group(2));
                } else {
                    System.err.println("no matching item:" + items[index] +
                    " while reading agent generation rule.");
                    continue;
                }
                index += 1;
                int every_seconds = 0;
                int every_end_time = 0;
                if (rule_tag.equals("TIMEEVERY")) {
                    Matcher timematch2 = timepat2.matcher(items[index]);
                    Matcher timematch = timepat.matcher(items[index]);
                    if (timematch2.matches()) {
                        every_end_time = 3600 *
                            Integer.parseInt(timematch2.group(1)) +
                            60 * Integer.parseInt(timematch2.group(2)) +
                            Integer.parseInt(timematch2.group(3));
                    } else if (timematch.matches()) {
                        every_end_time = 3600 *
                            Integer.parseInt(timematch.group(1)) +
                            60 * Integer.parseInt(timematch.group(2));
                    } else {
                        System.err.println("no matching item:" + items[index] +
                        " while reading agent generation rule.");
                        continue;
                    }
                    index += 1;
                    every_seconds = Integer.parseInt(items[index]);
                    index += 1;
                }

                // duration
                double duration = Double.parseDouble(items[index]);
                index += 1;
                // total number of generated agents
                int total = Integer.parseInt(items[index]);
                if (liner_generate_agent_ratio > 0) {
                    System.err.println("GenerateAgentFile total: " + total +
                            ", ratio: " + liner_generate_agent_ratio);
                    total = (int) (total * liner_generate_agent_ratio);
                    System.err.println("GenerateAgentFile total: " + total);
                }
                index += 1;
                // speed model
                String speedModelString = items[index];
                SpeedCalculationModel speed_model =
                    SpeedCalculationModel.LaneModel;
                if (speedModelString.equals("LANE")) {
                    speed_model = SpeedCalculationModel.LaneModel;
                    index += 1;
                } else if (speedModelString.equals("DENSITY")) {
                    speed_model = SpeedCalculationModel.DensityModel;
                    index += 1;
                } else if (speedModelString.equals("EXPECTED")) {
                    speed_model = SpeedCalculationModel.ExpectedDensityModel;
                    index += 1;
                }

                // EACHRANDOM
                int each = 0;
                if (rule_tag.equals("EACHRANDOM")) {
                    each = Integer.parseInt(items[index]);
                    index += 1;
                }

                // goal list which staff navigates
                ArrayList<String> navigationGoal = new ArrayList<String>();
                // navigated link for above goal
                ArrayList<String> navigatedLink = new ArrayList<String>();
                ArrayList<String> planned_route = new ArrayList<String>();
                ArrayList<String> planned_route_key = new ArrayList<String>();
                String goal = items[index];
                if (rule_tag.equals("RANDOMALL")) {
                    if (goal == null) {
                        System.err.println("no matching link:" + items[index] +
                                " while reading agent generation rule.");
                        continue;
                    }
                    // pick up nodes which contains specified tag
                    ArrayList<String> goal_candidates =
                        nodes.findPrefixedTagsOfTaggedNodes(goal,
                                                            randomAllNodePrefixTag);
                    // and choose randomly
                    if (goal_candidates.size() > 0) {
                        goal = goal_candidates.get(random.nextInt(
                                    goal_candidates.size() - 1));
                    }
                }
                if (rule_tag.equals("STAFF")) {
                    for (int i = index; i < items.length; i++) {
                        if (i + 1 < items.length) {
                            navigationGoal.add(items[i]);
                            navigatedLink.add(items[i + 1]);
                        }
                        i += 1;
                    }
                    int numNavigation = navigationGoal.size();
                    if (navigationGoal.size() != navigatedLink.size())
                        numNavigation = Math.min(navigationGoal.size(),
                                navigatedLink.size());
                    for (int i = 0; i < numNavigation; i++) {
                        planned_route.add(navigatedLink.get(i));
                    }
                } else if (rule_tag.equals("RANDOMALL")) {
                    if (goal == null) {
                        System.err.println("no matching link:" + items[index] +
                                " while reading agent generation rule.");
                        continue;
                    }
                    index += 1;
                    ArrayList<String> route_tags = new ArrayList<String>();
                    for (int i = index; i < items.length; ++i) {
                        if (items[i] != null &&
                                !items[i].equals("")) {
                            route_tags.add(items[i]);
                        }
                    }
                    // Pick up nodes which contains specified tag to select
                    // route candidates from route tags.
                    for (int i = 0; i < route_tags.size(); i++) {
                        ArrayList<String> route_candidate =
                            nodes.findPrefixedTagsOfTaggedNodes(route_tags.get(i),
                                                                randomAllNodePrefixTag) ;
                        if (route_candidate.size() > 1)
                            planned_route.add(route_candidate.get(
                                    random.nextInt(route_candidate.size() - 1)
                                ));
                    }
                    // And choose randomly. It's same with RANDOM.
                } else {
                    // goal and route plan
                    //String goal = items[index];
                    if (goal == null) {
                        System.err.println("no matching link:" + items[index] +
                                " while reading agent generation rule.");
                        continue;
                    }
                    index += 1;
                    for (int i = index; i < items.length; ++i) {
                        if (items[i] != null &&
                                !items[i].equals("")) {
                            planned_route.add(items[i]);
                        }
                    }
                }

                // 経路情報に未定義のタグが使用されていないかチェックする
                ArrayList<String> routeErrors = checkPlannedRoute(nodes, links, planned_route);
                if (! routeErrors.isEmpty()) {
                    definitionErrors.put(orgLine, routeErrors);
                }

                if (rule_tag.equals("EACH")) {
                    processGenerationForEach(nodes, links, 
                                             className,
                                             agentConf,
                                             startInfo,
                                             goal,
                                             planned_route,
                                             start_time,
                                             total,
                                             duration,
                                             speed_model,
                                             orgLine) ;
                //} else if (rule_tag.equals("RANDOM")) {
                } else if (rule_tag.equals("RANDOM") ||
                        rule_tag.equals("RANDOMALL")) {
                    processGenerationForRandom(nodes, links, 
                                               className,
                                               agentConf,
                                               startInfo,
                                               goal,
                                               planned_route,
                                               start_time,
                                               total,
                                               duration,
                                               speed_model,
                                               orgLine) ;
                } else if (rule_tag.equals("EACHRANDOM")) {
                    processGenerationForEachRandom(nodes, links, 
                                                   className,
                                                   agentConf,
                                                   startInfo,
                                                   goal,
                                                   planned_route,
                                                   each,
                                                   start_time,
                                                   total,
                                                   duration,
                                                   speed_model,
                                                   orgLine) ;
                } else if (rule_tag.equals("TIMEEVERY")) {
                    processGenerationForTimeEvery(nodes, links, 
                                                  className,
                                                  agentConf,
                                                  startInfo,
                                                  goal,
                                                  planned_route,
                                                  start_time,
                                                  every_end_time,
                                                  every_seconds,
                                                  total,
                                                  duration,
                                                  speed_model,
                                                  orgLine) ;
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
    private void processGenerationForEach(MapNodeTable nodes,
                                          MapLinkTable links,
                                          String className,
                                          String agentConf,
                                          StartInfo startInfo,
                                          String goal,
                                          ArrayList<String> planned_route,
                                          int start_time,
                                          int total,
                                          double duration,
                                          SpeedCalculationModel speed_model,
                                          String orgLine) {
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
                                               orgLine));
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
                                               orgLine));
        }
    }

    /**
     * RANDOM/RANDOMALL 用生成ルーチン
     */
    private void processGenerationForRandom(MapNodeTable nodes,
                                            MapLinkTable links,
                                            String className,
                                            String agentConf,
                                            StartInfo startInfo,
                                            String goal,
                                            ArrayList<String> planned_route,
                                            int start_time,
                                            int total,
                                            double duration,
                                            SpeedCalculationModel speed_model,
                                            String orgLine) {
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
                                                   orgLine));
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
                                                   orgLine));
        }
    }

    /**
     * EACH RANDOM 用生成ルーチン
     */
    private void processGenerationForEachRandom(MapNodeTable nodes,
                                                MapLinkTable links,
                                                String className,
                                                String agentConf,
                                                StartInfo startInfo,
                                                String goal,
                                                ArrayList<String> planned_route,
                                                int each,
                                                int start_time,
                                                int total,
                                                double duration,
                                                SpeedCalculationModel speed_model,
                                                String orgLine) {
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
                                                   orgLine));
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
                                                   orgLine));
        }
    }

    /**
     * TIME EVERY 用生成ルーチン
     */
    private void processGenerationForTimeEvery(MapNodeTable nodes,
                                               MapLinkTable links,
                                               String className,
                                               String agentConf,
                                               StartInfo startInfo,
                                               String goal,
                                               ArrayList<String> planned_route,
                                               int start_time,
                                               int every_end_time,
                                               int every_seconds,
                                               int total,
                                               double duration,
                                               SpeedCalculationModel speed_model,
                                               String orgLine) {
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
                // [2014.12.18 I.Noda]
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
                        /* [2014.12.18 I.Noda]
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
                    /* [2014.12.18 I.Noda]
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
                this.add(new GenerateAgentFromLink(className,
                                                   agentConf,
                                                   start_link,
                                                   startInfo.agentConditions,
                                                   goal_node,
                                                   plannedRoute,
                                                   step_time,
                                                   duration,
                                                   1,
                                                   speed_model,
                                                   random,
                                                   orgLine));
            }
            step_time += every_seconds;
        }
    }

}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
