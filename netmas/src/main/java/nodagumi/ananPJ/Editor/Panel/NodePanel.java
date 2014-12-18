package nodagumi.ananPJ.Editor.Panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;

import nodagumi.ananPJ.NetworkMapEditor;
import nodagumi.ananPJ.NetworkMapEditor.EditorMode;
import nodagumi.ananPJ.NetworkParts.MapPartGroup;
import nodagumi.ananPJ.NetworkParts.Node.*;

public class NodePanel extends PanelWithTable implements Serializable {
	private static final long serialVersionUID = -5011285748841425804L;
	NetworkMapEditor editor = null;
	MapNodeTable sortedNodes = null;
	NodesDataModel dataModel = null;
	JTable nodeTable = null;
	JCheckBox placeNode = null;

	JCheckBox filterNode = null;
	JTextArea searchText = null;
	JButton searchButton = null;
	//public MapNode.AttributePanel attributePanel = null;

	final static String[] COLUMN_NAMES = { "Tags", "Is Exit", "Position", "Parent" };

	private class NodesDataModel extends AbstractTableModel {
		private static final long serialVersionUID = -8473021012580835372L;

		public int getColumnCount() { return 4; }
		public int getRowCount() { return sortedNodes.size();}

		public String getColumnName(final int col) {
			return COLUMN_NAMES[col].toString();
		}

		public Object getValueAt(final int row, final int col) {
			switch(col) {
			case 0:
				return sortedNodes.get(row).getTagString();
			case 1:
				return sortedNodes.get(row).hasTag("Exit");
			case 2:
			{
				MapNode node = sortedNodes.get(row);
				return node.getX() + ", " + node.getY() + ", "+ node.getHeight();
			}
			case 3:
			{
				MapNode node = sortedNodes.get(row);
				MapPartGroup parent = (MapPartGroup)node.getParent();
				return parent.getTagString();
			}
			}
			return "ERR(" + row + ", " + col + ")";
		}
	}
	
	public NodePanel (NetworkMapEditor _editor) {
		editor = _editor;
		sortedNodes = editor.getMap().getNodes();
		dataModel = new NodesDataModel();
		nodeTable = new JTable(dataModel);
		
		setLayout(new BorderLayout());
		
		JScrollPane scrollpane = new JScrollPane(nodeTable,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollpane.setPreferredSize(new Dimension(300, 480));

		nodeTable.getSelectionModel().addListSelectionListener(this);
		add(scrollpane, BorderLayout.NORTH);
		
		/* filter/search nodes */
		JPanel filterPanel = new JPanel(new GridLayout(2, 3));

		filterNode = new JCheckBox("filter");
		filterNode.setSelected(false);
		filterNode.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (((JCheckBox)e.getItem()).getSelectedObjects()
						!= null) {
					searchButton.setText("Filter");
				} else {
					searchButton.setText("Select");
				}
				refresh();
			}
		});
		filterPanel.add(filterNode);

		searchText = new JTextArea("");
		filterPanel.add(searchText);
		searchButton = new JButton("Search");
		searchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int count = 0;
				for (MapNode node : editor.getMap().getNodes()) {
					node.selected = (null != node.matchTag(searchText.getText()));
					if (node.selected) ++count;
				}
				repaint();
				JOptionPane.showMessageDialog(null,
						"Found " + count + "nodes",
						"Search finished",
						JOptionPane.INFORMATION_MESSAGE);
				refresh();
			}
			
		});
		filterPanel.add(searchButton);

		/* place mode */
		placeNode = new JCheckBox("place node");
		placeNode.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == 2) {
					editor.setMode(EditorMode.EDIT_NODE);
				} else {
					editor.setMode(EditorMode.PLACE_NODE);
				}
			}
		});
		filterPanel.add(placeNode, BorderLayout.CENTER);
		filterPanel.add(new JLabel(""));
		filterPanel.add(new JLabel(""));

		add(filterPanel);
	}

	@Override
	public String getName() {
		return "NodePanel";
	}

	private boolean lockRefresh = false;
	private boolean lockValueChanged = false;

	public void refresh() {
		if (lockRefresh) return;

		sortedNodes = editor.getMap().getNodes();
		Collections.sort(sortedNodes, new Comparator<MapNode>() {
			@Override
			public int compare(MapNode lhs, MapNode rhs) {
				return (int)((lhs.getHeight() - rhs.getHeight())*10);
			}
		});
		
		lockValueChanged = true;
		dataModel.fireTableDataChanged();
		updateSelectionFromNodes();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				adjustColumnPreferredWidth(nodeTable);
				nodeTable.revalidate();
			}
		});
		lockValueChanged = false;
	}

	public void updateSelectionFromNodes() {
		ListSelectionModel lsm = nodeTable.getSelectionModel();
		lsm.clearSelection();
		for (int r = 0; r < nodeTable.getRowCount(); ++r) {
			if (sortedNodes.get(r).selected) {
				lsm.addSelectionInterval(r, r);
			}
		}
	}

	public void setPlaceCheckBox(boolean b) {
		placeNode.setSelected(b);
	}
	
	public boolean getPlaceCheckBox() {
		return placeNode.getSelectedObjects() != null;
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (lockValueChanged) return;
		ListSelectionModel lsm = nodeTable.getSelectionModel();
		for (int r = 0; r < nodeTable.getRowCount(); ++r) {
			sortedNodes.get(r).selected = lsm.isSelectedIndex(r);
		}
		lockRefresh = true;
		if (!e.getValueIsAdjusting())editor.updateAll();
		lockRefresh = false;
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
