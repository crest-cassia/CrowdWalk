package nodagumi.ananPJ.Gui.AgentAppearance.view3d;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Gui.AgentAppearance.model.ColorModel;
import nodagumi.ananPJ.Gui.FxColor;

/**
 * 球体エージェントの 3D 表示
 */
public class SphereAgent extends AgentViewBase3D {
    /**
     * エージェントのシェープを生成する
     */
    public Shape3D createShape(AgentBase agent, double size) {
        Shape3D shape = new Sphere(size / 2.0);

        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(getAgentColor(agent));
        shape.setMaterial(material);

        return shape;
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
        ((Sphere)shape).setRadius(size / 2.0);
    }

    /**
     * エージェントをホバー表示に切り替える
     */
    public void switchToHover(AgentBase agent, Shape3D shape) {
        PhongMaterial material = (PhongMaterial)shape.getMaterial();
        material.setDiffuseColor(panel.HOVER_COLOR);
    }

    /**
     * エージェントを通常表示に切り替える
     */
    public void switchToNormal(AgentBase agent, Shape3D shape) {
        updateAgent(agent, shape);
    }

    /**
     * エージェントの表示色を返す
     */
    protected Color getAgentColor(AgentBase agent) {
        switch (agent.getTriage()) {
        case GREEN:
            if (panel.isChangeAgentColorDependingOnSpeed()) {
                ColorModel color = (ColorModel)model.setAgent(agent);
                return Color.rgb(color.getRed(), color.getGreen(), color.getBlue());
            }
            break;
        case YELLOW:
            return FxColor.YELLOW;
        case RED:
            return FxColor.PRED;
        case BLACK:
            return FxColor.BLACK2;
        }
        return FxColor.DEFAULT_AGENT_COLOR;
    }
}
