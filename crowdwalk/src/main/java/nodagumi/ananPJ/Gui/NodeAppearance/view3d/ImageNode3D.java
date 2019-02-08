package nodagumi.ananPJ.Gui.NodeAppearance.view3d;

import java.io.File;
import java.util.HashMap;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.TriangleMesh;

import nodagumi.ananPJ.Gui.SimulationPanel3D;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;

/**
 * 画像ノードの 3D 表示.
 *
 * width * height サイズの長方形を形作る三角形メッシュに画像テクスチャを貼り付ける
 */
public class ImageNode3D extends NodeViewBase3D {
    private final static float[] TEX_COORDS = {
        0, 0,
        0, 1,
        1, 1,
        1, 0
    };

    private final static int[] FACES = {
        0, 0, 1, 1, 2, 2,
        2, 2, 3, 3, 0, 0
    };

    /**
     * 画像の頂点コンポーネントが定義された三角形メッシュ
     */
    protected TriangleMesh mesh;

    /**
     * コンストラクタ
     */
    public ImageNode3D() {}

    /**
     * 初期設定
     */
    public void init(SimulationPanel3D panel, HashMap parameters, NodeAppearance3D nodeAppearance) throws Exception {
        super.init(panel, parameters, nodeAppearance);

        String fileName = getStringParameter("fileName", null);
        if (fileName == null) {
            throw new Exception("Node appearance error: Image file name is not specified with ImageNode3D.");
        }
        String filePath = panel.getPropertiesHandler().furnishPropertiesDirPath(fileName, true, false);
        Image image = new Image(new File(filePath).toURI().toString());
        material.setDiffuseColor(Color.WHITE);
        material.setDiffuseMap(image);

        double width = getDoubleParameter("width", 0.0);
        double height = getDoubleParameter("height", 0.0);
        if (width <= 0.0 || height <= 0.0) {
            throw new Exception("Node appearance error: width or height is not specified correctly with ImageNode3D.");
        }

        Point2D[] vertices = new Point2D[4];
        vertices[0] = new Point2D(0.0, 0.0);
        vertices[1] = new Point2D(0.0, height);
        vertices[2] = new Point2D(width, height);
        vertices[3] = new Point2D(width, 0.0);

        float[] points = new float[4 * 3];
        for (int index = 0; index < 4; index++) {
            points[index * 3] = (float)(vertices[index].getX() - width / 2.0);
            points[index * 3 + 1] = (float)(vertices[index].getY() - height / 2.0);
            points[index * 3 + 2] = 0.0f;
        }

        mesh = new TriangleMesh();
        mesh.getPoints().setAll(points);
        mesh.getTexCoords().setAll(TEX_COORDS);
        mesh.getFaces().setAll(FACES);
    }

    /**
     * ノードのシェイプを生成する
     */
    public Shape3D[] createShapes(MapNode node) {
        Shape3D shape = new MeshView(mesh);
        shape.setMaterial(material);
        java.awt.geom.Point2D pos = node.getPosition();
        shape.setTranslateX(pos.getX());
        shape.setTranslateY(pos.getY());
        shape.setTranslateZ(-node.getHeight() * panel.getVerticalScale());

        Shape3D[] shapes = { shape };
        return shapes;
    }
}
