package nodagumi.ananPJ.Gui.AgentAppearance;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nodagumi.ananPJ.CrowdWalkLauncher;
import nodagumi.ananPJ.Gui.AgentAppearance.model.AgentAppearanceModel;
import nodagumi.ananPJ.GuiSimulationLauncher;
import nodagumi.ananPJ.misc.JsonicHashMapGetter;
import nodagumi.ananPJ.Simulator.Obstructer.ObstructerBase;
import nodagumi.Itk.*;

/**
 * エージェント表示の定義情報を扱うベースクラス。
 * <ul>
 * <li>設定方法: 
 * propaties_file の "agent_appearance_file" に、その定義ファイルを指定する。
 * </li>
 * <li>定義ファイルの形式は
 * <a target="_blank" href="https://github.com/crest-cassia/CrowdWalk/wiki/Agent-appearance-%E3%81%AE%E8%A8%AD%E5%AE%9A%E6%96%B9%E6%B3%95">こちら</a>を参照。
 * </li>
 * </ul>
 */
public class AgentAppearanceBase extends JsonicHashMapGetter {
    /**
     * エージェントの表示スタイルを決めるクラスの一覧
     */
    public static final String CLASSES = "/agent_appearance_classes.json";

    /**
     * クラスローダー
     */
    protected ClassFinder classFinder = new ClassFinder();

    /**
     * このスタイルを適用するエージェントのタグ
     */
    protected String tag;

    /**
     * タグが正規表現だった場合の正規表現パターン
     */
    protected Pattern tagPattern = null;

    /**
     * エージェントの表現値を計算するクラスのインスタンス
     */
    protected AgentAppearanceModel model;

    /**
     * エージェントの表現値を計算するクラスの初期化パラメータ
     */
    protected HashMap modelParameters;

    /**
     * エージェントを 2D 描画するクラスの名前
     */
    protected String viewClassName2D;

    /**
     * エージェントを 2D 描画するクラスの初期化パラメータ
     */
    protected HashMap viewParameters2D;

    /**
     * エージェントを 3D 描画するクラスの名前
     */
    protected String viewClassName3D;

    /**
     * エージェントを 3D 描画するクラスの初期化パラメータ
     */
    protected HashMap viewParameters3D;

    /**
     * コンストラクタ
     * @param launcher: GuiSimulationLauncher。
     * @param parameters: パラメータを格納したHashMap。
     */
    public AgentAppearanceBase(GuiSimulationLauncher launcher, HashMap parameters) {
        classFinder.aliasByJson(ObstructerBase.resourceToString(CLASSES));

        setParameters(parameters);
        try {
            // 適用対象となるエージェントタグ
            tag = getStringParameter("tag", "*");
            Matcher matcher = Pattern.compile("^\\/(.*)\\/$").matcher(tag);
            // タグが正規表現か?
            if (matcher.find()) {
                tagPattern = Pattern.compile(matcher.group(1));
            }

            // AgentAppearanceModel インスタンスの生成と初期設定
            HashMap<String, Object> modelPart = getHashMapParameter("model", null);
            if (modelPart == null) {
                throw new Exception("Agent appearance file format error.");
            }
            String modelClassName = (String)modelPart.get("className");
            if (modelClassName == null) {
                throw new Exception("Agent appearance file format error.");
            }
            if (! isUsableClass(modelClassName)) {
                throw new Exception("Agent appearance file error: invalid className - " + modelClassName);
            }
            modelParameters = (HashMap<String, Object>)modelPart.get("parameters");
            model = (AgentAppearanceModel)classFinder.newByName(modelClassName);
            model._init(launcher, modelParameters);
            model.init();

            // AgentViewBase2D インスタンス生成用の情報を取得
            HashMap<String, Object> view2DPart = getHashMapParameter("2D_View", null);
            if (view2DPart != null) {
                viewClassName2D = (String)view2DPart.get("className");
                if (viewClassName2D == null) {
                    throw new Exception("Agent appearance file format error.");
                }
                if (! isUsableClass(viewClassName2D)) {
                    throw new Exception("Agent appearance file error: invalid className - " + viewClassName2D);
                }
                viewParameters2D = (HashMap<String, Object>)view2DPart.get("parameters");
            }

            // AgentViewBase3D インスタンス生成用の情報を取得
            HashMap<String, Object> view3DPart = getHashMapParameter("3D_View", null);
            if (view3DPart != null && CrowdWalkLauncher.isUsable3dSimulator()) {
                viewClassName3D = (String)view3DPart.get("className");
                if (viewClassName3D == null) {
                    throw new Exception("Agent appearance file format error.");
                }
                if (! isUsableClass(viewClassName3D)) {
                    throw new Exception("Agent appearance file error: invalid className - " + viewClassName3D);
                }
                viewParameters3D = (HashMap<String, Object>)view3DPart.get("parameters");
            }
        } catch (Exception e) {
	    Itk.quitWithStackTrace(e) ;
        }
    }

    /**
     * 使用可能なクラスか?
     * @param className: チェックするクラス名。
     * @return 使用可能ならtrue。
     */
    protected boolean isUsableClass(String className) {
        return classFinder.isClassName(className);
    }

    /**
     * タグを取得。
     * @return タグ。
     */
    public String getTag() {
        return tag;
    }

    /**
     * モデルを取得。
     * @return モデル。
     */
    public AgentAppearanceModel getModel() {
        return model;
    }

    /**
     * タグに該当する文字列か?
     * @param text: textがタグに含まれるか。
     * @return 含まれれば true。
     */
    public boolean isTagApplied(String text) {
        if (tagPattern == null) {
            if (tag.equals("*") || text.indexOf(tag) != -1) {
                return true;
            }
        } else {
            return tagPattern.matcher(text).find();
        }
        return false;
    }

    /**
     * 2D View に使用可能
     * @return 使用可能なら true。
     */
    public boolean isValidFor2D() {
        return viewClassName2D != null;
    }

    /**
     * 3D View に使用可能
     * @return 使用可能なら true。
     */
    public boolean isValidFor3D() {
        return viewClassName3D != null;
    }
}
