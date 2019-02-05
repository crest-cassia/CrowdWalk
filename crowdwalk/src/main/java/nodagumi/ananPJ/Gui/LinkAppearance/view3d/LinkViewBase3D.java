package nodagumi.ananPJ.Gui.LinkAppearance.view3d;

import java.util.HashMap;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;

import nodagumi.ananPJ.Gui.FxColor;
import nodagumi.ananPJ.Gui.LinkAppearance.EdgePoints;
import nodagumi.ananPJ.Gui.SimulationPanel3D;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.ananPJ.misc.JsonicHashMapGetter;

/**
 * リンク 3D 描画のベースクラス
 */
public abstract class LinkViewBase3D extends JsonicHashMapGetter {
    /**
     * リンクの描画方法
     */
    public enum Method {
        FILLING,
        BORDERING
    };

    /**
     * リンクを擬似的な1ドット幅のラインで表示する時の幅(1mm)
     */
    public static double THIN_LINK_WIDTH = 0.001;

    /**
     * ピッキング用マテリアル
     */
    private static PhongMaterial pickingMaterial;

    /**
     * 3D シミュレーションパネル
     */
    protected SimulationPanel3D panel;

    /**
     * リンクの 3D 表示の定義情報
     */
    protected LinkAppearance3D linkAppearance;

    /**
     * リンクのマテリアル
     */
    protected PhongMaterial material;

    /**
     * リンクの描画方法
     */
    protected Method method = Method.FILLING;

    static {
        pickingMaterial = new PhongMaterial();
        pickingMaterial.setDiffuseColor(Color.TRANSPARENT);
    }

    /**
     * 初期設定
     */
    public void init(SimulationPanel3D panel, HashMap parameters, LinkAppearance3D linkAppearance) throws Exception {
        this.panel = panel;
        this.linkAppearance = linkAppearance;
        setParameters(parameters);

        String colorName = getStringParameter("color", "DEFAULT_LINK_COLOR");
        Color color = FxColor.getColor(colorName);
        if (color == null) {
            color = FxColor.DEFAULT_LINK_COLOR;
        }
        double transparency = getDoubleParameter("transparency", 0.0);
        color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 1.0 - transparency);

        material = new PhongMaterial();
        material.setDiffuseColor(color);

        String methodName = getStringParameter("method", method.name());
        method = Method.valueOf(methodName.toUpperCase());
    }

    /**
     * 最終的なリンクの幅を取得する
     */
    public abstract double getWidth(MapLink link);

    /**
     * リンクをポリゴン表示するための頂点座標を求める
     */
    public Point3D[] calcVertices(MapNode from, MapNode to, double width, double dz) {
        Point3D p1 = new Point3D(to.getX() - from.getX(), to.getY() - from.getY(), 0);
        Point3D p2 = p1.normalize().crossProduct(0, 0, width / 2.0);
        double dx = p2.getX();
        double dy = p2.getY();

        Point3D[] vertices = new Point3D[4];
        vertices[0] = new Point3D(from.getX() + dx, from.getY() + dy, -from.getHeight() + dz);
        vertices[1] = new Point3D(from.getX() - dx, from.getY() - dy, -from.getHeight() + dz);
        vertices[3] = new Point3D(to.getX() - dx, to.getY() - dy, -to.getHeight() + dz);
        vertices[2] = new Point3D(to.getX() + dx, to.getY() + dy, -to.getHeight() + dz);
        return vertices;
    }

    /**
     * リンクをポリゴン表示するための頂点座標を求める
     */
    public Point3D[] calcVertices(Point3D point1, Point3D point2, double width, double dz) {
        Point3D p1 = new Point3D(point2.getX() - point1.getX(), point2.getY() - point1.getY(), 0);
        Point3D p2 = p1.normalize().crossProduct(0, 0, width / 2.0);
        double dx = p2.getX();
        double dy = p2.getY();

        Point3D[] vertices = new Point3D[4];
        vertices[0] = new Point3D(point1.getX() + dx, point1.getY() + dy, -point1.getZ() + dz);
        vertices[1] = new Point3D(point1.getX() - dx, point1.getY() - dy, -point1.getZ() + dz);
        vertices[3] = new Point3D(point2.getX() - dx, point2.getY() - dy, -point2.getZ() + dz);
        vertices[2] = new Point3D(point2.getX() + dx, point2.getY() + dy, -point2.getZ() + dz);
        return vertices;
    }

    /**
     * 幅のある実線(中を塗りつぶした長方形シェイプ)を生成する
     */
    public Shape3D createSingleLine(MapLink link) {
        Point3D[] vertices = calcVertices(link.getFrom(), link.getTo(), getWidth(link), 0.0);
        Shape3D shape = new MeshView(panel.new QuadPolygon(vertices));
        shape.setDrawMode(DrawMode.FILL);
        shape.setMaterial(material);
        return shape;
    }

    /**
     * 実線(線分と見分けが付かない超鋭角の三角形シェイプ)を生成する
     */
    public Shape3D createSolidLine(Point3D point1, Point3D point2) {
        Point3D[] vertices = calcVertices(point1, point2, THIN_LINK_WIDTH, 0.0);
        Shape3D shape = new MeshView(panel.new TrianglePolygon(vertices));
        shape.setDrawMode(DrawMode.LINE);
        shape.setCullFace(CullFace.NONE);    // これがないと時々表示が消える
        shape.setMaterial(material);
        return shape;
    }

    /**
     * リンクの縁取りラインを生成する
     */
    public Shape3D[] createBorderLine(MapLink link) {
        EdgePoints.Points points = linkAppearance.getEdgePoints(link);
        if (points == null) {
            return null;
        }
        Point2D a1 = points.fromNodeLeftPoint;
        Point2D a2 = points.toNodeRightPoint;
        Point2D b1 = points.fromNodeRightPoint;
        Point2D b2 = points.toNodeLeftPoint;
        if (a1 == null || a2 == null || b1 == null || b2 == null) {
            return null;
        }

        Point3D point1 = new Point3D(a1.getX(), a1.getY(), link.getFrom().getHeight());
        Point3D point2 = new Point3D(a2.getX(), a2.getY(), link.getTo().getHeight());
        Shape3D borderLine1 = createSolidLine(point1, point2);

        Point3D point3 = new Point3D(b1.getX(), b1.getY(), link.getFrom().getHeight());
        Point3D point4 = new Point3D(b2.getX(), b2.getY(), link.getTo().getHeight());
        Shape3D borderLine2 = createSolidLine(point3, point4);

        Shape3D[] shapes = { borderLine1, borderLine2 };
        return shapes;
    }

    /**
     * リンクのシェイプを生成する
     */
    public Shape3D[] createShapes(MapLink link) {
        switch (method) {
        case FILLING:
            Shape3D[] shapes = { createSingleLine(link) };
            return shapes;
        case BORDERING:
            return createBorderLine(link);
        }
        return null;
    }

    /**
     * ピッキング用の透明シェイプを生成する
     */
    public Shape3D createPickingShape(MapLink link) {
        Point3D[] vertices = calcVertices(link.getFrom(), link.getTo(), link.getWidth(), -0.1); // 10cm 上方に配置する
        Shape3D shape = new MeshView(panel.new QuadPolygon(vertices));
        shape.setDrawMode(DrawMode.FILL);
        shape.setMaterial(pickingMaterial);
        return shape;
    }
}
