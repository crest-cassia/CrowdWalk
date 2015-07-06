// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Editor.Panel;

import java.awt.Component;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

public abstract class PanelWithTable extends JPanel
	implements ListSelectionListener {

	protected void adjustColumnPreferredWidth(JTable table) {
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
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
