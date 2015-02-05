package nodagumi.ananPJ.Editor.Panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
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
import nodagumi.ananPJ.Agents.RunningAroundPerson;
import nodagumi.ananPJ.Editor.AgentFactory;
import nodagumi.ananPJ.NetworkMapEditor.EditorMode;
import nodagumi.ananPJ.NetworkParts.OBNode;
import nodagumi.ananPJ.NetworkParts.Link.MapLink;

import nodagumi.Itk.*;

public class AgentPanel extends JPanel
    implements ListSelectionListener, Serializable {
    private static final long serialVersionUID = 8975068495519079933L;
    private NetworkMapEditor editor = null;
    private ArrayList<AgentBase> agents = null;
    private AgentsDataModel data_model = null;
    public AgentFactory agentFactory = null;
    
    JTable agent_table = null;
    JCheckBox placeAgent = null;
    JTextField agent_place_tag = null;

    final static String[] COLUMN_NAMES = { "Goal", "Type", "Position", "Route" };

    private class AgentsDataModel extends AbstractTableModel {
        private static final long serialVersionUID = 6682455819993737049L;

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
                if (agent instanceof RunningAroundPerson) {
                    RunningAroundPerson rp = (RunningAroundPerson)agent;
                    return rp.getGoal();
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
                if (agent instanceof RunningAroundPerson) {
                    RunningAroundPerson rp = (RunningAroundPerson)agent;
                    StringBuffer route_string = new StringBuffer(); 
                    for (Term via : rp.getPlannedRoute()) {
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
        agentFactory = new AgentFactory(editor, _random);
        setLayout(new BorderLayout());
        
        JPanel agent_list_panel = new JPanel(new BorderLayout());
        JScrollPane tscrollpane = new JScrollPane(agent_table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        tscrollpane.setPreferredSize(new Dimension(300, 240));

        agent_table.getSelectionModel().addListSelectionListener(this);
        agent_list_panel.add(tscrollpane, BorderLayout.CENTER);

        /* manipulate list */
        JPanel button_panel = new JPanel(new GridLayout(1, 2));
        
        placeAgent = new JCheckBox("place agent");
        placeAgent.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == 2) {
                    editor.setMode(EditorMode.EDIT_AGENT);
                } else {
                    editor.setMode(EditorMode.PLACE_AGENT);
                }
            }
        });
        button_panel.add(placeAgent);

        JButton deleteAgents = new JButton("Delete selected agents");
        deleteAgents.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (JOptionPane.showConfirmDialog(null, "Really remove agents?", "Proceed",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
                        == JOptionPane.NO_OPTION) return;
                ArrayList<AgentBase> agentsToRemove = new ArrayList<AgentBase>();
                for (AgentBase agent : agents) {
                    if (agent.selected) agentsToRemove.add(agent);
                }
                
                for (AgentBase agent : agentsToRemove) {
                    OBNode parent = (OBNode)agent.getParent();
                    editor.getMap().removeOBNode(parent, agent, true);
                }
                refresh();
            }
        });
        button_panel.add(deleteAgents);
        agent_list_panel.add(button_panel, BorderLayout.SOUTH);
        add(agent_list_panel, BorderLayout.CENTER);     

        /* agent generation */
        JPanel generate_agent_panel = new JPanel(new BorderLayout());
        generate_agent_panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.BLACK), "generate agent"));
        JPanel generate_manip_panel = new JPanel(new FlowLayout());

        generate_manip_panel.add(new JLabel(" tag to place agent:"));
        agent_place_tag = new JTextField();
        agent_place_tag.setPreferredSize(new Dimension(300, 24));
        generate_manip_panel.add(agent_place_tag);
        JButton generateRandom = new JButton("Generate Randomly");
        generateRandom.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
				Itk.dbgErr("!!! placeAgentsRandomly() needs target now !!!") ;
            } 
        });
        generate_manip_panel.add(generateRandom);
        JButton generateEven = new JButton("Generate Evenly");
        generateEven.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) { 
				Itk.dbgErr("!!! placeAgentsEvenly() needs target now !!!") ;
			}
        });
        generate_manip_panel.add(generateEven);
        generate_agent_panel.add(generate_manip_panel, BorderLayout.NORTH);

        /* type of agent */
        generate_agent_panel.add(agentFactory, BorderLayout.CENTER);

        add(generate_agent_panel, BorderLayout.SOUTH);
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

    public void setPlaceCheckBox(boolean b) {
        placeAgent.setSelected(b);
    }
    
    public boolean getPlaceCheckBox() {
        return placeAgent.getSelectedObjects() != null;
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
