// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Editor.Panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import nodagumi.ananPJ.NetworkMapEditor;
import nodagumi.ananPJ.NetworkMapEditor.EditorMode;
import nodagumi.ananPJ.NetworkMap.Area.MapArea;

public class PollutionPanel extends JPanel 
	implements ActionListener, ListSelectionListener {
	NetworkMapEditor editor = null;
	JLabel pollutionFileLabel = null;

	AreaDataModel dataModel = null;
	JTable areaTable = null;
	JCheckBox placeArea = null;

	final static String[] COLUMN_NAMES = { "ID", "Area",
		"Tags"};

	private class AreaDataModel extends AbstractTableModel {
		
		public AreaDataModel() {
		}

		public int getColumnCount() { return 3; }
		public int getRowCount() { return editor.getMap().getAreas().size();}

		public String getColumnName(final int col) {
			return COLUMN_NAMES[col].toString();
		}

		public Object getValueAt(final int row, final int col) {
			ArrayList<MapArea> areas = editor.getMap().getAreas();
			if (areas.isEmpty()) {
				return "ERR(" + row + ", " + col + ")";
			}
			MapArea area = areas.get(row);
			switch(col) {
			case 0:
			{
				Matcher match = area.matchTag("(\\d+)");
				if (match == null) { return "(not assigned)";}
				else { return match.group(0); }
			}
			case 1:
				Rectangle bounds = area.getShape().getBounds();
				return "(" + bounds.x + ", " +
				bounds.y + ")-(" + (bounds.x + bounds.width) + ", " +
				(bounds.y + bounds.height) + ")";
			case 2:
				return area.getTagString();
			}
			return "ERR(" + row + ", " + col + ")";
		}
	}

	private JPanel pollutionFilePanel;
	public PollutionPanel(NetworkMapEditor _editor) {
		editor = _editor;
		setLayout(new BorderLayout());
		
		/* pollution file */
		pollutionFilePanel = new JPanel(new GridLayout(2, 1));
		pollutionFileLabel = new JLabel("Pollution Settings: " +
				editor.getSetupFileInfo().getPollutionFile());

		pollutionFilePanel.add(pollutionFileLabel);
		JButton openButton = new JButton("Open");
		openButton.addActionListener(this);
		pollutionFilePanel.add(openButton);
		add(pollutionFilePanel, BorderLayout.NORTH);

		placeArea = new JCheckBox("place area");
		placeArea.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == 2) {
					editor.setMode(EditorMode.EDIT_POLLUTION);
				} else {
					editor.setMode(EditorMode.PLACE_POLLUTION);
				}
			}
		});
		add(placeArea, BorderLayout.CENTER);
		
		dataModel = new AreaDataModel();
		areaTable = new JTable(dataModel);
		
		JScrollPane scrollpane = new JScrollPane(areaTable,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollpane.setPreferredSize(new Dimension(300, 480));

		areaTable.getSelectionModel().addListSelectionListener(this);
		add(scrollpane, BorderLayout.SOUTH);
	}

	private boolean lockRefresh = false;
	private boolean lockValueChanged = false;

	@Override
	public String getName() {
		return "PollutionPanel";
	}

	public void refresh() {
		if (lockRefresh) return;

		lockValueChanged = true;
		dataModel.fireTableDataChanged();
		updateSelectionFromAreas();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				adjustColumnPreferredWidth(areaTable);
				areaTable.revalidate();
			}
		});
		lockValueChanged = false;
		if (editor.getSetupFileInfo().getPollutionFile() != null) {
			File pollution_file =
                new File(editor.getSetupFileInfo().getPollutionFile());
			String message = "";
			if (!pollution_file.exists()) {
				message += " (error: does not exist)";
			} else if (pollution_file.isDirectory()) {
				message += " (error: is directory)";
			} else if (!pollution_file.canRead()) {
				message += " (error: unreadable)";
			}
			pollutionFileLabel.setText("Pollution Settings: " +
					pollution_file.getName() +
					message);
		} else {		
			pollutionFileLabel.setText("Pollution Settings: file not set");
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("Open")) {
			FileDialog fd = new FileDialog(editor.getFrame(), 
					"Set pollution scenario", FileDialog.LOAD);

	    	String dirName = null, fileName = null; 
	    	dirName = editor.getDirName();
	    	fileName = "";

	    	fd.setFile(fileName);
	    	fd.setDirectory(dirName);
	    	fd.setVisible (true);
	        
	    	if (fd.getFile() == null) return;
	    	String filename = fd.getDirectory() + fd.getFile();
	    	pollutionFileLabel.setText("Pollution Settings: " + filename);
	    	editor.getSetupFileInfo().setPollutionFile(filename);

	    	repaint();
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (lockValueChanged) return;
		ListSelectionModel lsm = areaTable.getSelectionModel();
		for (int r = 0; r < areaTable.getRowCount(); ++r) {
			editor.getMap().getAreas().get(r).selected = lsm.isSelectedIndex(r);
		}
		lockRefresh = true;
		if (!e.getValueIsAdjusting())editor.updateAll();
		lockRefresh = false;
	}

	public void updateSelectionFromAreas() {
		ListSelectionModel lsm = areaTable.getSelectionModel();
		lsm.clearSelection();
		for (int r = 0; r < areaTable.getRowCount(); ++r) {
			if (editor.getMap().getAreas().get(r).selected) {
				lsm.addSelectionInterval(r, r);
			}
		}
	}

	private void adjustColumnPreferredWidth(JTable table) {
		TableColumnModel columnModel = table.getColumnModel();
		for (int col = 0; col < table.getColumnCount(); ++ col) {
			int max_width = 0;
			
			for (int row = 0; row < table.getRowCount(); ++ row) {
				TableCellRenderer rend = table.getCellRenderer(row, col);
				Object o = table.getValueAt(row,col);
				Component c = rend.getTableCellRendererComponent(table, o,
						false, false, row, col);
				max_width = Math.max(c.getPreferredSize().width, max_width);
			}
			TableColumn column = columnModel.getColumn(col);
			column.setPreferredWidth(max_width);
		}
	}

	/* checkbox */
	public void setPlaceCheckBox(boolean b) {
		placeArea.setSelected(b);
	}
	
	public boolean getPlaceCheckBox() {
		return placeArea.getSelectedObjects() != null;
	}
}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
