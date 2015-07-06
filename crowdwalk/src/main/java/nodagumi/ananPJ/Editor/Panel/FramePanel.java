// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Editor.Panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
//import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import nodagumi.ananPJ.GuiSimulationEditorLauncher;
import nodagumi.ananPJ.Editor.EditorFrame;

public class FramePanel extends JPanel
	implements ListSelectionListener, ActionListener {

	GuiSimulationEditorLauncher editor = null;
	ArrayList<EditorFrame> sortedFrames = null;
	JTable frameTable = null;
	JButton updateButton = null;

	final static String[] COLUMN_NAMES = { "Id", "Label", "Height" };

	private class FramesDataModel extends AbstractTableModel {

		@Override
		public int getColumnCount() {
			return 3;
		}

		@Override
		public int getRowCount() {
			return sortedFrames.size();
		}

		public String getColumnName(final int col) {
			return COLUMN_NAMES[col].toString();
		}

		@Override
		public Object getValueAt(int row, int column) {
			switch (column) {
			case 0:
				//return sortedFrames.get(row).layer;
			case 1:
				return sortedFrames.get(row).getLabel();
			case 2:
				return "" + sortedFrames.get(row).getMinHeight() + "-"
				+ sortedFrames.get(row).getMaxHeight() + "("
				+ sortedFrames.get(row).getDefaultHeight() + ")";
			}
			return null;
		}
	}
	FramesDataModel dataModel = null;

	class FrameSettings extends JPanel 
	implements ChangeListener {

		GuiSimulationEditorLauncher editor = null;
		FramePanel frame_panel = null;
		//JTextField frameName = new JTextField();
		JSpinner minHeight = new JSpinner(new SpinnerNumberModel(0.0, -10000.0, 10000.0, 0.1));
		JSpinner maxHeight = new JSpinner(new SpinnerNumberModel(0.0, -10000.0, 10000.0, 0.1));
		JSpinner defaultHeight = new JSpinner(new SpinnerNumberModel(0.0, -10000.0, 10000.0, 0.1));

		JSpinner west = new JSpinner(new SpinnerNumberModel(0.0, -10000.0, 10000.0, 0.1));
		JSpinner north = new JSpinner(new SpinnerNumberModel(0.0, -10000.0, 10000.0, 0.1));
		JSpinner east = new JSpinner(new SpinnerNumberModel(0.0, -10000.0, 10000.0, 0.1));
		JSpinner south = new JSpinner(new SpinnerNumberModel(0.0, -10000.0, 10000.0, 0.1));

		EditorFrame frameInInterest = null; 

		public FrameSettings(GuiSimulationEditorLauncher _editor,
				FramePanel _frame_panel) {
			editor = _editor;
			frame_panel = _frame_panel;

			setPreferredSize(new Dimension(40, 100));
			setLayout(new GridLayout(4, 2));
			
			//add (new JLabel("Name"));
			//add(frameName);
			add (new JLabel("min height"));
			add(minHeight);
			add (new JLabel("max height"));
			add(maxHeight);
			add (new JLabel("default height"));
			add(defaultHeight);
			add (new JLabel("west"));
			add(west);
			add (new JLabel("north"));
			add(north);
			add (new JLabel("east"));
			add(east);
			add (new JLabel("south"));
			add(south);
			
			minHeight.addChangeListener(this);
			maxHeight.addChangeListener(this);
			defaultHeight.addChangeListener(this);
			west.addChangeListener(this);
			north.addChangeListener(this);
			east.addChangeListener(this);
			south.addChangeListener(this);
			
			setSpinnersEnabled(false);
		}
		
		public void setFrameInInterest(EditorFrame frame) {
			frameInInterest = frame;
			if (frame == null) {
				setSpinnersEnabled(false);
				return;
			}

			setSpinnersEnabled(true);
			defaultHeight.setValue(frame.getDefaultHeight());
			minHeight.setValue(frame.getMinHeight());
			maxHeight.setValue(frame.getMaxHeight());
			north.setValue(frame.getNorth());
			west.setValue(frame.getWest());
			east.setValue(frame.getEast());
			south.setValue(frame.getSouth());
		}
	
		public void updateFrame() {
			if (frameInInterest == null) return;
			frameInInterest.setMinHeight(((Double)minHeight.getValue()).doubleValue());
			frameInInterest.setMaxHeight(((Double)maxHeight.getValue()).doubleValue());
			frameInInterest.setDefaultHeight(((Double)defaultHeight.getValue()).doubleValue());

			frameInInterest.setNorth(((Double)north.getValue()).doubleValue());
			frameInInterest.setWest(((Double)west.getValue()).doubleValue());
			frameInInterest.setEast(((Double)east.getValue()).doubleValue());
			frameInInterest.setSouth(((Double)south.getValue()).doubleValue());

			frame_panel.updateButton.setEnabled(false);
		}
		private void setSpinnersEnabled(boolean b) {
			minHeight.setEnabled(b);
			maxHeight.setEnabled(b);
			defaultHeight.setEnabled(b);
			west.setEnabled(b);
			north.setEnabled(b);
			east.setEnabled(b);
			south.setEnabled(b);
		}
		//public String getLabel() {
		//return frameName.getText();
		//}
		public double getMinHeight() {
			return ((Double)minHeight.getValue()).doubleValue();
		}
		public double getMaxHeight() {
			return ((Double)maxHeight.getValue()).doubleValue();
		}
		public double getDefaultHeight() {
			return ((Double)defaultHeight.getValue()).doubleValue();
		}

		private boolean changeLocked = false;

		@Override
		public void stateChanged(ChangeEvent e) {
			if (changeLocked == true) return;
			changeLocked = true;
			frame_panel.updateButton.setEnabled(true);
			if (e.getSource() == minHeight) {
				if (getDefaultHeight() < getMinHeight()) {
					defaultHeight.setValue(getMinHeight());
				}
				if (getMaxHeight() < getMinHeight()) {
					maxHeight.setValue(getMinHeight());
				}
			} else if (e.getSource() == maxHeight) {
				if (getMinHeight() > getMaxHeight()) {
					minHeight.setValue(getMaxHeight());
				}
				if (getDefaultHeight() < getMaxHeight()) {
					defaultHeight.setValue(getMaxHeight());
				}
			} else if (e.getSource() == defaultHeight) {
				if (getMinHeight() > getDefaultHeight()) {
					minHeight.setValue(getDefaultHeight());
				}
				if (getMaxHeight() < getDefaultHeight()) {
					maxHeight.setValue(getDefaultHeight());
				}
			} else {
				/* should not come here */
			}
			changeLocked = false;
				
		}
	}

	@Override
	public String getName() {
		return "FramePanel";
	}
	

	private FrameSettings settings = null;

	public FramePanel(GuiSimulationEditorLauncher _editor) {
		editor = _editor;
		//sortedFrames = editor.getLayers();
		setLayout(new BorderLayout());
		
		dataModel = new FramesDataModel(); 
		frameTable = new JTable(dataModel);
		JScrollPane tscrollpane = new JScrollPane(frameTable,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		tscrollpane.setPreferredSize(new Dimension(300, 235));

		frameTable.getSelectionModel().addListSelectionListener(this);
		add(tscrollpane, BorderLayout.NORTH);		

		JButton removeButton = new JButton("Remove Frame");
		removeButton.setPreferredSize(new Dimension(150, 10));
		removeButton.addActionListener(this);
		add(removeButton, BorderLayout.WEST);
		
		updateButton = new JButton("Update Frame");
		updateButton.setPreferredSize(new Dimension(150, 10));
		updateButton.addActionListener(this);
		updateButton.setEnabled(false);
		add(updateButton, BorderLayout.EAST);

		settings = new FrameSettings(editor, this);
		JScrollPane sscrollpane = new JScrollPane(settings,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		sscrollpane.setPreferredSize(new Dimension(300, 235));
		add(sscrollpane, BorderLayout.SOUTH);		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("Update Frame")) {
			settings.updateFrame();
			updateButton.setEnabled(false);
			refresh();
		} else if (e.getActionCommand().equals("Remove Frame")) {
			ArrayList<EditorFrame> framesToRemove = new ArrayList<EditorFrame>();
			ListSelectionModel lsm = frameTable.getSelectionModel();
			for (int r = 0; r < frameTable.getRowCount(); ++r) {
				if(lsm.isSelectedIndex(r)){
					framesToRemove.add(sortedFrames.get(r));
				}
			}
			//editor.getLayers().removeAll(framesToRemove);
			refresh();
		}
	}

	private Boolean lockRefresh = false;
	private Boolean lockValueChanged = false;

	public void refresh() {
		if (lockRefresh) return;

		//sortedFrames = (ArrayList<EditorFrame>) editor.getLayers().clone();
		Collections.sort(sortedFrames, new Comparator<EditorFrame>() {
			@Override
			public int compare(EditorFrame lhs, EditorFrame rhs) {
				//if (lhs.layer != rhs.layer) {
					//return lhs.layer - rhs.layer;
				//} else 
				if (lhs.getDefaultHeight() != rhs.getDefaultHeight()) {
					return (int)(lhs.getDefaultHeight() - rhs.getDefaultHeight());
				}
				return 0;
			}
		});
		lockValueChanged = true;
		dataModel.fireTableDataChanged();
		updateSelectionFromFrames();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				adjustColumnPreferredWidth(frameTable);
				frameTable.revalidate();
			}
		});
		lockValueChanged = false;
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

	public void updateSelectionFromFrames() {
		ListSelectionModel lsm = frameTable.getSelectionModel();
		lsm.clearSelection();
		for (int r = 0; r < frameTable.getRowCount(); ++r) {
			if (sortedFrames.get(r).isVisible()) {
				lsm.addSelectionInterval(r, r);
			}
		}
	}

	@Override
	/* when the selection in the table is changed */
	public void valueChanged(ListSelectionEvent e) {
		if (lockValueChanged) return;
		ListSelectionModel lsm = frameTable.getSelectionModel();
		int selected_count = 0;
		EditorFrame selectedFrame = null;
		for (int r = 0; r < frameTable.getRowCount(); ++r) {
			if (lsm.isSelectedIndex(r)) {
				sortedFrames.get(r).setVisible(true);
				selectedFrame = sortedFrames.get(r);
				++selected_count;
			} else {
				sortedFrames.get(r).setVisible(false);
			}
		}
		if (selected_count == 1) {
			settings.setFrameInInterest(selectedFrame);
			updateButton.setEnabled(true);
		} else {
			settings.setFrameInInterest(null);
			updateButton.setEnabled(false);
		}
		lockRefresh = true;
		if (!e.getValueIsAdjusting())editor.updateAll();
		lockRefresh = false;
	}
}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
