// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Agents;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.io.PrintStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import javax.vecmath.Vector3d;

import org.w3c.dom.Element;

import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.OBMapPart;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Link.MapLink.Direction;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.Agents.AgentFactory;
import nodagumi.ananPJ.misc.RoutePlan ;
import nodagumi.ananPJ.misc.Place ;
import nodagumi.ananPJ.misc.SetupFileInfo;
import nodagumi.ananPJ.misc.SimTime;
import nodagumi.ananPJ.Simulator.EvacuationSimulator;
import nodagumi.ananPJ.Simulator.Obstructer.ObstructerBase;
import nodagumi.ananPJ.Simulator.Obstructer.ObstructerBase.TriageLevel;


import nodagumi.Itk.* ;

//======================================================================
/**
 * エージェントのベース。抽象クラス。
 */
public abstract class AgentBase extends OBMapPart
implements Comparable<AgentBase> {

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Agent の詳細設定情報を格納しているもの
     */
    public Term config ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * generatedTime: 生成された時刻
     * finishedTime: ゴールに到達した時刻
     * evacuated: ゴールに到達したかどうかのフラグ
     * stuck: スタックしたかどうかのフラグ(スタックしたら evacuated も true となる)
     */
    public SimTime generatedTime = null;
    public SimTime finishedTime = null;
    private boolean evacuated = false;
    private boolean stuck = false;
    public SimTime currentTime = null ;

    /**
     * 生成された時のリンク上の位置
     */
    public double generatedPosition = 0.0;

    /**
     * 死亡した時のリンク上の位置
     */
    public double diedPosition = 0.0;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * map: 自分の存在しているマップ。
     */
    protected NetworkMap map = null ;

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
     * accel: 加速度
     */
    protected double speed;
    protected double accel = 0.0;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * obstructerType: 使用する Obstructer のクラス名
     * obstructer: Obstructer による影響の処理を行う部分。
     */
    protected static String obstructerType = "Flood";
    public ObstructerBase obstructer ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 設定関係
     */

    /**
     * 設定文字列（generation file 中の設定情報の文字列）
     */
    protected String configLine = "none";

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 乱数生成器
     */
    protected Random random = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

    /**
     * リンク上の表示上の横ずれ幅の量
     */
    protected double swing_width;

    /**
     * リンク上の表示上の横ずれ幅の座標差分
     */
    protected Vector3d swing = new Vector3d(0, 0, 0);

    /**
     * 直前の位置。
     * AgentHandler の表示制御あたりで使用。
     */
    protected Point2D lastPosition = null ;

    /**
     * 直前のswing。
     * AgentHandler の表示制御あたりで使用。
     */
    protected Vector3d lastSwing = null ;

    //############################################################
    /**
     * 初期化関係
     */
    //------------------------------------------------------------
    /**
     * constractors
     * 引数なしconstractorはClassFinder.newByName で必要。
     */
    public AgentBase() {}

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
    public void init(Random _random, EvacuationSimulator simulator, 
                     AgentFactory factory, SimTime currentTime) {
        super.init(null);
        random = _random;
        //swing_width = Math.random() * 2.0 - 1.0;
        swing_width = random.nextDouble() * 2.0 - 1.0;
        calcSwing();
        // ObstructerBase のサブクラスのインスタンスを取得
        obstructer = ObstructerBase.createAndInitialize(obstructerType, this) ;
        //AgentFactory から移したもの
        generatedTime = currentTime ;
        setMap(simulator.getMap()) ;
        setConfigLine(factory.configLine) ;
        // set route
        setGoal(new Term(factory.goal));
        Term planned_route_in_Term =
            (factory.planned_route == null ?
             Term.newArrayTerm() :
             new Term(new ArrayList<Term>(factory.planned_route))) ;
        setPlannedRoute((List)planned_route_in_Term.getArray());
        // tag
        for (final String tag : factory.tags) {
            addTag(tag);
        }
    }

    //------------------------------------------------------------
    /**
     * Conf による初期化。
     * 継承しているクラスの設定のため。
     * @param conf json の連想配列形式を scan した Map
     */
    public void initByConf(Term conf, Term fallback){
        if(conf != null) {
            config = conf ;
        } else {
            config = new Term() ;
        }
        if(fallback != null) {
            SetupFileInfo.attachFallback(config, fallback) ;
        }
    } ;

    //------------------------------------------------------------
    /**
     * Conf からの値の取得(double)
     */
    public double getDoubleFromConfig(String slot, double fallback) {
        return SetupFileInfo.fetchFallbackDouble(config, slot, fallback) ;
    }

    //------------------------------------------------------------
    /**
     * Conf からの値の取得(double)
     */
    public double getIntFromConfig(String slot, int fallback) {
        return SetupFileInfo.fetchFallbackInt(config, slot, fallback) ;
    }

    //------------------------------------------------------------
    /**
     * Conf からの値の取得(Term)
     */
    public Term getTermFromConfig(String slot, Term fallback) {
        return SetupFileInfo.fetchFallbackTerm(config, slot, fallback) ;
    }

    //------------------------------------------------------------
    /**
     * Conf からの値の取得(String)
     */
    public String getStringFromConfig(String slot, String fallback) {
        return SetupFileInfo.fetchFallbackString(config, slot, fallback) ;
    }

    //############################################################
    /**
     * インスタンス変数へのアクセス
     */

    //------------------------------------------------------------
    /**
     * マップをセット
     */
    public void setMap(NetworkMap _map) {
        map = _map ;
    }

    //------------------------------------------------------------
    /**
     * 避難完了をセット
     */
    public void setEvacuated(boolean evacuated, SimTime evacuateTime,
                             boolean stuck) {
        this.evacuated = evacuated;
        this.finishedTime = evacuateTime ;
        this.stuck = stuck;
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
     * スタックしたかどうか
     */
    public boolean isStuck() {
        return stuck;
    }

    //------------------------------------------------------------
    /**
     * 死亡したかどうか
     */
    public boolean isDead() {
        return obstructer.isDead() ;
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
     * 全経路をリセット
     */
    public void clearPlannedRoute() {
        setPlannedRoute(new ArrayList<Term>(), true) ;
    }

    //------------------------------------------------------------
    /**
     * 全経路をセット
     */
    public void setPlannedRoute(List<Term> _planned_route) {
        setPlannedRoute(_planned_route, false) ;
    }

    //------------------------------------------------------------
    /**
     * 全経路をセット
     * @param _planned_route : セットするルート(tag の配列)
     * @param resetIndexP : index もリセットするかどうか。
     */
    public void setPlannedRoute(List<Term> _planned_route,
                                boolean resetIndexP) {
        routePlan.setRoute(_planned_route) ;
        if(resetIndexP) routePlan.resetIndex() ;
    }

    //------------------------------------------------------------
    /**
     * 全経路をセット
     */
    public void insertRouteTagSafely(Term tag) {
        routePlan.insertSafely(tag) ;
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
    public Direction getDirection() {
        return currentPlace.getDirection() ;
    }

    //------------------------------------------------------------
    /**
     * 現在のリンクに対する向きを取得
     */
    public Direction getLastDirection() {
        return lastPlace.getDirection() ;
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
        return accel;
    }

    //------------------------------------------------------------
    /**
     * ???
     */
    public static void setObstructerType(String s) {
        obstructerType = s;
    }
    //------------------------------------------------------------
    /**
     * トリアージレベル
     */
    public TriageLevel getTriage() {
        return obstructer.getTriage() ;
    }

    //------------------------------------------------------------
    /**
     * トリアージレベル（数値）
     */
    public int getTriageInt() {
        return obstructer.getTriageInt() ;
    }

    //------------------------------------------------------------
    /**
     * トリアージレベル（文字列）
     */
    public String getTriageName() {
        return obstructer.getTriageName() ;
    }

    //------------------------------------------------------------
    /**
     * 動作が終了(避難完了または死亡)しているか?
     */
    public boolean finished() {
        return isEvacuated() || isDead();
    }

    //------------------------------------------------------------
    /**
     * 汚染環境(洪水、ガス等)に暴露する
     */
    public void exposed(double c) {
        if (isDead()) {
            return;
        }
        obstructer.expose(c);
        if (isDead()) {
            diedPosition = getAdvancingDistance();
        }
    }

    //------------------------------------------------------------
    /**
     * 設定文字列（generation file 中の設定情報の文字列）を取得
     */
    public String getConfigLine() {
        return configLine;
    }

    //------------------------------------------------------------
    /**
     * 設定文字列（generation file 中の設定情報の文字列）を格納
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

    //------------------------------------------------------------
    /**
     * 乱数生成(実数)
     */
    public double getRandomDouble() {
        return random.nextDouble() ;
    }

    //------------------------------------------------------------
    /**
     * 乱数生成(整数)
     */
    public int getRandomInt() {
        return random.nextInt() ;
    }

    //------------------------------------------------------------
    /**
     * 乱数生成(整数) 上限有り
     */
    public int getRandomInt(int n) {
        return random.nextInt(n) ;
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
        if (isPlannedRouteCompleted()) {
            return "";
        } else {
            Term term = routePlan.top();
            if (term.isString()) {
                return term.getHeadString();
            } else if (term.isNull()) {
                return "";
            } else {
                Term target = (Term)term.getArg("target");
                if (target == null) {
                    return "";
                } else {
                    return target.getHeadString();
                }
            }
        }
    }

    //------------------------------------------------------------
    /**
     * evacuation の完了
     * @param currentTime : 時刻
     * @param onNode : true なら currentPlace.getHeadingNode() 上
     *                 false なら currentPlace.getLink() 上
     * @param stuck : スタックかどうか
     */
    protected void finalizeEvacuation(SimTime currentTime,
                                      boolean onNode, boolean stuck) {
        consumePlannedRoute() ;
        setEvacuated(true, currentTime, stuck) ;
        if(currentPlace.isWalking()) {
            lastPlace.set(currentPlace) ;
            currentPlace.getLink().agentExits(this) ;
            currentPlace.quitLastLink() ;
        }
        if (networkMap != null) {
            networkMap.getNotifier().agentEvacuated(this);
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
    abstract public void prepareForSimulation() ;

    //------------------------------------------------------------
    /**
     * シミュレーション各サイクルの前半に呼ばれる。
     */
    public void preUpdate(SimTime currentTime) {
        this.currentTime = currentTime ;
    }

    //------------------------------------------------------------
    /**
     * シミュレーション各サイクルの後半に呼ばれる。
     */
    abstract public boolean update(SimTime currentTime) ;

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
     * sort, binarySearch 用比較関数
     * エージェントのリンク上の進み具合で比較。
     * 逆向きなどはちゃんと方向を直して扱う。
     */
    public int compareTo(AgentBase rhs) {
        if(this == rhs) return 0 ;

        double h1 = this.currentPlace.getAdvancingDistance() ;
        double h2 = rhs.currentPlace.getAdvancingDistance() ;

        if(h1 > h2) {
            return 1 ;
        } else if(h1 < h2) {
            return -1 ;
        } else {
            return ID.compareTo(rhs.ID) ;
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
     * ルート のなかの代表的目的地の取得
     * @param workingRoutePlan : 調べるルート。通常の place tag の場合もある。
     *    もし directive が isKnownDirective() なら、なにか返すべき。(???)
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
                Itk.logWarn("A directive includes no subgoal.") ;
                Itk.logWarn_("directive", directive) ;
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
     * Alert 関係
     * RationalAgent 以下でないと意味がない
     */
    public void alertMessage(Term message, SimTime currentTime) {
        // do nothing ;
    }
    //############################################################
    /**
     * 入出力関係
     */

    //------------------------------------------------------------
    /**
     * 表示用
     */
    abstract public void drawInEditor(Graphics2D g) ;

    //------------------------------------------------------------
    /**
     * Agent については、fromDom はサポートしない
     */
    public static OBNode fromDom(Element element) {
        Itk.logError("fromDom() is not supported.") ;
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
    protected void calcSwing() {
        MapLink currentLink = currentPlace.getLink() ;
        if (null == currentLink) {
            swing = new Vector3d(0, 0, 0);
            return;
        }

        double scale = ((MapPartGroup)(currentLink.getParent())).getScale();
        double fwidth = currentLink.getWidth() / 2 / scale;
        double x1 = currentLink.getFrom().getX();
        double x2 = currentLink.getTo().getX();
        double y1 = currentLink.getFrom().getY();
        double y2 = currentLink.getTo().getY();

        Vector3d v1 = new Vector3d(x2 - x1, y2-y1, 0);
        v1.normalize();
        Vector3d v2 = new Vector3d(0, 0, fwidth * swing_width);
        swing = new Vector3d();     // TODO: この行は不要かもしれない
        swing.cross(v1, v2);
    }

    /**
     * swing 値の取得
     */
    public Vector3d getSwing() {
        return swing;
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

    /**
     * WAIT_FOR/WAIT_UNTIL 処理中か?
     */
    public boolean isWaiting() {
        return false;
    }

    /**
     * lastPosition を更新する
     *
     * @return 値が変化したら true、変わらなければ false
     */
    public boolean updateLastPosition() {
        Point2D currentPosition = getPos();
        if (lastPosition == null || ! currentPosition.equals(lastPosition)) {
            lastPosition = currentPosition;
            return true;
        }
        return false;
    }

    /**
     * lastSwing を更新する
     *
     * @return 値が変化したら true、変わらなければ false
     */
    public boolean updateLastSwing() {
        if (lastSwing == null || ! swing.equals(lastSwing)) {
            lastSwing = swing;
            return true;
        }
        return false;
    }
}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
