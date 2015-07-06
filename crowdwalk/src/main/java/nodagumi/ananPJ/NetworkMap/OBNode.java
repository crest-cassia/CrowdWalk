// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.NetworkMap;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ClassNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.tree.DefaultMutableTreeNode;

import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.ananPJ.NetworkMap.Area.MapAreaRectangle;
import nodagumi.ananPJ.Agents.AgentBase;

import nodagumi.Itk.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/* When making a new offspring of OBNode, you have to
 * modify the following methods.
 *  OBNode.fromDom(Element element)
 */
public abstract class OBNode extends DefaultMutableTreeNode {
    public enum NType{NODE,LINK,AGENT,GROUP,AREA,SYMLINK}

    public String ID;
    protected ArrayList<String> tags;
    public boolean selected = false;
    protected NetworkMap networkMap;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * alert message
     */
    public HashMap<Term, Double> alertMessageTable ;

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
        alertMessageTable = new HashMap<Term, Double>() ;
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
                addTag(child.getTextContent()) ;
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
        } else if (tagName.equals("tag")){
            return null;
        } else {
            System.err.println(tagName + " not known to OBnode.fromDom");
            return null;
        }
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

        if(tags.contains(_tag)) return true ;

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

    public void removeTag(String _tag) {
        ArrayList<String> tags_r = new ArrayList<String>();
        tags_r.add(_tag);
        tags.removeAll(tags_r);
    }

    public final ArrayList<String> getTags() {
        return tags;
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
    public void setNetworkMap(NetworkMap networkMap) {
        this.networkMap = networkMap;
    }

    /**
     * NetworkMap を返す.
     */
    public NetworkMap getNetworkMap() {
        return networkMap;
    }

    static class TagSetupPanel extends JPanel {
        private ArrayList<OBNode> nodes;
        private ArrayList<String> tags = new ArrayList<String>();
        private ArrayList<JCheckBox> tag_cbs = new ArrayList<JCheckBox>();

        JDialog parent;

        public TagSetupPanel(ArrayList<OBNode> _nodes, JDialog _parent) {
            nodes = _nodes;
            parent = _parent;
            setLayout(new BorderLayout());

            JPanel remove_tag_panel = setup_remove_tag();
            add(remove_tag_panel, BorderLayout.CENTER);
            JPanel add_tag_panel = setup_add_tag();
            add(add_tag_panel, BorderLayout.SOUTH);

            parent.repaint();
        }

        JButton update_button;
        private JPanel setup_remove_tag() {
            JPanel panel = new JPanel();

            panel.setLayout(new FlowLayout());
            panel.setBorder(BorderFactory.createTitledBorder(
                                                             BorderFactory.createLineBorder(Color.black), "remove tags"));

            for (OBNode node : nodes) {
                if (node.selected) {
                    for (String tag : node.getTags()) {
                        if (!tags.contains(tag)) {
                            tags.add(tag);
                        }
                    }
                }
            }

            if (tags.isEmpty()) {
                panel.add(new JLabel("no tags to remove"));
                parent.repaint();
                return panel;
            }

            for (String tag : tags) {
                JCheckBox cb = new JCheckBox(tag);
                cb.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            boolean be = false;
                            for (JCheckBox cb : tag_cbs) { be |= cb.isSelected(); }
                            update_button.setEnabled(be);
                        }
                    });
                tag_cbs.add(cb);
                panel.add(cb);
            }

            update_button = new JButton("remove");
            update_button.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) { update_tags(); }
                });
            update_button.setEnabled(false);
            panel.add(update_button);

            return panel;
        }

        private void update_tags() {
            for (JCheckBox cb : tag_cbs) {
                if (cb.isSelected()) {
                    String tag = cb.getText();
                    for (OBNode node : nodes) {
                        if (node.selected) {
                            node.removeTag(tag);
                        }
                    }
                }
            }
            if (parent != null) {
                parent.dispose();
            }
        }

        JTextField add_tag_field;
        JButton add_tag_button;

        private JPanel setup_add_tag() {
            JPanel panel = new JPanel();
            panel.setLayout(new GridBagLayout());
            panel.setBorder(BorderFactory.createTitledBorder(
                                                             BorderFactory.createLineBorder(Color.black), "add tag"));
            GridBagConstraints c;

            add_tag_field = new JTextField();
            add_tag_field.setPreferredSize(new Dimension(200, 24));
            add_tag_field.addKeyListener(new KeyListener() {
                    public void keyTyped(KeyEvent e) {
                        boolean enabled = !add_tag_field.getText().isEmpty();
                        add_tag_button.setEnabled(enabled);
                        if (enabled && (e.getKeyChar() == 10)) {
                            add_tag();
                        }
                    }
                    public void keyReleased(KeyEvent e) {}
                    public void keyPressed(KeyEvent e) {}
                });
            c = new GridBagConstraints();
            panel.add(add_tag_field, c);

            add_tag_button = new JButton("add");
            add_tag_button.setEnabled(false);
            add_tag_button.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) { add_tag(); }
                });
            c = new GridBagConstraints();
            c.gridx = 1;
            panel.add(add_tag_button, c);

            return panel;
        }

        private void add_tag() {
            String tag = add_tag_field.getText();
            for (OBNode node : nodes) {
                if (node.selected) {
                    node.addTag(tag);
                }
            }
            if (parent != null) {
                parent.dispose();
            }
        }
    }

    // Conversion from MapNode, MapLink, MapArea to OBNode
    @SuppressWarnings("unchecked")
    public static JPanel setupTagPanel(ArrayList nodes, JDialog parent) {
        TagSetupPanel panel = new TagSetupPanel(nodes, parent);
        return panel;
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
    public boolean isGateClosed(AgentBase agent, double time) {
        for(String gateTag: gateTable.keySet()) {
            GateBase gate = gateTable.get(gateTag) ;
            if(gate.isClosed(agent, time)) return true ;
        }
        return false ;
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
        return gate ;
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
         * @param time : シミュレーション時刻
         * @param agent: 対象となるエージェント
         * @return デフォルトでは、単にこのゲートが閉じているかどうか
         */
        public boolean isClosed(AgentBase agent, double time) {
            return isClosed() ;
        }

        //----------------------------------------
        /**
         * 閉じているかどうか？
         * 拡張のために、時刻とエージェントを受け取る。
         * @param time : シミュレーション時刻
         * @param agent: 対象となるエージェント
         * @return デフォルトでは、単にこのゲートが閉じているかどうか
         */
        public boolean isOpened(AgentBase agent, double time) {
            return !isClosed(agent, time) ;
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

    }
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End: