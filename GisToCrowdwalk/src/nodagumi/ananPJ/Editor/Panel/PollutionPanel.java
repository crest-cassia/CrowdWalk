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
import java.io.Serializable;
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
import nodagumi.ananPJ.NetworkParts.Pollution.PollutedArea;

public class PollutionPanel extends JPanel 
implements ActionListener, ListSelectionListener, Serializable {
	private static final long serialVersionUID = -8907318830563961670L;
	NetworkMapEditor editor = null;
	JLabel pollutionFileLabel = null;

	RoomDataModel dataModel = null;
	JTable roomTable = null;
	JCheckBox placeRoom = null;

	final static String[] COLUMN_NAMES = { "ID", "Area",
		"Tags"};

	private class RoomDataModel extends AbstractTableModel {
		private static final long serialVersionUID = -4213633342883812858L;
		
		public RoomDataModel() {
		}

		public int getColumnCount() { return 3; }
		public int getRowCount() { return editor.getMap().getRooms().size();}

		public String getColumnName(final int col) {
			return COLUMN_NAMES[col].toString();
		}

		public Object getValueAt(final int row, final int col) {
			PollutedArea area = editor.getMap().getRooms().get(row);
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
				editor.getMap().getPollutionFile());

		pollutionFilePanel.add(pollutionFileLabel);
		JButton openButton = new JButton("Open");
		openButton.addActionListener(this);
		pollutionFilePanel.add(openButton);
		add(pollutionFilePanel, BorderLayout.NORTH);

		placeRoom = new JCheckBox("place room");
		placeRoom.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == 2) {
					editor.setMode(EditorMode.EDIT_POLLUTION);
				} else {
					editor.setMode(EditorMode.PLACE_POLLUTION);
				}
			}
		});
		add(placeRoom, BorderLayout.CENTER);
		
		dataModel = new RoomDataModel();
		roomTable = new JTable(dataModel);
		
		JScrollPane scrollpane = new JScrollPane(roomTable,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollpane.setPreferredSize(new Dimension(300, 480));

		roomTable.getSelectionModel().addListSelectionListener(this);
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
		updateSelectionFromRooms();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				adjustColumnPreferredWidth(roomTable);
				roomTable.revalidate();
			}
		});
		lockValueChanged = false;
		if (editor.getMap().getPollutionFile() != null) {
			File pollution_file = new File(editor.getMap().getPollutionFile());
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
	    	editor.getMap().setPollutionFile(filename);

	    	repaint();
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (lockValueChanged) return;
		ListSelectionModel lsm = roomTable.getSelectionModel();
		for (int r = 0; r < roomTable.getRowCount(); ++r) {
			editor.getMap().getRooms().get(r).selected = lsm.isSelectedIndex(r);
		}
		lockRefresh = true;
		if (!e.getValueIsAdjusting())editor.updateAll();
		lockRefresh = false;
	}

	public void updateSelectionFromRooms() {
		ListSelectionModel lsm = roomTable.getSelectionModel();
		lsm.clearSelection();
		for (int r = 0; r < roomTable.getRowCount(); ++r) {
			if (editor.getMap().getRooms().get(r).selected) {
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
		placeRoom.setSelected(b);
	}
	
	public boolean getPlaceCheckBox() {
		return placeRoom.getSelectedObjects() != null;
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
