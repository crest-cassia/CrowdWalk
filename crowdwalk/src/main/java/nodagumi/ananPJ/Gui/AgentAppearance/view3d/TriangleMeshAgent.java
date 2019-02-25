package nodagumi.ananPJ.Gui.AgentAppearance.view3d;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;

import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;

import nodagumi.ananPJ.Agents.AgentBase;

/**
 * ユーザー定義のポリゴンエージェントの 3D 表示
 */
public class TriangleMeshAgent extends SphereAgent {
    /**
     * 頂点座標
     */
    protected float[] points;

    /**
     * faces
     */
    protected int[] faces;

    /**
     * 描画モード
     */
    protected DrawMode drawMode = DrawMode.FILL;

    /**
     * ポリゴンに加える基本回転の角度
     */
    protected double rotationAngle = 0.0;

    /**
     * ポリゴンに加える基本回転の回転軸
     */
    protected Point3D rotationAxis = Rotate.Z_AXIS;

    /**
     * エージェントと Rotate オブジェクトの対応表
     */
    protected HashMap<AgentBase, Rotate> rotations = new HashMap();

    /**
     * 初期設定 2
     */
    public void init() throws Exception {
        ArrayList<BigDecimal> points = getBigDecimalArrayList("points", null);
        if (points == null) {
            throw new Exception("Agent appearance error: points is not specified with TriangleMeshAgent.");
        }
        this.points = new float[points.size()];
        for (int index = 0; index < points.size(); index++) {
            this.points[index] = points.get(index).floatValue();
        }

        ArrayList<BigDecimal> faces = getBigDecimalArrayList("faces", null);
        if (faces == null) {
            throw new Exception("Agent appearance error: faces is not specified with TriangleMeshAgent.");
        }
        this.faces = new int[faces.size()];
        for (int index = 0; index < faces.size(); index++) {
            this.faces[index] = faces.get(index).intValue();
        }

        switch (getStringParameter("drawMode", "FILL").toUpperCase()) {
        case "FILL":
            drawMode = DrawMode.FILL;
            break;
        case "LINE":
            drawMode = DrawMode.LINE;
            break;
        }

        rotationAngle = getDoubleParameter("rotationAngle", rotationAngle);

        switch (getStringParameter("rotationAxis", "Z")) {
        case "X":
            rotationAxis = Rotate.X_AXIS;
            break;
        case "Y":
            rotationAxis = Rotate.Y_AXIS;
            break;
        case "Z":
            rotationAxis = Rotate.Z_AXIS;
            break;
        }
    }

    /**
     * エージェントのシェープを生成する
     */
    public Shape3D createShape(AgentBase agent, double size) {
        float points[] = new float[this.points.length];
        for (int index = 0; index < points.length; index++) {
            points[index] = this.points[index] * (float)size;
        }
        TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().addAll(0, 0);
        mesh.getPoints().setAll(points);
        mesh.getFaces().setAll(faces);

        Shape3D shape = new MeshView(mesh);
        shape.setDrawMode(drawMode);
        shape.setCullFace(CullFace.NONE);

        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(getAgentColor(agent));
        shape.setMaterial(material);

        double angle = Math.atan2(agent.getNextNode().getY() - agent.getPrevNode().getY(), agent.getNextNode().getX() - agent.getPrevNode().getX()) * 180.0 / Math.PI;
        Rotate rotate = new Rotate(angle, Rotate.Z_AXIS);
        if (rotationAngle == 0.0) {
            shape.getTransforms().add(rotate);  // この transform はリアクティブなので rotate の値が変わると反応する
        } else {
            Rotate baseRotate = new Rotate(rotationAngle, rotationAxis);
            shape.getTransforms().addAll(rotate, baseRotate);
        }
        rotations.put(agent, rotate);

        return shape;
    }

    /**
     * エージェントが移動した
     */
    public void agentMoved(AgentBase agent, Shape3D shape) {
        // 進行方向に向きを変える
        Rotate rotate = rotations.get(agent);
        double angle = Math.atan2(agent.getNextNode().getY() - agent.getPrevNode().getY(), agent.getNextNode().getX() - agent.getPrevNode().getX()) * 180.0 / Math.PI;
        rotate.setAngle(angle);
    }

    /**
     * エージェントのサイズを変更する
     */
    public void changeAgentSize(AgentBase agent, Shape3D shape, double size) {
        float _size = (float)size;
        float[] points = new float[this.points.length];
        for (int index = 0; index < points.length; index++) {
            points[index] = this.points[index] * _size;
        }
        TriangleMesh mesh = (TriangleMesh)((MeshView)shape).getMesh();
        mesh.getPoints().setAll(points);
    }
}
