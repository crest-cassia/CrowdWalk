package nodagumi.ananPJ.Gui.AgentAppearance.view3d;

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
import nodagumi.ananPJ.Gui.SimulationPanel3D.TrianglePolygon;

/**
 * 平面三角形エージェントの 3D 表示
 */
public class PlaneTriangleAgent extends SphereAgent {
    /**
     * エージェントと Rotate オブジェクトの対応表
     */
    protected HashMap<AgentBase, Rotate> rotations = new HashMap();

    /**
     * エージェントのシェープを生成する
     */
    public Shape3D createShape(AgentBase agent, double size) {
        Shape3D shape = new MeshView(makeTrianglePolygon(size));
        shape.setDrawMode(DrawMode.FILL);
        shape.setCullFace(CullFace.NONE);

        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(getAgentColor(agent));
        shape.setMaterial(material);

        double angle = Math.atan2(agent.getNextNode().getY() - agent.getPrevNode().getY(), agent.getNextNode().getX() - agent.getPrevNode().getX()) * 180.0 / Math.PI;
        Rotate rotate = new Rotate(angle + 90.0, Rotate.Z_AXIS);
        shape.getTransforms().add(rotate);
        rotations.put(agent, rotate);

        return shape;
    }

    /**
     * エージェントが移動した
     */
    public void agentMoved(AgentBase agent, Shape3D shape) {
        Rotate rotate = rotations.get(agent);
        double angle = Math.atan2(agent.getNextNode().getY() - agent.getPrevNode().getY(), agent.getNextNode().getX() - agent.getPrevNode().getX()) * 180.0 / Math.PI;
        rotate.setAngle(angle + 90.0);
    }

    /**
     * エージェントの表示を更新する
     */
    public void updateAgent(AgentBase agent, Shape3D shape) {
        Color color = getAgentColor(agent);
        PhongMaterial material = (PhongMaterial)shape.getMaterial();
        if (! material.getDiffuseColor().equals(color)) {
            material.setDiffuseColor(color);
        }
    }

    /**
     * エージェントのサイズを変更する
     */
    public void changeAgentSize(AgentBase agent, Shape3D shape, double size) {
        ((MeshView)shape).setMesh(makeTrianglePolygon(size));
    }

    /**
     * 平面三角形ポリゴンを生成する
     */
    protected TrianglePolygon makeTrianglePolygon(double size) {
        double height = Math.sqrt(size * size - Math.pow(size / 2.0, 2.0));
        double x1 = 0.0;
        double y1 = -2.0 * height / 3.0;
        double x2 = -(size / 2.0);
        double y2 = height / 3.0;
        double x3 = size / 2.0;
        double y3 = y2;

        Point3D[] vertices = new Point3D[3];
        vertices[0] = new Point3D(x1, y1, 0.0);
        vertices[1] = new Point3D(x2, y2, 0.0);
        vertices[2] = new Point3D(x3, y3, 0.0);
        return panel.new TrianglePolygon(vertices);
    }
}
