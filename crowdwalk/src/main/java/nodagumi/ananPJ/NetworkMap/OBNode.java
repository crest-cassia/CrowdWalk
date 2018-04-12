// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.NetworkMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.tree.DefaultMutableTreeNode;

import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.ananPJ.NetworkMap.Area.MapAreaRectangle;
import nodagumi.ananPJ.NetworkMap.Polygon.MapPolygon;
import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.misc.SimTime;
import nodagumi.ananPJ.misc.Trail;

import nodagumi.Itk.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/* When making a new offspring of OBNode, you have to
 * modify the following methods.
 *  OBNode.fromDom(Element element)
 */
public abstract class OBNode extends DefaultMutableTreeNode
    implements Trail.Content
{
    public enum NType{NODE,LINK,AGENT,GROUP,AREA,SYMLINK,POLYGON}

    public String ID;

    protected String idNumber = null;
    protected ArrayList<String> tags;
    public boolean selected = false;
    protected NetworkMap map;

    //------------------------------------------------------------
    /**
     * method for Trail.Content interface.
     */
    public Object getJsonObject(){ return getID() ; }
    
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * alert message
     */
    public HashMap<Term, SimTime> alertMessageTable ;

    //------------------------------------------------------------
    /**
     * 引数なしconstractor。 ClassFinder.newByName で必要。
     */
    public OBNode() { this(null) ;} ;

    public OBNode(String _ID){
        init(_ID) ;
    }

    /**
     * 初期化。constractorから分離。
     */
    public void init(String _ID){
        ID = _ID;
        tags = new ArrayList<String>();
        alertMessageTable = new HashMap<Term, SimTime>() ;
    }

    public abstract boolean isLeaf();
    public abstract NType getNodeType();
    public Element toDom(Document dom, String tagname) {
        Element element = dom.createElement(tagname);
        for (String tag_str : tags) {
            Element tnode = dom.createElement("tag");
            Text tag_text = dom.createTextNode(tag_str);
            tnode.appendChild(tag_text);
            element.appendChild(tnode);
        }
        return element;
    }

    protected void getAttributesFromDom(Element element) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); ++i) {
            if (children.item(i)  instanceof Element) {
                Element child = (Element)children.item(i);
                if (!child.getTagName().equals("tag")) continue;
                addTag(Itk.intern(child.getTextContent())) ;
            }
        }
    }

    public static String getNodeTypeString() {return null;}
    public static OBNode fromDom(Element element) {
        final String tagName = element.getTagName();
        if (tagName.equals(MapPartGroup.getNodeTypeString())) {
            return MapPartGroup.fromDom(element);
        } else if (tagName.equals(MapNode.getNodeTypeString())) {
            return MapNode.fromDom(element);
        } else if (tagName.equals(MapLink.getNodeTypeString())) {
            return MapLink.fromDom(element);
        } else if (tagName.equals(MapAreaRectangle.getNodeTypeString())) {
            return MapAreaRectangle.fromDom(element);
        } else if (tagName.equals(OBNodeSymbolicLink.getNodeTypeString())) {
            return OBNodeSymbolicLink.fromDom(element);
        } else if (tagName.equals(MapPolygon.getNodeTypeString())) {
            return MapPolygon.fromDom(element);
        } else if (tagName.equals("tag")){
            return null;
        } else {
            System.err.println(tagName + " not known to OBnode.fromDom");
            return null;
        }
    }


    //------------------------------------------------------------
    // アクセサ
    //------------------------------------------------------------
    /**
     * ID 取得
     */
    public String getID() {
        return ID ;
    }

    /**
     * ID の番号部分(0詰めを除く)を取得
     */
    public String getIdNumber() {
        if (idNumber == null) {
            idNumber = ID.replaceFirst("[^0-9]*", "");
            // 番号部分なしならば "" を返す
            if (idNumber.isEmpty()) {
                return idNumber;
            }
            idNumber = idNumber.replaceFirst("0*", "");
            // 0のみ
            if (idNumber.isEmpty()) {
                idNumber = "0";
            }
        }
        return idNumber;
    }

    /* tag related methods */
    public boolean hasTag (Term _tag) {
        if(_tag.isAtom())
            return hasTag(_tag.getString()) ;
        else
            return false ;
    }

    public boolean hasTag (String _tag) {
        if (_tag == null) return false;

        //return tags.contains(_tag) ;
        return Itk.containsItself(tags, _tag) ;
    }

    /**
     * 部分文字列 _tag を含んでいるタグがあるか?
     */
    public boolean hasSubTag(String subtag) {
        if (subtag == null || subtag.isEmpty())
            return false;

        for (String tag : tags) {
            if (tag.contains(subtag))
                return true;
        }
        return false;
    }

    public Matcher matchTag (String pattern_str) {
        Pattern pattern = Pattern.compile(pattern_str);
        for (final String tag : tags) {
            Matcher matcher = pattern.matcher(tag);
            if (matcher.matches()) {
                return matcher;
            }
        }
        return null;
    }

    public void allTagsClear(){
        tags.clear();
    }

    public boolean addTag(String _tag) {
        if (_tag == null || _tag.equals("")) return false;

        if (hasTag(_tag)) {
            return false;
        }
        tags.add(_tag);
        return true;
    }

    /**
     * タグを置き換える
     */
    public boolean replaceTag(String srcTag, String dstTag) {
        int index = tags.indexOf(srcTag);
        if (index == -1) {
            return false;
        }
        tags.set(index, dstTag);
        return true;
    }

    public void removeTag(String _tag) {
        ArrayList<String> tags_r = new ArrayList<String>();
        tags_r.add(_tag);
        tags.removeAll(tags_r);
    }

    public final ArrayList<String> getTags() {
        return tags;
    }

    /**
     * n 番目のタグを取り出す。
     */
    public String getNthTag(int n) {
        if(getTags().size() > n) {
            return getTags().get(n) ;
        } else {
            return null ;
        }
    }

    public String getTagString() {
        StringBuffer str = new StringBuffer();
        boolean first = true;
        for (final String tag : tags) {
            if (!first) {
                str.append(",");
            }
            str.append(tag);
            first = false;
        }
        return str.toString();
    }

    /* verbose output used for hints */
    public String getHintString() {
        return "(default OBNode hint)";
    }

    public boolean isOffspring(OBNode node) {
        OBNode parent = (OBNode)getParent();
        if (parent == null) return false;
        if (parent == node) return true;
        return (parent.isOffspring(node));
    }

    /**
     * NetworkMap をセットする.
     */
    final public void setMap(NetworkMap _map) {
        map = _map ;
    }

    /**
     * NetworkMap を返す.
     */
    final public NetworkMap getMap() {
        return map ;
    }

    /**
     * このインスタンスの文字列化
     */
    public String toString() {
        return getTagString();
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * ゲート（分断制御用交通規制）テーブル。
     * 各ノード・リンクのゲートは、タグにより参照できる。
     * 単一のタグには単一のゲートのみ割り振ることができる。
     */
    private HashMap<String, GateBase> gateTable =
        new HashMap<String, GateBase>() ;

    //------------------------------------------------------------
    /**
     * ゲート（分断制御用交通規制）のチェック
     */
    public boolean isGateClosed(AgentBase agent, SimTime currentTime) {
        for(String gateTag: gateTable.keySet()) {
            GateBase gate = gateTable.get(gateTag) ;
            if(gate.isClosed(agent, currentTime)) return true ;
        }
        return false ;
    }

    //------------------------------------------------------------
    /**
     * ゲート操作  by Term
     */
    public GateBase switchGate(Term gateTag, boolean closed) {
        return switchGate(gateTag.getString(), closed) ;
    }
    
    //------------------------------------------------------------
    /**
     * ゲート操作
     */
    public GateBase switchGate(String gateTag, boolean closed) {
        GateBase gate = gateTable.get(gateTag) ;
        if(gate == null) {
            gate = new GateBase(gateTag, closed) ;
            gateTable.put(gateTag, gate) ;
        } else {
            gate.switchGate(closed) ;
        }
        Itk.logInfo("OBNode::switchGate", gateTag,
                    (closed ? "close" : "open"), this) ;
        return gate ;
    }

    //------------------------------------------------------------
    /**
     * ゲート閉鎖 by Term
     */
    public GateBase closeGate(Term tag) {
        return closeGate(tag.getString()) ;
    }

    //------------------------------------------------------------
    /**
     * ゲート閉鎖
     */
    public GateBase closeGate(String tag) {
        return switchGate(tag, true) ;
    }

    //------------------------------------------------------------
    /**
     * ゲート開放 by Term
     */
    public GateBase openGate(Term tag) {
        return openGate(tag.getString()) ;
    }

    //------------------------------------------------------------
    /**
     * ゲート開放
     */
    public GateBase openGate(String tag) {
        return switchGate(tag, false) ;
    }

    //============================================================
    /**
     * 通行規制制御用クラス
     */
    class GateBase {
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * OBNode内でこのゲートを参照するためのタグ
         */
        public String tag;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 現在閉じている（通行止め）かどうか？
         */
        public boolean closed ;

        //----------------------------------------
        /**
         * コンストラクタ
         */
        public GateBase(String _tag, boolean _closed) {
            tag = _tag ;
            closed = _closed ;
        }

        //----------------------------------------
        /**
         * 閉じているかどうか？
         * 拡張のために、時刻とエージェントを受け取る。
         * @param currnetTime : シミュレーション時刻
         * @param agent: 対象となるエージェント
         * @return デフォルトでは、単にこのゲートが閉じているかどうか
         */
        public boolean isClosed(AgentBase agent, SimTime currentTime) {
            return isClosed() ;
        }

        //----------------------------------------
        /**
         * 閉じているかどうか？
         * 拡張のために、時刻とエージェントを受け取る。
         * @param currentTime : シミュレーション時刻
         * @param agent: 対象となるエージェント
         * @return デフォルトでは、単にこのゲートが閉じているかどうか
         */
        public boolean isOpened(AgentBase agent, SimTime currentTime) {
            return !isClosed(agent, currentTime) ;
        }

        //----------------------------------------
        /**
         * 閉じているかどうか？
         */
        public boolean isClosed() {
            return closed ;
        }

        //----------------------------------------
        /**
         * 開いているかどうか？
         */
        public boolean isOpened() {
            return !isClosed() ;
        }

        //----------------------------------------
        /**
         * ゲートの開閉
         */
        public GateBase switchGate(boolean _closed) {
            closed = _closed ;
            return this ;
        }

        //----------------------------------------
        /**
         * ゲートを閉じる
         */
        public GateBase close() {
            return switchGate(true) ;
        }

        //----------------------------------------
        /**
         * ゲートを開ける
         */
        public GateBase open() {
            return switchGate(false) ;
        }

    } // class GateBase

}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
