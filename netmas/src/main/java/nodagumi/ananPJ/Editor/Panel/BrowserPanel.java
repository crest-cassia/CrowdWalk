package nodagumi.ananPJ.Editor.Panel;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
	
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;


import javax.swing.tree.TreePath;
import nodagumi.ananPJ.misc.*;


import nodagumi.ananPJ.NetworkMap;
import nodagumi.ananPJ.NetworkMapEditor;
import nodagumi.ananPJ.Editor.EditorFrame;
import nodagumi.ananPJ.NetworkParts.MapPartGroup;
import nodagumi.ananPJ.NetworkParts.OBNode;

public class BrowserPanel
extends JPanel
implements MouseListener, TreeSelectionListener, Serializable {
	private static final long serialVersionUID = -1584209347343617161L;

	private NetworkMapEditor editor = null;
	DnDJTree tree;
	private static int groupCount = 0;
	private enum OP{ADD,SHOW,CLEAR,ALLCLEAR,SEARCH};
	/* used in pop-up menu */
	private int mouseX;
	private int mouseY;
	private JPopupMenu popmenu;
	private JMenuItem jmiMakeGroup;
	private JMenuItem jmiEditGroupInfo;
	private JMenuItem jmiShowTags;
	private JMenuItem jmiAddTagRec;
	private JMenuItem jmiClearTagRec;
	private JMenuItem jmiDeleteItem;

	public BrowserPanel (NetworkMapEditor _editor){
		editor = _editor;

		tree = new DnDJTree(editor.getMap());
		tree.setModel(editor.getMap());
		setLayout(new BorderLayout());
		JScrollPane scrollpane = new JScrollPane(tree,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollpane.setPreferredSize(new Dimension(300, 480));
		add(scrollpane, BorderLayout.NORTH);
		
		popmenu = new JPopupMenu();
		jmiMakeGroup = new JMenuItem("Make Group");
		jmiMakeGroup.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						TreePath path = tree.getPathForLocation(mouseX, mouseY);
				        if (path == null) return;
				        DefaultMutableTreeNode selected_node = (DefaultMutableTreeNode)path.getLastPathComponent();
				        if(selected_node.getAllowsChildren()){
				        	make_group(selected_node);
				        }
				        else {
				        	System.err.println(tree.getLastSelectedPathComponent()+" is OBNode");
				        }
				        tree.repaint();
					}
				}
		);
		jmiEditGroupInfo = new JMenuItem("Edit Group Info");
		jmiEditGroupInfo.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					TreePath path = tree.getPathForLocation(mouseX, mouseY);
			        if (path == null) return;
			        /* get OBNode */
			        OBNode _OBNode = (OBNode)tree.getLastSelectedPathComponent();
			        //TODO:bachi: Show OBNode information and change interface
			        popupNotifyDialog("Tags",_OBNode.getTagString());
			        tree.repaint();
				}	
			}
		);
		jmiShowTags = new JMenuItem("Show Tags");
		jmiShowTags.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					TreePath path = tree.getPathForLocation(mouseX, mouseY);
			        if (path == null) return;
			        /* get OBNode */
			        OBNode _OBNode = (OBNode)tree.getLastSelectedPathComponent();
			        popupNotifyDialog("Tags",_OBNode.getTagString());
			        tree.repaint();
				}	
			}
		);

		jmiAddTagRec = new JMenuItem("Tag add recursive");
		jmiAddTagRec.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					TreePath path = tree.getPathForLocation(mouseX, mouseY);
			        if (path == null) return;
			        /* get OBNode */
			        OBNode _OBNode = (OBNode)tree.getLastSelectedPathComponent();
			        String tag = JOptionPane.showInputDialog(null,
			        		"Add tag recursive", "Tag",
			        		JOptionPane.QUESTION_MESSAGE);
			        //popupInputDialog("Tag add recursive","");
			        if (tag != null) {
			        	recursiveTreeAround(_OBNode,tag,OP.ADD);
			        }
			        tree.repaint();
			        refresh();
				}	
			}
		);
		
		jmiClearTagRec = new JMenuItem ("Tag clear recursive");
		jmiClearTagRec.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						TreePath path = tree.getPathForLocation(mouseX, mouseY);
				        if (path == null) return;
				        /* get OBNode */
				        //DefaultMutableTreeNode _node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
				        OBNode _OBNode = (OBNode) tree.getLastSelectedPathComponent();
				        recursiveTreeAround(_OBNode,"",OP.ALLCLEAR);
				        tree.repaint();
					}
				}
			);
		
		jmiDeleteItem = new JMenuItem ("Delete item");
		jmiDeleteItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						TreePath path = tree.getPathForLocation(mouseX, mouseY);
				        if (path == null) return;
				        /* get OBNode */
				        int r = JOptionPane.showConfirmDialog(null,
				        		"Really delete this object and it's offspring?",
				        		"Waring: deleting item",
				        		JOptionPane.OK_CANCEL_OPTION,
				        		JOptionPane.QUESTION_MESSAGE);
				        if (r != JOptionPane.YES_OPTION) return;
				        OBNode ob_node = (OBNode) tree.getLastSelectedPathComponent();
						editor.getMap().removeOBNode((OBNode)ob_node.getParent(), ob_node, true);
				        tree.repaint();
					}
				}
			);
		
		popmenu.add(jmiMakeGroup);
		popmenu.add(jmiShowTags);
		popmenu.add(jmiAddTagRec);
		popmenu.add(jmiClearTagRec);
		popmenu.add(jmiDeleteItem);
		
		popmenu.addSeparator();

		JMenuItem mi = new JMenuItem("Show Frame");
		mi.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						TreePath path = tree.getPathForLocation(mouseX, mouseY);
				        if (path == null) return;
				        /* get OBNode */
				        OBNode ob_node = (OBNode) tree.getLastSelectedPathComponent();
				        MapPartGroup group = null;
				        if (ob_node instanceof MapPartGroup) {
				        	group = (MapPartGroup)ob_node;
				        } else {
				        	group = (MapPartGroup)ob_node.getParent();
				        }
				        EditorFrame frame = showEditorForGroup(group);
				        frame.clearSelection();
				        ob_node.selected = true;
					}
				});
		popmenu.add(mi);

		tree.addMouseListener(this);
		setVisible(true);		
	}
	
	private boolean recursiveTreeAround(OBNode _OBNode,String tag,OP op){
		switch (op){ 
		case ADD:
			_OBNode.addTag(tag);
			break;
		case ALLCLEAR:
			_OBNode.allTagsClear();
			break;
		}
		if(_OBNode.isLeaf()){
			return true;
		}else{
			if(_OBNode.getChildCount() != 0){
				for(int i = 0; i < _OBNode.getChildCount();i++ ){
					recursiveTreeAround((OBNode)_OBNode.getChildAt(i),tag,op);
				}
			}
			else
				return true;
		}
		return true;
	}
	
	class TagNameAndHeightDialog
	extends JDialog
	implements ActionListener {
		private static final long serialVersionUID = 5981796182956953284L;
		private JSpinner height;
		private JTextField tag = new JTextField(10);
		private JButton ok_button;
		
		public TagNameAndHeightDialog(double default_height) {
			this.setModal(true);
			Container panel = getContentPane();
			panel.setLayout(new FlowLayout());
			
			tag.addKeyListener(new KeyListener() {
				public void keyTyped(KeyEvent e) {
					if (e.getKeyChar() == 10) {
						dispose();
					} else {
						if (tag.getText().isEmpty()) {
							ok_button.setText("cancel");
						} else {
							ok_button.setText("OK");
						}
					}
				}
				
				public void keyReleased(KeyEvent e) {}
				public void keyPressed(KeyEvent e) {}
			});
			height = new JSpinner(
					new SpinnerNumberModel(
							default_height,
							-1000.0, 1000.0, 0.1));

			panel.add(new JLabel("tag:"));
			panel.add(tag);
			panel.add(new JLabel("height:"));
			panel.add(height);
			panel.add(new JLabel());
			
			ok_button = new JButton("cancel");
			ok_button.addActionListener(this);
    		panel.add(ok_button);
    		
    		this.pack();
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			dispose();
		}
		
		public double getDefaultHeight() {
			return (Double)height.getValue();
		}
		
		public String getTagName() {
			if (tag.getText().isEmpty()) return null;
			return tag.getText();
		}
	}

	private void make_group(DefaultMutableTreeNode parentNode){
		TagNameAndHeightDialog get_tag_and_height =
			new TagNameAndHeightDialog(((MapPartGroup)parentNode).getDefaultHeight());
		get_tag_and_height.setLocation(getMousePosition());
		get_tag_and_height.setVisible(true);
		if (get_tag_and_height.getTagName() == null) { return; }
		
		MapPartGroup group = editor.getMap().createGroupNode((MapPartGroup) parentNode);

		double default_height = get_tag_and_height.getDefaultHeight();
		group.setDefaultHeight(default_height);
		group.setMinHeight(default_height - 5);
		group.setMaxHeight(default_height + 5);
		group.addTag(get_tag_and_height.getTagName());

		groupCount++;
        tree.repaint();
	}
	
	public void refresh() {
		tree.setModel(editor.getMap());
        tree.repaint();
	}
	
	@Override
	public void valueChanged(TreeSelectionEvent arg0) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if(SwingUtilities.isRightMouseButton(e)) { // (2-1)
			int selRow = tree.getRowForLocation(e.getX(), e.getY()); // (2-2)
			if(selRow != -1) {
				tree.setSelectionRow(selRow); // (2-3)
				popmenu.show(e.getComponent(), e.getX(), e.getY()); // (2-4)
				mouseX=e.getX();
				mouseY=e.getY();
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
	}
	
	@Override
	public void mouseExited(MouseEvent arg0) {
	}
	
	@Override
	public void mouseClicked(MouseEvent arg0) {
		OBNode ob_node = (OBNode)tree.getLastSelectedPathComponent();
        if (ob_node == null) return;

	    if ((arg0.getClickCount() == 2) &&
	    		(javax.swing.SwingUtilities.isLeftMouseButton(arg0))) {
	        /* Generate editor frame */
	    	if(!ob_node.isLeaf()){
		        MapPartGroup group = (MapPartGroup)ob_node;
			    showEditorForGroup(group);
	        } else {
	        	/* double click on non-group node */
	        }
	    } else {
	    	/* single click */
	    	MapPartGroup group = (MapPartGroup)ob_node.getParent();
	    	if (group != null) {
	    		Object o = group.getUserObject();
	    		if (o != null && o instanceof EditorFrame) {
	    			EditorFrame frame = (EditorFrame)o;
	    			frame.clearSelection();
	    			ob_node.selected = true;
	    			frame.repaint();
	    		}
	    	}
	    }
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
	}

	/*
	private void popupJtfAlias(final DefaultMutableTreeNode _targetNode) {

        // Set attributes with a dialog 
    	class AliasSetDialog extends JDialog  
        implements ActionListener {
    		OBNode _targetLeaf = (OBNode)_targetNode;
    		JTextField aliasField = new JTextField(_targetLeaf.getAlias(),10);
    	    JButton ok = new JButton("OK");
        	private Container contentPane;
    		public AliasSetDialog(DefaultMutableTreeNode _targetNode) {
    			super(editor,"Change AliasName",true);
    			this.setModal(true);
    			contentPane = getContentPane();
    			contentPane.setLayout(new BorderLayout());
    			contentPane.add(BorderLayout.WEST,aliasField);
    			contentPane.add(BorderLayout.EAST,ok);
    			ok.addActionListener(this);
    			setSize(260,50);
    			setLocation(200,200);
        	}
			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getActionCommand() == "OK") {
					_targetLeaf.setAlias(aliasField.getText());
					_targetNode.setUserObject(_targetLeaf);
					dispose();
				}
			}
        }
        AliasSetDialog dialog = new AliasSetDialog(_targetNode);
    	dialog.setVisible(true);
    }
*/
	private void popupNotifyDialog(final String title,final String text) {
        /* Set attributes with a dialog */
    	class notifyDialog extends JDialog  
        implements ActionListener {
			private static final long serialVersionUID = 8733681453456302201L;
			JLabel tagsField= new JLabel(text);
    	    JButton ok = new JButton("OK");
        	private Container contentPane;
    		public notifyDialog() {
    			super(editor.getFrame(), title, true);
    			this.setModal(true);
    			contentPane = getContentPane();
    			contentPane.setLayout(new BorderLayout());
    			contentPane.add(BorderLayout.WEST,tagsField);
    			contentPane.add(BorderLayout.EAST,ok);
    			ok.addActionListener(this);
    			setSize(260,50);
    			setLocation(200,200);
        	}
			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getActionCommand() == "OK") {
					dispose();
				}
			}
        }
        notifyDialog dialog = new notifyDialog();
    	dialog.setVisible(true);
    }
	
	private EditorFrame showEditorForGroup(MapPartGroup group) {
    	EditorFrame frame;
	    if(!group.haveEditorFrame()) {
	    	frame = editor.getMap().openEditorFrame(group);
	        editor.updateAll();
	        refresh();
	    } else {
	    	frame = (EditorFrame)group.getUserObject();
	    	if (!frame.isVisible()) {
	    		frame.setVisible(true);
	    	} else {
	    		/* already shown*/
	    	}
	    }
	    return frame;
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
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
