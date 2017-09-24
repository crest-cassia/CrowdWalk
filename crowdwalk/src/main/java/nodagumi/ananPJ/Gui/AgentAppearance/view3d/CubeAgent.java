package nodagumi.ananPJ.Gui.AgentAppearance.view3d;

import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Box;

import nodagumi.ananPJ.Agents.AgentBase;

/**
 * 立方体エージェントの 3D 表示
 */
public class CubeAgent extends SphereAgent {
    /**
     * エージェントのシェープを生成する
     */
    public Shape3D createShape(AgentBase agent, double size) {
        Shape3D shape = new Box(size, size, size);

        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(getAgentColor(agent));
        shape.setMaterial(material);

        return shape;
    }

    /**
     * エージェントのサイズを変更する
     */
    public void changeAgentSize(AgentBase agent, Shape3D shape, double size) {
        ((Box)shape).setWidth(size);
        ((Box)shape).setHeight(size);
        ((Box)shape).setDepth(size);
    }
}
