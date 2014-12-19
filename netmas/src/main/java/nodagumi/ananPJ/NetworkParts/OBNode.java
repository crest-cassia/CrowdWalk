// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.NetworkParts;

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
import java.io.Serializable;
import java.lang.ClassNotFoundException;
import java.util.ArrayList;
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

import nodagumi.ananPJ.Agents.EvacuationAgent;
import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.NetworkParts.Pollution.PollutedAreaRectangle;
import nodagumi.ananPJ.misc.osmTools.osmGroup;
import nodagumi.ananPJ.misc.osmTools.osmLink;
import nodagumi.ananPJ.misc.osmTools.osmNode;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/* When making a new offspring of OBNode, you have to
 * modify the following methods.
 *  OBNode.fromDom(Element element)
 */
public abstract class OBNode extends DefaultMutableTreeNode
    implements Serializable {
  public enum NType{NODE,LINK,AGENT,GROUP,ROOM,SYMLINK}

  public int ID;
  protected ArrayList<String> tags;
  public boolean selected = false;

  /**
    * 引数なしconstractor。 ClassFinder.newByName で必要。
	*/
  public OBNode() { this(0) ;} ;

  public OBNode(int _ID){
	  init(_ID) ;
  }

  /**
    * 初期化。constractorから分離。
	*/
  public void init(int _ID){
      ID = _ID;
      tags = new ArrayList<String>();
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
              addTag(child.getTextContent().toUpperCase());
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
      } else if (tagName.equals(EvacuationAgent.getNodeTypeString())) {
          return EvacuationAgent.fromDom(element);
      } else if (tagName.equals(PollutedAreaRectangle.getNodeTypeString())) {
          return PollutedAreaRectangle.fromDom(element);
      } else if (tagName.equals(OBNodeSymbolicLink.getNodeTypeString())) {
          return OBNodeSymbolicLink.fromDom(element);
      } else if (tagName.equals("tag")){
          return null;
      // tkokada
      /*
      } else if (tagName.equals(osmNode.getNodeTypeString())) {
          return osmNode.fromDom(element);
      } else if (tagName.equals(osmLink.getNodeTypeString())) {
          return osmLink.fromDom(element);
      */
      } else if (tagName.equals(osmGroup.getNodeTypeString())) {
          return osmGroup.fromDom(element);
      } else {
          System.err.println(tagName + " not known to OBnode.fromDom");
          return null;
      }
  }
  

  /* tag related methods */
  public boolean hasTag (String _tag) {
      if (_tag == null) return false;
      if (_tag.equals("*")) return true;

	  /* [2014.12.19 I.Noda] should obsolete
	   * これは、絶対に辞めたい。
	   */
      _tag = _tag.toUpperCase();

	  /* [2014.12.19 I.Noda] 
	   * 以下は同じ意味のはず。
	   */
	  if(tags.contains(_tag)) return true ;
	  /* 古いもの
      for (final String tag : tags) {
          if (_tag.equals(tag)) {
              return true;
          }
      }
	  */

	  /* [2014.12.19 I.Noda] obsolete obsolute
	   * これもバグの元。
	   */
      if (_tag.equals("EMERGENCY")) { return hasTag("EXIT"); }

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
      if (_tag.contains(" ")) {
          Thread.dumpStack();
          System.exit(0);
      }
      if (_tag == null || _tag.equals("")) return false;
      if (! _tag.equals("root") && ! _tag.equals(_tag.toUpperCase())) {
          System.err.println("小文字を含んだタグが使われています: " + _tag);
          System.exit(0);
      }
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
  
  public void prepareForSave(boolean hasDisplay) {
  }
  public void postLoad(boolean hasDisplay) {
  }
  
  public boolean isOffspring(OBNode node) {
      OBNode parent = (OBNode)getParent();
      if (parent == null) return false;
      if (parent == node) return true;
      return (parent.isOffspring(node));
  }
  
  static class TagSetupPanel extends JPanel {
    private static final long serialVersionUID = 3573992132966257203L;
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

    // Conversion from MapNode, MapLink, PollutedArea to OBNode
    @SuppressWarnings("unchecked")
    public static JPanel setupTagPanel(ArrayList nodes, JDialog parent) {
        TagSetupPanel panel = new TagSetupPanel(nodes, parent);
        return panel;
    }

    public Element storeToDOM(Document doc, String tag) {
        Element element = doc.createElement(tag);
        element.setAttribute("class", "OBNode");

        Element idElement = doc.createElement("ID");
        idElement.setAttribute("class", "int");
        Text idText = doc.createTextNode("" + ID);
        idElement.appendChild(idText);
        element.appendChild(idElement);

        return element;
    }

    // 2013.02.21 tkokada add for ScenarioEvent STOP_TIMES
    private ArrayList<StopTime> stopTimes = new ArrayList<StopTime>();

    public boolean isStopTimesEnabled() {
        if (stopTimes.size() > 0)
            return true;
        return false;
    }

    public void addStopTime(String tag, double offset, double moving,
            double stopping) {
        StopTime st = new StopTime(tag, offset, moving, stopping);
        stopTimes.add(st);
    }

    public void removeStopTime(String tag) {
        Iterator<StopTime> iter = stopTimes.iterator();
        while (iter.hasNext()) {
            if (iter.next().tag.equals(tag)) {
                iter.remove();
            }
        }
    }

    public boolean isStop(String tag, double time) {
        for (StopTime st : stopTimes) {
            if (st.tag.equals(tag)) {
                if (st.isStop(time))
                    return true;
            }
        }
        return false;
    }

    class StopTime {
        public String tag;
        public double offset, moving, stopping;

        public StopTime(String tag, double offset, double moving,
                double stopping) {
            this.tag = tag;
            this.offset = offset;
            this.moving = moving;
            this.stopping = stopping;
        }

        public boolean isStop(double time) {
            // System.err.println("time - this.offset: " + (time - this.offset) + ", moving: " +  this.moving + ", stopping: " + this.stopping);
            return ((time - this.offset) % (this.moving + this.stopping)) > moving;
        }
    }
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
