package nodagumi.ananPJ.NetworkParts;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/* Parent of agent/node/link (leaf of OB) */
public abstract class OBMapPart extends OBNode implements Serializable {

	/**
	 * 引数なしconstractor。 ClassFinder.newByName で必要。
	 */
	public OBMapPart() {} ;

    public OBMapPart(int _ID){
		init(_ID) ;
	}

	/**
	 * 初期化。constractorから分離。
	 */
    @Override
    public void init(int _ID){
        super.init(_ID);
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
