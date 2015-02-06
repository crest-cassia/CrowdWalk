// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Agents;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.PrintStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ClassNotFoundException;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.vecmath.Vector3d;

import org.w3c.dom.Element;

import nodagumi.ananPJ.NetworkMap;
import nodagumi.ananPJ.NetworkParts.MapPartGroup;
import nodagumi.ananPJ.NetworkParts.OBMapPart;
import nodagumi.ananPJ.NetworkParts.OBNode;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.misc.AgentGenerationFile;
import nodagumi.ananPJ.misc.GenerateAgent;
import nodagumi.ananPJ.misc.RoutePlan ;
import nodagumi.ananPJ.misc.Place ;
import nodagumi.ananPJ.Simulator.Pollution;

import nodagumi.Itk.* ;

public abstract class AgentBase extends OBMapPart
implements Comparable<AgentBase>, Serializable {
    private static final long serialVersionUID = 2580480798262915926L;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * agent_count は生成したエージェントの総数。
     * agentNumber は、agent の中での通しナンバー。
     */
    static int agent_count = 0;
    public int agentNumber;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Agent の詳細設定情報を格納しているもの
     */
    public Term config ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Agent の詳細設定情報を格納しているもの
     */
    static public String configFallbackSlot = "_fallback" ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * generatedTime: 生成された時刻
     * finishedTime: ゴールに到達した時刻
     * evacuated: ゴールに到達したかどうかのフラグ
     */
    public double generatedTime;
    public double finishedTime;
    private boolean evacuated = false;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * goal: 目的地
     * routePlan: 目的地までの経路
     */
    protected Term goal;
    protected RoutePlan routePlan = new RoutePlan();

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 現在地および前回の位置。
     */
    protected Place currentPlace = new Place() ;
    protected Place lastPlace = new Place() ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * speed: エージェントが単位時間に進む距離
     * dv: 加速度
     */
    protected double speed;
    protected double dv = 0.0;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * currentExposuerAmount: 現在の暴露量
     * accumulatedExposuerAmount: 暴露量の累積
     * pollutionType: 累積するかどうか？
     * pollution: ???
     */
    public double currentExposureAmount = 0.0;
    public double accumulatedExposureAmount = 0.0;
    protected static String pollutionType = "NonAccumulated";
    protected Pollution pollution = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 表示・設定関係
     */
    public int displayMode = 0;
    protected double swing_width;
    protected String configLine = "none";

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 乱数生成器
     */
    protected Random random = null;

    //############################################################
    /**
     * 初期化関係
     */
    //------------------------------------------------------------
    /**
     * constractors
     * 引数なしconstractorはClassFinder.newByName で必要。
     */
    public AgentBase() {} ;
    public AgentBase(int _id, Random _random) {
        init(_id, _random) ;
    }

    //------------------------------------------------------------
    /**
     * エージェントのクラス短縮名を取得。
     * AgentBase は abstract なので、null
     */
    public static String getTypeName() { return null; };

    //------------------------------------------------------------
    /**
     * 初期化。constractorから分離。
     */
    public void init(int _id, Random _random) {
        super.init(_id);
        random = _random;
        agentNumber = agent_count++;
        //swing_width = Math.random() * 2.0 - 1.0;
        swing_width = random.nextDouble() * 2.0 - 1.0;
        // Pollution のサブクラスのインスタンスを取得
        pollution = Pollution.getInstance(pollutionType + "Pollution");
    }

    //------------------------------------------------------------
    /**
     * Conf による初期化。
     * 継承しているクラスの設定のため。
     * @param conf json の連想配列形式を scan した Map
     */
    public void initByConf(Term conf, Term fallback) {
        if(conf != null) {
            config = conf ;
        } else {
            config = new Term() ;
        }
        if(fallback != null) {
            config.setArg(configFallbackSlot, fallback) ;
        }
    } ;

    //------------------------------------------------------------
    /**
     * Conf による初期化。(obsolete)
     * 継承しているクラスの設定のため。
     * @param confString json で書かれたAgentのconfigulation。
     */
        public void _initByConf(String confString, Term fallback) {
        Term conf = Term.newByJson(confString) ;
        initByConf(conf, fallback) ;
    }

    //------------------------------------------------------------
    /**
     * Conf からの値の取得(double)
     */
    public double getDoubleFromConfig(String slot, double fallback) {
        if(config.hasArg(slot))
            return config.fetchArgDouble(slot, configFallbackSlot) ;
        else
            return fallback ;
    }

    //------------------------------------------------------------
    /**
     * Conf からの値の取得(double)
     */
    public double getIntFromConfig(String slot, int fallback) {
        if(config.hasArg(slot))
            return config.fetchArgInt(slot, configFallbackSlot) ;
        else
            return fallback ;
    }

    //------------------------------------------------------------
    /**
     * エージェント複製
     */
    public AgentBase copyAndInitialize() {
        try {
            AgentBase r = (AgentBase)this.getClass().newInstance() ;
            return copyAndInitializeBody(r) ;
        } catch(Exception ex) {
            ex.printStackTrace() ;
            Itk.dbgErr("can not make a new instance from an agent.") ;
            Itk.dbgMsg("agent", this) ;
            return null ;
        }
    }

    //------------------------------------------------------------
    /**
     * 与えられたエージェントインスタンスに内容をコピーし、初期化。
     */
    public AgentBase copyAndInitializeBody(AgentBase r) {
        r.ID = ID;
        r.generatedTime = generatedTime;
        r.currentPlace = currentPlace.duplicate() ;
        r.speed = 0;
        r.goal = goal;
        r.routePlan = routePlan.duplicate() ;
        r.routePlan.resetIndex() ;
        
        r.random = random;
        for (String tag : tags) {
            r.addTag(tag);
        }

        return r ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * クラス名によるエージェントの作成。
     * [2014.12.30 I.Noda]
     * これまで決め打ちであったものを、GenerateAgent で登録されたものが
     * 自動で対応されるようにしてある。
     */
    public static AgentBase createEmptyAgent(String type, Random _random) {
        if (type.equals("NOT SELECTED")) {
            return null;
        } else {
            AgentBase agent = GenerateAgent.newAgentByName(type) ;
            if(agent != null) {
                agent.init(0, _random) ;
                return agent ;
            } else {
                return null ;
            }
        }
    }

    //############################################################
    /**
     * インスタンス変数へのアクセス
     */
    //------------------------------------------------------------
    /**
     * エージェント id
     */
    public int getAgentNumber() {
        return agentNumber;
    }

    //------------------------------------------------------------
    /**
     * 避難完了をセット
     */
    public void setEvacuated(boolean evacuated, double time) {
        this.evacuated = evacuated;
    }

    //------------------------------------------------------------
    /**
     * 避難完了かどうかチェック
     */
    public boolean isEvacuated() {
        return evacuated;
    }

    //------------------------------------------------------------
    /**
     * ゴールをセット
     */
    public void setGoal(Term _goal) {
        goal = _goal;
    }

    //------------------------------------------------------------
    /**
     * ゴールを変更
     * 基本的に setGoal と同じだが、
     * シミュレーション途中でゴールを変更する場合に、
     * クラスごとに処理を変更できるようにラッパーとして用意する。
     */
    public void changeGoal(Term _goal) {
        setGoal(_goal) ;
    }

    //------------------------------------------------------------
    /**
     * ゴールを取得
     */
    public Term getGoal() {
        return goal;
    }

    //------------------------------------------------------------
    /**
     * 経路を取得
     */
    public RoutePlan getRoutePlan() {
        return routePlan ;
    } ;

    //------------------------------------------------------------
    /**
     * 全経路をセット
     */
    public void setPlannedRoute(List<Term> _planned_route) {
        routePlan.setRoute(_planned_route) ;
    }

    //------------------------------------------------------------
    /**
     * 全経路を取得
     */
    public List<Term> getPlannedRoute() {
        return routePlan.getRoute() ;
    }

    //------------------------------------------------------------
    /**
     * 現在のリンクを取得
     */
    public MapLink getCurrentLink() {
        return currentPlace.getLink() ;
    }

    //------------------------------------------------------------
    /**
     * 直前ノードを取得
     */
    public MapNode getPrevNode() {
        return currentPlace.getEnteringNode() ;
    }

    //------------------------------------------------------------
    /**
     * 次のノードを取得
     */
    public MapNode getNextNode() {
        return currentPlace.getHeadingNode() ;
    }

    //------------------------------------------------------------
    /**
     * ゴール後の最後のノードを取得
     */
    public MapNode getLastNode() {
        return currentPlace.getLastNode() ;
    }

    //------------------------------------------------------------
    /**
     * リンク上の進んだ距離
     */
    public double getAdvancingDistance() {
        return currentPlace.getAdvancingDistance() ;
    }

    //------------------------------------------------------------
    /**
     * リンク上の残りの距離
     */
    public double getRemainingDistance() {
        return currentPlace.getRemainingDistance() ;
    }

    //------------------------------------------------------------
    /**
     * 現在のリンク上の絶対位置（リンクから見た位置）を得る
     */
    public double getPositionOnLink() {
        return currentPlace.getPositionOnLink() ;
    }

    //------------------------------------------------------------
    /**
     * 最後のリンク上の絶対位置（リンクから見た位置）を得る
     */
    public double getLastPositionOnLink() {
        return lastPlace.getPositionOnLink() ;
    }

    //------------------------------------------------------------
    /**
     * 現在のリンクに対する向きを取得
     */
    public double getDirection() {
        return currentPlace.getDirectionValue() ;
    }

    //------------------------------------------------------------
    /**
     * 現在のリンクに対する向きを取得
     */
    public double getLastDirection() {
        return lastPlace.getDirectionValue() ;
    }

    //------------------------------------------------------------
    /**
     * 現在のリンクに対して前向き（リンクの fromNode から toNode）
     * に向かっているか？
     */
    public boolean isForwardDirection() {
        return currentPlace.isForwardDirection() ;
    }

    //------------------------------------------------------------
    /**
     * 現在のリンクに対して逆向き（リンクの toNode から fromNode）
     * に向かっているか？
     */
    public boolean isBackwardDirection() {
        return currentPlace.isBackwardDirection() ;
    }

    //------------------------------------------------------------
    /**
     * エージェントをリンクに配置
     */
    public void place(MapLink link, MapNode enteringNode, 
                      double advancingDistance) {
        currentPlace.set(link, enteringNode, true, advancingDistance) ;
    }

    //------------------------------------------------------------
    /**
     * エージェントをリンクにランダムに配置
     */
    public void placeAtRandomPosition(MapLink link) {
        currentPlace.setAtRandomPosition(link, random) ;
    }

    //------------------------------------------------------------
    /**
     * 向き変更
     */
    protected void turnAround() {
        currentPlace.turnAround() ;
    }
    
    //------------------------------------------------------------
    /**
     * 速度を取得
     */
    public double getSpeed() {
        return speed;
    }

    //------------------------------------------------------------
    /**
     * 速度をセット
     */
    public void setSpeed(double _speed) {
        speed = _speed;
    }

    //------------------------------------------------------------
    /**
     * 加速度を取得
     */
    public double getAcceleration() {
        return dv;
    }

    //------------------------------------------------------------
    /**
     * ???
     */
    public static void setPollutionType(String s) {
        pollutionType = s;
    }
    //------------------------------------------------------------
    /**
     * ???
     */
    public int getTriage() {
        return pollution.getTriage(this);
    }

    //------------------------------------------------------------
    /**
     * ???
     */
    public boolean finished() {
        return pollution.finished(this);
    }

    //------------------------------------------------------------
    /**
     * ???
     */
    public void exposed(double c) {
        pollution.expose(this, c);
    }

    //------------------------------------------------------------
    /**
     * 設定用
     */
    public String getConfigLine() {
        return configLine;
    }

    //------------------------------------------------------------
    /**
     * 設定用
     */
    public void setConfigLine(String str) {
        configLine = str;
    }

    //------------------------------------------------------------
    /**
     * 乱数生成器
     */
    public void setRandom(Random _random) {
        random = _random;
    }

    //############################################################
    /**
     * 経路計画関連
     */

    //------------------------------------------------------------
    /**
     *
     */
    public boolean isPlannedRouteCompleted() {
        return routePlan.isEmpty() ;
    }

    //------------------------------------------------------------
    /**
     *
     */
    public void consumePlannedRoute() {
        routePlan.makeCompleted() ;
    }

    //------------------------------------------------------------
    /**
     *
     */
    public String getNextCandidateString() {
        return isPlannedRouteCompleted() ? "" : routePlan.top().getString() ;
    }

    //------------------------------------------------------------
    /**
     * evacuation の完了
     * @param time : 時刻
     * @param onNode : true なら currentPlace.getHeadingNode() 上
     *                 false なら currentPlace.getLink() 上
     */
    protected void finalizeEvacuation(double time, boolean onNode) {
        consumePlannedRoute() ;
        setEvacuated(true, time) ;
        if(currentPlace.isWalking()) {
            lastPlace.set(currentPlace) ;
            currentPlace.getLink().agentExits(this) ;
            currentPlace.quitLastLink() ;
        }
    }

    //############################################################
    /**
     * シミュレーションサイクル
     */
    //------------------------------------------------------------
    /**
     * シミュレーション開始前に呼ばれる。
     */
    abstract public void prepareForSimulation(double _ts);

    //------------------------------------------------------------
    /**
     * シミュレーション各サイクルの前半に呼ばれる。
     */
    abstract public void preUpdate(double time);

    //------------------------------------------------------------
    /**
     * シミュレーション各サイクルの後半に呼ばれる。
     */
    abstract public boolean update(double time);

    //------------------------------------------------------------
    /**
     * 表示用
     */
    abstract public void updateViews();

    //############################################################
    /**
     * エージェントのソート関係
     */
    //------------------------------------------------------------
    /**
     * sort, binarySearch 用比較関数(古くて間違っている)
     */
    public int compareTo_orig(AgentBase rhs) {
        double h1 = this.currentPlace.getPositionOnLink() ;
        double h2 = rhs.currentPlace.getPositionOnLink() ;

        // tkokada modified
        if (h1 == h2) {
            //return (int)((agentNumber - rhs.agentNumber) * getDirection());
            // m.saito modified
            if (agentNumber == rhs.agentNumber) {
                return 0;
            } else if (agentNumber > rhs.agentNumber) {
                return (int)(1 * getDirection());
            } else {
                return (int)(-1 * getDirection());
            }
            //return 0;
        } else if (h1 > h2) {
            return (int)(1 * getDirection());
            //return 1;
        } else {
            return (int)(-1 * getDirection());
            //return -1;
        }
    }

    //------------------------------------------------------------
    /**
     * sort, binarySearch 用比較関数
     * エージェントのリンク上の進み具合で比較。
     * 逆向きなどはちゃんと方向を直して扱う。
     */
    public int compareTo(AgentBase rhs) {
        if(agentNumber == rhs.agentNumber) return 0 ;

        double h1 = this.currentPlace.getAdvancingDistance() ;
        double h2 = rhs.currentPlace.getAdvancingDistance() ;

        if(h1 > h2) {
            return 1 ;
        } else if(h1 < h2) {
            return -1 ;
        } else if(agentNumber > rhs.agentNumber) {
            return 1;
        } else {
            return -1;
        }
    }

    //############################################################
    /**
     * directive 関係
     */
    //------------------------------------------------------------
    /**
     * ある Term が Directive かどうかのチェック
     * 単純に Atom でないかどうかをチェック。
     * 実際の知りするかどうかは、isKnownDirective で定義。
     */
    public boolean isDirectiveTerm(Term term) {
        return !term.isAtom() ;
    }

    //------------------------------------------------------------
    /* [2014.12.29 I.Noda]
     * directive を増やす場合は、継承するクラスで以下２つを再定義していく。
     */
    /**
     * 知っている directive かどうかのチェック
     */
    public boolean isKnownDirective(Term term) {
        return false ;
    }

    //------------------------------------------------------------
    /**
     * Directive のなかの代表的目的地の取得
     * @param directive : 調べる directive。通常の place tag の場合もある。
     *    もし directive が isKnownDirective() なら、なにか返すべき。
     * @return もし directive なら代表的目的地。そうでないなら null
     */
    public Term getPrimalTargetPlaceInDirective(Term directive) {
        return null ;
    }

    //------------------------------------------------------------
    /**
     * Directive のなかの代表的目的地の取得
     * @param directive : 調べる directive。通常の place tag の場合もある。
     *    もし directive が isKnownDirective() なら、なにか返すべき。
     * @return もし directive なら代表的目的地。そうでないなら null
     */
    public Term nakedTargetFromRoutePlan(RoutePlan workingRoutePlan) {
        Term subgoal = workingRoutePlan.top() ;
        if(isKnownDirective(subgoal)) {
            Term nakedSubgoal = getPrimalTargetPlaceInDirective(subgoal) ;
            if(nakedSubgoal != null) {
                return nakedSubgoal ;
            } else {
                return subgoal ;
            }
        } else {
            return subgoal ;
        }
    }

    //------------------------------------------------------------
    /**
     * directive に含まれる目的地タグの抽出
     * @return pushした数
     */
    public int pushPlaceTagInDirective(Term directive,
                                       ArrayList<Term> goalList) {
        int count = 0 ;
        if(isKnownDirective(directive)) {
            Term subgoal = getPrimalTargetPlaceInDirective(directive) ;
            if(subgoal != null) {
                goalList.add(subgoal) ;
                count++ ;
            } else {
                Itk.dbgWrn("A directive includes no subgoal.") ;
                Itk.dbgMsg("directive", directive) ;
            }
        }
        return count ;
    }

    //------------------------------------------------------------
    /* [2014.12.29 I.Noda]
     * 今後の拡張性のため、Route 上にある Atom 以外の Term はすべて
     * Directive とみなす。（つまり、Atom (String) のみを経由地点の tag
     * と扱うことにする。
     */
    // planned_route の残り経路がすべて WAIT_FOR/WAIT_UNTIL ならば true を返す
    public boolean isRestAllRouteDirective() {
        if (isPlannedRouteCompleted()) {
            return false;
        }

        int delta = 0 ;
        while (delta < routePlan.length()) {
            Term candidate = routePlan.top(delta);
            if(!isDirectiveTerm(candidate)) {
                return false ;
            }
            delta += 1 ;
        }
        return true;
    }

    //############################################################
    /**
     * 入出力関係
     */
    //------------------------------------------------------------
    /**
     * 表示用
     */
    abstract public void draw(Graphics2D g, boolean experiment);

    //------------------------------------------------------------
    /**
     * Agent については、fromDom はサポートしない
     */
    public static OBNode fromDom(Element element) {
        Itk.dbgErr("fromDom() is not supported.") ;
        return null ;
    }

    //------------------------------------------------------------
    /**
     * おそらく、OBNode 汎用のルーチン。
     */
    public final static String getNodeTypeString() {
        return "Agent";
    }
    
    //------------------------------------------------------------
    /**
     * 現状をダンプ
     */
    abstract public void dumpResult(PrintStream out);
    
    //------------------------------------------------------------
    /**
     * 文字列化
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer() ;
        buffer.append(this.getClass().getSimpleName()) ;
        buffer.append("[") ;
        buffer.append("id:").append(ID) ;
        buffer.append("]") ;
        return buffer.toString() ;
    };

    //------------------------------------------------------------
    /**
     * リンク上の表示上の横ずれ幅計算
     */
    public Vector3d getSwing() {
        MapLink currentLink = currentPlace.getLink() ;
        if (null == currentLink) {
            return new Vector3d(0, 0, 0);
        }

        double scale = ((MapPartGroup)(currentLink.getParent())).getScale();
        double fwidth = currentLink.width / 2 / scale;
        double x1 = currentLink.getFrom().getX();
        double x2 = currentLink.getTo().getX();
        double y1 = currentLink.getFrom().getY();
        double y2 = currentLink.getTo().getY();

        Vector3d v1 = new Vector3d(x2 - x1, y2-y1, 0);
        v1.normalize();
        Vector3d v2 = new Vector3d(0, 0, fwidth * swing_width);
        Vector3d v3 = new Vector3d();
        v3.cross(v1, v2);
        return v3;
    }

    //------------------------------------------------------------
    /**
     * 表示上の位置計算
     */
    public Point2D getPos() {
        return currentPlace.getPosForDisplay() ;
    }

    //------------------------------------------------------------
    /**
     * 高さ？
     */
    public double getHeight() {
        return currentPlace.getHeightForDisplay() ;
    }
    
    //------------------------------------------------------------
    /**
     * Setting the agent's attributes
     */
    public static void showAttributeDialog(NetworkMap networkMap,
            ArrayList<AgentBase> agents) {
        /* Set attributes with a dialog */
        class AttributeSetDialog  extends JDialog implements ActionListener {
            private static final long serialVersionUID = -5975770541398630L;

            private NetworkMap networkMap;
            private ArrayList<AgentBase> agents;
            private JTextField[] textFields; 

            public AttributeSetDialog(NetworkMap _networkMap,
                    ArrayList<AgentBase> _agents) {
                super();

                networkMap = _networkMap;
                this.setModal(true);
                agents = _agents;

                int count = 0;
                for (AgentBase agent : agents) {
                    if (agent.selected) {
                        ++count;
                    }
                }
                if (count == 0) return;
                setUpPanel();
            }
            
            JComboBox target;
            private void setUpPanel() {
                Container contentPane = getContentPane();

                JPanel panel = null;

                textFields = new JTextField[1];

                /* labels and text fields */
                panel = new JPanel(new GridLayout(1, 2));

                panel.add(new JLabel("Type:"));
                target = new JComboBox(networkMap.getAllTags().toArray());
                panel.add(target);
                contentPane.add(panel, BorderLayout.NORTH);

                /* ok and cancel button */
                textFields[0] = new JTextField("type");
                panel = new JPanel(new GridLayout(1, 3));
                panel.add(new JLabel());
                JButton ok = new JButton("OK");
                ok.addActionListener(this);
                panel.add(ok);
                JButton cancel = new JButton("Cancel");
                cancel.addActionListener(this);
                panel.add(cancel);

                contentPane.add(panel, BorderLayout.SOUTH);
                this.pack();
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("OK")) {
                    final String goalString = (String)target.getSelectedItem();
                    for (AgentBase agent : agents) {
                        if (agent.selected)
                            agent.setGoal(new Term(goalString));
                    }
                    this.dispose();
                } else if (e.getActionCommand().equals("Cancel")) {
                    this.dispose();
                }
            }

        }
        AttributeSetDialog dialog = new AttributeSetDialog(networkMap, agents);
        dialog.setVisible(true);
    }

    //------------------------------------------------------------
    /**
     * ルート表示(for editor)
     */
    public static void showRouteDialog(NetworkMap networkMap,
            ArrayList<AgentBase> agents) {
        /* Set attributes with a dialog */
        class AttributeSetDialog  extends JDialog implements ActionListener {
            private static final long serialVersionUID = -6560704811897168475L;

            private ArrayList<AgentBase> agents;

            public AttributeSetDialog(NetworkMap networkMap,
                    ArrayList<AgentBase> _agents) {
                super();

                this.setModal(true);
                agents = _agents;

                int count = 0;
                for (AgentBase agent : agents) {
                    if (agent.selected) {
                        ++count;
                    }
                }
                if (count == 0) return;
                setUpPanel(networkMap);
            }
            
            JComboBox[] routes;
            private void setUpPanel(NetworkMap networkMap) {
                Container contentPane = getContentPane();

                String route_length_str = JOptionPane.showInputDialog("Route length?");
                if (route_length_str == null) {
                    this.dispose();
                    return;
                }

                int route_length = Integer.parseInt(route_length_str);
                JPanel panel = null;

                routes = new JComboBox[route_length];

                /* labels and text fields */
                panel = new JPanel(new GridLayout(route_length, 2));

                for (int i = 0; i < route_length; ++i) {
                    panel.add(new JLabel("Via " + i));
                    routes[i] = new JComboBox(networkMap.getAllTags().toArray());
                    panel.add(routes[i]);
                }
                contentPane.add(panel, BorderLayout.NORTH);

                /* ok and cancel button */
                panel = new JPanel(new GridLayout(1, 3));
                panel.add(new JLabel());
                JButton ok = new JButton("OK");
                ok.addActionListener(this);
                panel.add(ok);
                JButton cancel = new JButton("Cancel");
                cancel.addActionListener(this);
                panel.add(cancel);

                contentPane.add(panel, BorderLayout.SOUTH);
                this.pack();
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("OK")) {
                    ArrayList<Term> planned_route = new ArrayList<Term>();
                    for (int i = 0; i < routes.length; i++) {
                        Term tag = new Term((String)routes[i].getSelectedItem());
                        planned_route.add(tag) ;
                    }
                    for (AgentBase agent : agents) {
                        if (agent.selected)
                            agent.setPlannedRoute(planned_route);
                    }
                    this.dispose();
                } else if (e.getActionCommand().equals("Cancel")) {
                    this.dispose();
                }
            }

        }
        AttributeSetDialog dialog = new AttributeSetDialog(networkMap, agents);
        dialog.setVisible(true);
    }

    //------------------------------------------------------------
    /**
     * ???
     */
    abstract public JPanel paramSettingPanel(NetworkMap networkMap);

}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
