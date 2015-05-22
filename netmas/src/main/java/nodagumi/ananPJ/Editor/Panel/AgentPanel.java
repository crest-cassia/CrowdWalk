// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Editor.Panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Random;

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
import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Agents.WalkAgent;
import nodagumi.ananPJ.NetworkParts.Link.MapLink;

import nodagumi.Itk.*;

public class AgentPanel extends JPanel
	implements ListSelectionListener {
    private NetworkMapEditor editor = null;
    private ArrayList<AgentBase> agents = null;
    private AgentsDataModel data_model = null;
    
    JTable agent_table = null;

    final static String[] COLUMN_NAMES = { "Goal", "Type", "Position", "Route" };

    private class AgentsDataModel extends AbstractTableModel {

        public int getColumnCount() { return 4; }
        public int getRowCount() { return agents.size();}

        public String getColumnName(final int col) {
            return COLUMN_NAMES[col].toString();
        }

        public Object getValueAt(final int row, final int col) {
            AgentBase agent = agents.get(row);
            switch(col) {
            case 0:
            {
                if (agent instanceof WalkAgent) {
                    WalkAgent wagent = (WalkAgent)agent;
                    return wagent.getGoal();
                }
                return "(NO GOAL)";
            }
            case 1:
                return agent.getClass();
            case 2:
            {
				MapLink link = agent.getCurrentLink() ;
                if (link == null) return "";
                return link.getTagString() + " " + agent.getPositionOnLink();
            }
            case 3:
            {
                if (agent instanceof WalkAgent) {
                    WalkAgent wagent = (WalkAgent)agent;
                    StringBuffer route_string = new StringBuffer(); 
                    for (Term via : wagent.getPlannedRoute()) {
                        route_string.append("," + via);
                    }
                    return route_string.toString();
                } else {
                    return "";
                }
            }
            default:

            }
            return "ERR(" + row + ", " + col + ")";
        }
    }
    
    public AgentPanel (NetworkMapEditor _editor, Random _random) {
        editor = _editor;
        agents = editor.getMap().getAgents();
        data_model = new AgentsDataModel();
        agent_table = new JTable(data_model);
        setLayout(new BorderLayout());
        
        JPanel agent_list_panel = new JPanel(new BorderLayout());
        JScrollPane tscrollpane = new JScrollPane(agent_table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        tscrollpane.setPreferredSize(new Dimension(300, 240));

        agent_table.getSelectionModel().addListSelectionListener(this);
        agent_list_panel.add(tscrollpane, BorderLayout.CENTER);

        add(agent_list_panel, BorderLayout.CENTER);     
    }
    
    @Override
    public String getName() {
        return "AgentPanel";
    }
    
    private Boolean lockRefresh = false;
    private Boolean lockValueChanged = false;

    public void refresh() {
        if (lockRefresh) return;

        agents = editor.getMap().getAgents();
        lockValueChanged = true;
        data_model.fireTableDataChanged();
        updateSelectionFromAgents();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                adjustColumnPreferredWidth(agent_table);
                agent_table.revalidate();
            }
        });
        lockValueChanged = false;
    }

    public void updateSelectionFromAgents() {
        ListSelectionModel lsm = agent_table.getSelectionModel();
        lsm.clearSelection();
        for (int r = 0; r < agent_table.getRowCount(); ++r) {
            if (agents.get(r).selected) {
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

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (lockValueChanged) return;
        ListSelectionModel lsm = agent_table.getSelectionModel();
        for (int r = 0; r < agent_table.getRowCount(); ++r) {
            agents.get(r).selected = lsm.isSelectedIndex(r);
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
