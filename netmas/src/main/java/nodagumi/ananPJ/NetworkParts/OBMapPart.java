package nodagumi.ananPJ.NetworkParts;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/* Parent of agent/node/link (leaf of OB) */
public abstract class OBMapPart extends OBNode implements Serializable {

    public OBMapPart(int _ID){
        super(_ID);
        this.setAllowsChildren(false);
    }

    @Override
    public boolean isLeaf() {
        return true;
    }
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
