package nodagumi.ananPJ.Gui.LinkAppearance.view2d;

import java.awt.geom.Line2D;
import java.util.HashMap;

import nodagumi.ananPJ.Gui.LinkAppearance.EdgePoints;
import nodagumi.ananPJ.Gui.LinkAppearance.LinkAppearanceBase;
import nodagumi.ananPJ.Gui.SimulationPanel2D;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;

import nodagumi.Itk.Itk;

/**
 * リンク 2D 表示の定義情報を扱う
 */
public class LinkAppearance2D extends LinkAppearanceBase {
    /**
     * リンクを 2D 描画するオブジェクト
     */
    private LinkViewBase2D view;

    /**
     * 2D シミュレーションパネル
     */
    private SimulationPanel2D panel;

    /**
     * リンクの Line2D を保持する
     */
    private HashMap<MapLink, Line2D[]> linkLines;

    /**
     * コンストラクタ
     */
    public LinkAppearance2D(SimulationPanel2D panel, HashMap parameters, EdgePoints edgePoints, HashMap<MapLink, Line2D[]> linkLines) {
        super(parameters, edgePoints);

        if (isValidFor2D()) {
            this.panel = panel;
            this.linkLines = linkLines;
            try {
                view = (LinkViewBase2D)classFinder.newByName(viewClassName2D);
                view.init(panel, viewParameters2D, this);
            } catch (Exception e) {
                Itk.quitWithStackTrace(e);
            }
        }
    }

    /**
     * 描画クラスのインスタンスを取得する
     */
    public final LinkViewBase2D getView() {
        return view;
    }

    /**
     * リンクの Line2D を取得する
     */
    public final Line2D[] getLinkLines(MapLink link) {
        return linkLines.get(link);
    }

    /**
     * リンクの Line2D を更新する
     */
    public final void update(MapLink link) {
        linkLines.put(link, view.createLinkLines(link));
    }
}
