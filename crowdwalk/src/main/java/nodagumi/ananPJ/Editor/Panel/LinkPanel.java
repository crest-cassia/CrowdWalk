// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Editor.Panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
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
import java.util.ArrayList;

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

import nodagumi.ananPJ.GuiSimulationEditorLauncher;
import nodagumi.ananPJ.GuiSimulationEditorLauncher.EditorMode;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.Link.*;

public class LinkPanel extends PanelWithTable
	implements MapLink.AttributePanel.Listener {
    GuiSimulationEditorLauncher editor = null;
    MapLinkTable shownLinks = null;
    LinksDataModel dataModel = null;
    JTable linkTable = null;
    JCheckBox placeLink = null;
    JCheckBox filter_link = null;
    JTextArea searchText = null;
    JButton searchButton = null; 
    public MapLink.AttributePanel attributePanel = null;

    final static String[] COLUMN_NAMES = { "Tags", "Length", "Width", "Parent" };

    private class LinksDataModel extends AbstractTableModel {

        public LinksDataModel(final MapLinkTable _links) {
            shownLinks = _links;
        }
        
        public int getColumnCount() { return 4; }
        public int getRowCount() { 
            synchronized(shownLinks){
                return shownLinks.size();}
        }

        public String getColumnName(final int col) {
            return COLUMN_NAMES[col].toString();
        }

        public Object getValueAt(final int row, final int col) {
            synchronized(shownLinks) {
                switch(col) {
                case 0:
                    return shownLinks.get(row).getTagString();
                case 1:
                    return shownLinks.get(row).getLength();
                case 2:
                    return shownLinks.get(row).getWidth();
                case 3:
                    return ((OBNode)shownLinks.get(row).getParent()).getTagString();
                default:
                    return "ERR(" + row + ", " + col + ")";
                }
            }
        }
    }

    public LinkPanel (GuiSimulationEditorLauncher _editor) {
        editor = _editor;
        shownLinks = editor.getLinks();
        synchronized(shownLinks){
            dataModel = new LinksDataModel(shownLinks);
        }
        linkTable = new JTable(dataModel);
        /* list of links */
        JPanel link_list_panel = new JPanel(new BorderLayout());
        setLayout(new BorderLayout());
        JScrollPane scrollpane = new JScrollPane(linkTable,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollpane.setPreferredSize(new Dimension(300, 400));

        linkTable.getSelectionModel().addListSelectionListener(this);
        link_list_panel.add(scrollpane, BorderLayout.CENTER);

        /* list manipulation */
        JPanel list_manipulation_panel = new JPanel(new GridBagLayout());
        GridBagConstraints c;
        
        filter_link = new JCheckBox("filter");
        filter_link.setSelected(false);
        filter_link.addItemListener(new ItemListener() {
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
        c = new GridBagConstraints();
        list_manipulation_panel.add(filter_link, c);

        searchText = new JTextArea("");
        searchText.setPreferredSize(new Dimension(200, 24));
        c = new GridBagConstraints();
        c.gridx = 1;
        list_manipulation_panel.add(searchText, c);
        searchButton = new JButton("Select");
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int count = 0;
                synchronized(shownLinks) {
                    for (MapLink link : shownLinks) {
                        link.selected = (null != link.matchTag(searchText.getText()));
                        if (link.selected) ++count;
                    }
                }
                repaint();
                JOptionPane.showMessageDialog(null,
                        "Found " + count + "links",
                        "Search finished",
                        JOptionPane.INFORMATION_MESSAGE);
                refresh();
            }
            
        });
        c = new GridBagConstraints();
        c.gridx = 2;
        list_manipulation_panel.add(searchButton, c);       
        
        placeLink = new JCheckBox("place link");
        placeLink.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == 2) {
                    editor.setMode(EditorMode.EDIT_LINK);
                } else {
                    editor.setMode(EditorMode.PLACE_LINK);
                }
            }
        });
        c = new GridBagConstraints();
        c.gridy = 1;
        list_manipulation_panel.add(placeLink, c);
        c = new GridBagConstraints();
        c.gridx = 1; c.gridy = 1;
        list_manipulation_panel.add(new JLabel(""), c);
        
        JButton deleteLinks = new JButton("Delete selected");
        deleteLinks.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (JOptionPane.showConfirmDialog(null, "Really remove links?", "Proceed",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
                        == JOptionPane.NO_OPTION) return;
                MapLinkTable linksToRemove = new MapLinkTable();
                synchronized(shownLinks) {
                    for (MapLink link : shownLinks) {
                        if (link.selected) linksToRemove.add(link);
                    }
                    shownLinks.removeAll(linksToRemove);
                }
                for (MapLink link : linksToRemove) {
                    System.err.println(link.getTagString());
                    OBNode parent = (OBNode) link.getParent();
                    editor.getMap().removeOBNode(parent, link, false);
                }
                refresh();
            }
        });
        c = new GridBagConstraints();
        c.gridx = 2; c.gridy = 1;
        list_manipulation_panel.add(deleteLinks, c);        
        link_list_panel.add(list_manipulation_panel, BorderLayout.SOUTH);
        add(link_list_panel, BorderLayout.CENTER);
        
        /* attributes of each link */
        attributePanel = MapLink.getAttributePanel();
        attributePanel.addListner(this);
        add(attributePanel, BorderLayout.SOUTH);
    }
    
    @Override
    public String getName() {
        return "LinkPanel";
    }
    
    private Boolean lockRefresh = false;
    private Boolean lockValueChanged = false;

    public void refresh() {
        if (lockRefresh) return;

        if (filter_link.getSelectedObjects() != null) {
            /* filter mode */
            shownLinks = new MapLinkTable();
            synchronized(shownLinks) {
                for (MapLink link : editor.getLinks()) {
                    if (null != link.matchTag(searchText.getText())) {
                        shownLinks.add(link);
                    }
                }
            }
        } else { 
            shownLinks = editor.getMap().getLinks();
        }

        lockValueChanged = true;
        dataModel.fireTableDataChanged();
        updateSelectionFromLinks();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                adjustColumnPreferredWidth(linkTable);
                linkTable.revalidate();
            }
        });
        lockValueChanged = false;
    }

    public void updateSelectionFromLinks() {
        ListSelectionModel lsm = linkTable.getSelectionModel();
        lsm.clearSelection();
        for (int r = 0; r < linkTable.getRowCount(); ++r) {
            synchronized(shownLinks){
                if (shownLinks.get(r).selected) {
                    lsm.addSelectionInterval(r, r);
                }
            }
        }
        updateAttributePanel();
    }

    public void setPlaceCheckBox(boolean b) {
        placeLink.setSelected(b);
    }
    
    public boolean getPlaceCheckBox() {
        return placeLink.getSelectedObjects() != null;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (lockValueChanged) return;
        
        updateAttributePanel();
        lockRefresh = true;
        if (!e.getValueIsAdjusting())editor.updateAll();
        lockRefresh = false;
    }
    
    private void updateAttributePanel() {
        ListSelectionModel lsm = linkTable.getSelectionModel();
        int count = 0;
        double length = 0.0, width = 0.0;
        String label = "";
        synchronized(shownLinks) {
            for (int r = 0; r < linkTable.getRowCount(); ++r) {
                MapLink link = shownLinks.get(r);
                link.selected = lsm.isSelectedIndex(r);
                if (link.selected) {
                    count++;
                    length += link.getLength();
                    width += link.getWidth();
                    for (final String tag : link.getTags()) {
                        if (!label.contains(tag)){
                            label += ", " + tag;
                        }
                    }
                }
            }
        }
        if (count == 0) return;
        length /= count;
        width /= count;

        boolean single = (count == 1);

        attributePanel.setDetectChange(false);
        if (single) {
            attributePanel.setLinkLength(length);
            attributePanel.setLengthEnabled(true);
        } else {
            attributePanel.setLinkLength(length);
            attributePanel.setLengthEnabled(false);
        }
        attributePanel.setLinkWidth(width);
        attributePanel.setDetectChange(true);
    }

    @Override
    public void valueChanged() {
        ListSelectionModel lsm = linkTable.getSelectionModel();
        int count = 0;
        synchronized(shownLinks){
            for (int r = 0; r < linkTable.getRowCount(); ++r) {
                MapLink link = shownLinks.get(r);
                link.selected = lsm.isSelectedIndex(r);
                if (link.selected) {
                    count++;
                }
            }
            if (count == 0) return;
            boolean single = count == 1;
            for (int r = 0; r < linkTable.getRowCount(); ++r) {
                MapLink link = shownLinks.get(r);
                link.selected = lsm.isSelectedIndex(r);
                if (link.selected) {
                    link.setWidth(attributePanel.getLinkWidth());
                    if (single) {
                        link.setLength(attributePanel.getLinkLength());
                        return;
                    }
                }
            }
        }
        refresh();
    }
}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
