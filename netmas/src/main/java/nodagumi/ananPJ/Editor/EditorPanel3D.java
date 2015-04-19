package nodagumi.ananPJ.Editor;

import java.awt.geom.Rectangle2D;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.media.j3d.Group;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Vector3f;

import nodagumi.ananPJ.NetworkMapEditor;
import nodagumi.ananPJ.NetworkPanel3D;
import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;

import com.sun.j3d.utils.universe.ViewingPlatform;

public class EditorPanel3D extends NetworkPanel3D {

    private NetworkMapEditor editor;

    private MapNode hoverNode = null;
    private MapLink hoverLink = null;
    private AgentBase hoverAgent = null;
    private Rectangle2D selectedArea = null;

    private EditorPanel3D(NetworkMapEditor editor, JFrame _parent) {
        super(editor.getNodes(), editor.getLinks(), _parent, editor.getProperties());
    }

    public static NetworkPanel3D createPanel(NetworkMapEditor editor,
            JFrame parent) {
        if (editor.getNodes().size() == 0) {
            JOptionPane.showMessageDialog(null,
                    "NetworkMap seems to be empty!",
                    "Error making3D panel",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
        EditorPanel3D p = new EditorPanel3D(editor, parent);
        p.initialize();
        return p;
    }

    @Override
    public void initialize() {
        super.initialize();
        ViewingPlatform vplt = universe.getViewingPlatform();
        TransformGroup viewpoint = vplt.getViewPlatformTransform();
        Transform3D trans3d = new Transform3D();
        trans3d.setRotation(new AxisAngle4d(1, 0, 0, -1.57));
        trans3d.setTranslation(new Vector3f(0.0f, 10.0f, 0.0f));
        trans3d.setRotation(new AxisAngle4d(1, 0, 0, -1.57));
        trans3d.setRotation(new AxisAngle4d(0, 0, 0, 0));
        viewpoint.setTransform(trans3d);
    }

    @Override
    protected void register_map_objects() {
    }

    @Override
    protected void registerOtherObjects() {
    }
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
