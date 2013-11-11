package nodagumi.ananPJ.NetworkParts;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class OBNodeSymbolicLink extends OBMapPart implements Serializable {
	/**
	 * エディタ上で別のグループに存在するノードを表示するためのシンボリックリンク．
	 */
	private static final long serialVersionUID = 2287558438601265909L;
	
	private OBNode original = null;
	
	public OBNodeSymbolicLink(int _id) {
		super(_id);
	}
	
	public OBNodeSymbolicLink(int _id,
			OBNode _original) {
		super(_id);
		original = _original;
	}

	public OBNode getOriginal() {
		return original;
	}
	
	public void setOriginal(OBNode _original) {
		original = _original;
	}

	@Override
	public NType getNodeType() {
		return NType.SYMLINK;
	}

	@Override
	public boolean isLeaf() {
		return true;
	}

	@Override
	public Element toDom(Document dom, String tagname) {
		Element element = super.toDom(dom, getNodeTypeString());
		element.setAttribute("id", "" + ID);
		element.setAttribute("orig", "" + original.ID);
		return element;
	}
		
	public static String getNodeTypeString() {
		return "SymbolicLink";
	}
	
	public static OBNodeSymbolicLink fromDom(Element element) {
		int id = Integer.parseInt(element.getAttribute("id"));
		int orig_id = Integer.parseInt(element.getAttribute("orig"));
		
		OBNodeSymbolicLink symlink = new OBNodeSymbolicLink(id);
		symlink.setUserObject(orig_id);

		return symlink;
	}
	
	public String toString() {
		return getTagString();
	}
    // tkokada
    /*
    private void writeObject(ObjectOutputStream stream) {
        try {
            stream.defaultWriteObject();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream stream) {
        try {
            stream.defaultReadObject();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
    }
    */
}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
