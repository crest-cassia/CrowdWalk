
package nodagumi.ananPJ.misc;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.io.*;
import javax.swing.tree.*;

import nodagumi.ananPJ.NetworkParts.OBNode;


public class DnDJTree extends JTree implements DragSourceListener, DropTargetListener, DragGestureListener {
    /**
	 * 
	 */
	private static final String NAME = "TREE-TEST";
    private static final DataFlavor localObjectFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType, NAME);
    private static final DataFlavor[] supportedFlavors = { localObjectFlavor };
    private TreeNode dropTargetNode = null;
    private TreeNode draggedNode = null;

    public DnDJTree(DefaultTreeModel _model) {
        super(_model);

        setToolTipText("");

        setCellRenderer(new DnDTreeCellRenderer());
        setModel(new DefaultTreeModel(new DefaultMutableTreeNode("default")));
        new DragSource().createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, this);
        //dropTarget = new DropTarget(this, this);
    }

    //@Override
    public void dragGestureRecognized(DragGestureEvent dge) {
        Point pt = dge.getDragOrigin();
        TreePath path = getPathForLocation(pt.x, pt.y);
        if(path==null || path.getParentPath()==null) {
            return;
        }

        draggedNode = (TreeNode) path.getLastPathComponent();
        Transferable trans = new RJLTransferable(draggedNode);
        new DragSource().startDrag(dge, Cursor.getDefaultCursor(), trans, this);
    }


    //@Override
    public void dragDropEnd(DragSourceDropEvent dsde) {
        dropTargetNode = null;
        draggedNode = null;
        repaint();
    }

    //@Override
    public void dragEnter(DragSourceDragEvent dsde) {
        dsde.getDragSourceContext().setCursor(DragSource.DefaultMoveDrop);
    }

    //@Override
    public void dragExit(DragSourceEvent dse) {
        dse.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
    }

    //@Override
    public void dragOver(DragSourceDragEvent dsde) {}

    //@Override
    public void dropActionChanged(DragSourceDragEvent dsde) {}

    // DropTargetListener events
    //@Override
    public void dragEnter(DropTargetDragEvent dtde) {}
    //@Override
    public void dragExit(DropTargetEvent dte) {}
    //@Override
    public void dragOver(DropTargetDragEvent dtde) {
        DataFlavor[] f = dtde.getCurrentDataFlavors();
        boolean isDataFlavorSupported = f[0].getHumanPresentableName().equals(NAME);
        if(!isDataFlavorSupported) {
            //
            rejectDrag(dtde);
            return;
        }
        /* figure out which cell it's over, no drag to self */
        Point pt = dtde.getLocation();
        TreePath path = getPathForLocation(pt.x, pt.y);
        if(path==null) {
            //
            rejectDrag(dtde);
            return;
        }
        Object droppedObject;
        try {
            droppedObject = dtde.getTransferable().getTransferData(localObjectFlavor);
        }catch(Exception ex) {
            rejectDrag(dtde);
            return;
        }
        MutableTreeNode droppedNode       = (MutableTreeNode) droppedObject;
        DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) targetNode.getParent();
        while(parentNode!=null) {
            if(droppedNode.equals(parentNode)) {
                //�e�m�[�h��q�m�[�h�Ƀh���b�v���悤�Ƃ��Ă���
                rejectDrag(dtde);
                return;
            }

            parentNode = (DefaultMutableTreeNode)parentNode.getParent();
        }
        
        //dropTargetNode 
        dropTargetNode = targetNode; //(TreeNode) path.getLastPathComponent();
        dtde.acceptDrag(dtde.getDropAction());
        repaint();
    }
    private void rejectDrag(DropTargetDragEvent dtde) {
        dtde.rejectDrag();
        dropTargetNode = null; // dropTargetNode(flag)
        repaint();             // Rectangle2
    }

    //@Override
    public void drop(DropTargetDropEvent dtde) {
        Object droppedObject;
        try {
            droppedObject = dtde.getTransferable().getTransferData(localObjectFlavor);
        }catch(Exception e) {
            e.printStackTrace();
            dtde.dropComplete(false);
            return;
        }
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        Point p = dtde.getLocation();
        TreePath path = getPathForLocation(p.x, p.y);
        if(path==null || !(droppedObject instanceof MutableTreeNode)) {
            dtde.dropComplete(false);
            return;
        }
        System.out.println("drop path is " + path);
        MutableTreeNode droppedNode       = (MutableTreeNode) droppedObject;
        DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) targetNode.getParent();
        if(targetNode.equals(droppedNode)) {
            //
            dtde.dropComplete(false);
            return;
        }
        dtde.acceptDrop(DnDConstants.ACTION_MOVE);
        model.removeNodeFromParent(droppedNode);
        System.out.println("[debug] now targetNode.getAllowsChildren()== " +targetNode.getAllowsChildren()); 
        if(parentNode!=null &&!targetNode.getAllowsChildren()) {
        	//parent is 
            model.insertNodeInto(droppedNode, parentNode, parentNode.getIndex(targetNode));
        }else{
        	// parent is LeafNode
            model.insertNodeInto(droppedNode, targetNode, targetNode.getChildCount());
        }
        dtde.dropComplete(true);
    }
    //@Override
    public void dropActionChanged(DropTargetDragEvent dtde) {}

    static class RJLTransferable implements Transferable {
        Object object;
        public RJLTransferable(Object o) {
            object = o;
        }
        //@Override
        public Object getTransferData(DataFlavor df) throws UnsupportedFlavorException, IOException {
            if(isDataFlavorSupported(df)) {
                return object;
            }else{
                throw new UnsupportedFlavorException(df);
            }
        }
        //@Override
        public boolean isDataFlavorSupported(DataFlavor df) {
            //return (df.getHumanPresentableName().equals(NAME));
            return (df.equals(localObjectFlavor));
        }
        //@Override
        public DataFlavor[] getTransferDataFlavors() {
            return supportedFlavors;
        }
    }

    // custom renderer
    class DnDTreeCellRenderer extends DefaultTreeCellRenderer {

		private boolean isTargetNode;
    	private boolean isTargetNodeLeaf;
        //private Insets lastItemInsets;
        Icon node_icon, link_icon, agent_icon, symlink_icon;


        public DnDTreeCellRenderer() {
            super();
            
            node_icon = new ImageIcon(getClass().getResource("/img/node_icon.png"));
            link_icon = new ImageIcon(getClass().getResource("/img/link_icon.png"));
            agent_icon = new ImageIcon(getClass().getResource("/img/agent_icon.png"));
            symlink_icon = new ImageIcon(getClass().getResource("/img/symlink_icon.png"));
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean isSelected, boolean isExpanded, boolean isLeaf,
                                                      int row,
                                                      boolean hasFocus) {
        	super.getTreeCellRendererComponent(tree, value, isSelected, isExpanded, isLeaf, row, hasFocus);
            isTargetNode = (value == dropTargetNode);
            isTargetNodeLeaf = (isTargetNode && ((TreeNode)value).getAllowsChildren());

            /* for ob_node */
            OBNode ob_node = null;
            if (value instanceof OBNode) {
            	ob_node =  (OBNode)(value);	
			} else {
				return this;
			}

            if (isLeaf) {
            	if (ob_node.getNodeType() == OBNode.NType.NODE) {
            		setIcon(node_icon);
            	} else if (ob_node.getNodeType() == OBNode.NType.LINK) {
            		setIcon(link_icon);
            	} else if (ob_node.getNodeType() == OBNode.NType.AGENT) {
            		setIcon(agent_icon);
            	} else if (ob_node.getNodeType() == OBNode.NType.SYMLINK) {
            		setIcon(symlink_icon);
            	}
            } else {
            	/* no op */
            }
            String tooltip = ob_node.getHintString();
            //System.err.println(tooltip);
            setToolTipText(tooltip);
            return this;
        }


        @Override
        public void paintComponent(Graphics g) {
        	super.paintComponent(g);
            if(isTargetNode) {
                g.setColor(Color.BLACK);
                if(!isTargetNodeLeaf) {
                    g.drawLine(0, 0, getSize().width, 0);
                }else{
                    g.drawRect(0, 0, getSize().width-1, getSize().height-1);
                }
            }
        }
    }
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
