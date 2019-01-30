package nodagumi.ananPJ.Gui.LinkAppearance.view3d;

import javafx.geometry.Point3D;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;

import nodagumi.ananPJ.NetworkMap.Link.MapLink;

/**
 * 実線リンクの 3D 表示
 */
public class SolidLine3D extends LinkViewBase3D {
    /**
     * コンストラクタ
     */
    public SolidLine3D() {}

    /**
     * 最終的なリンクの幅を取得する
     */
    public double getWidth(MapLink link) {
        return THIN_LINK_WIDTH;
    }

    /**
     * 線分と見分けが付かない超鋭角の三角形シェイプを生成する
     */
    public Shape3D[] createShapes(MapLink link) {
        Point3D[] vertices = calcVertices(link.getFrom(), link.getTo(), THIN_LINK_WIDTH, 0.0);
        Shape3D shape = new MeshView(panel.new TrianglePolygon(vertices));
        shape.setDrawMode(DrawMode.LINE);
        shape.setCullFace(CullFace.NONE);    // これがないと時々表示が消える
        shape.setMaterial(material);

        Shape3D[] shapes = { shape };
        return shapes;
    }
}
