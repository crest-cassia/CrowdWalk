package nodagumi.ananPJ.Gui.LinkAppearance;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nodagumi.ananPJ.CrowdWalkLauncher;
import nodagumi.ananPJ.Gui.LinkAppearance.EdgePoints;
import nodagumi.ananPJ.misc.JsonicHashMapGetter;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.Simulator.Obstructer.ObstructerBase;
import nodagumi.Itk.*;

/**
 * リンク表示の定義情報を扱うベースクラス
 */
public class LinkAppearanceBase extends JsonicHashMapGetter {
    /**
     * リンクの表示スタイルを決めるクラスの一覧
     */
    public static final String CLASSES = "/link_appearance_classes.json";

    /**
     * クラスローダー
     */
    protected ClassFinder classFinder = new ClassFinder();

    /**
     * このスタイルを適用するリンクのタグ
     */
    protected String tag;

    /**
     * タグが正規表現だった場合の正規表現パターン
     */
    protected Pattern tagPattern = null;

    /**
     * リンクを 2D 描画するクラスの名前
     */
    protected String viewClassName2D;

    /**
     * リンクを 2D 描画するクラスの初期化パラメータ
     */
    protected HashMap viewParameters2D;

    /**
     * リンクを 3D 描画するクラスの名前
     */
    protected String viewClassName3D;

    /**
     * リンクを 3D 描画するクラスの初期化パラメータ
     */
    protected HashMap viewParameters3D;

    /**
     * リンクの縁取りラインを生成するための座標
     */
    protected EdgePoints edgePoints;

    /**
     * コンストラクタ
     */
    public LinkAppearanceBase(HashMap parameters, EdgePoints edgePoints) {
        this.edgePoints = edgePoints;
        classFinder.aliasByJson(ObstructerBase.resourceToString(CLASSES));

        setParameters(parameters);
        try {
            // 適用対象となるリンクタグ
            tag = getStringParameter("tag", "*");
            Matcher matcher = Pattern.compile("^\\/(.*)\\/$").matcher(tag);
            // タグが正規表現か?
            if (matcher.find()) {
                tagPattern = Pattern.compile(matcher.group(1));
            }

            // LinkViewBase2D インスタンス生成用の情報を取得
            HashMap<String, Object> view2DPart = getHashMapParameter("2D_View", null);
            if (view2DPart != null) {
                viewClassName2D = (String)view2DPart.get("className");
                if (viewClassName2D == null) {
                    throw new Exception("Link appearance file format error.");
                }
                if (! isUsableClass(viewClassName2D)) {
                    throw new Exception("Link appearance file error: invalid className - " + viewClassName2D);
                }
                viewParameters2D = (HashMap<String, Object>)view2DPart.get("parameters");
            }

            // LinkViewBase3D インスタンス生成用の情報を取得
            HashMap<String, Object> view3DPart = getHashMapParameter("3D_View", null);
            if (view3DPart != null && CrowdWalkLauncher.isUsable3dSimulator()) {
                viewClassName3D = (String)view3DPart.get("className");
                if (viewClassName3D == null) {
                    throw new Exception("Link appearance file format error.");
                }
                if (! isUsableClass(viewClassName3D)) {
                    throw new Exception("Link appearance file error: invalid className - " + viewClassName3D);
                }
                viewParameters3D = (HashMap<String, Object>)view3DPart.get("parameters");
            }
        } catch (Exception e) {
            Itk.quitWithStackTrace(e);
        }
    }

    /**
     * 使用可能なクラスか?
     */
    protected boolean isUsableClass(String className) {
        return classFinder.isClassName(className);
    }

    public String getTag() {
        return tag;
    }

    /**
     * タグに該当する文字列か?
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
     */
    public boolean isValidFor2D() {
        return viewClassName2D != null;
    }

    /**
     * 3D View に使用可能
     */
    public boolean isValidFor3D() {
        return viewClassName3D != null;
    }

    /**
     * リンクの縁取りラインを生成するための座標を取得する
     */
    public EdgePoints.Points getEdgePoints(MapLink link) {
        return edgePoints.getEdgePoints(link);
    }
}
