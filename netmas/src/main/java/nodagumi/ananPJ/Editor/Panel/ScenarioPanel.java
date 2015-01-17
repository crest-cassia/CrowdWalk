package nodagumi.ananPJ.Editor.Panel;

import java.awt.BorderLayout;
import java.awt.FileDialog;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;

import nodagumi.ananPJ.NetworkMap;
import nodagumi.ananPJ.NetworkMapEditor;
import nodagumi.ananPJ.NetworkParts.OBNode;
import nodagumi.ananPJ.misc.AgentGenerationFile;

public class ScenarioPanel extends PanelWithTable implements Serializable {
    private static final long serialVersionUID = 5603704970745259422L;
    NetworkMapEditor editor = null;
    JLabel generateFileLabel = null;
    JButton generateFileButton = null;
    JLabel responseFileLabel = null;
    JButton responseFileButton = null;
    Random random = null;

    JTable generationTable = null;
    final static String[] COLUMN_NAMES = { "Goal", "Start", "Path" };
    private class AgentGenerationDataModel extends AbstractTableModel {
        private static final long serialVersionUID = 8846594313399230323L;
        AgentGenerationFile file = null;
        public AgentGenerationDataModel() {
        }

        public void setFile(File _file) {
            if (_file != null) { 
                try {
                    file = new AgentGenerationFile(_file.getPath(),
												   editor.getMap(),
												   true, 1.0, random);
                } catch(Exception ex) {
					ex.printStackTrace();
                    System.err.printf("Illegal AgentGenerationFile: %s\n%s", _file.getPath(), ex.getMessage());
                    System.exit(1);
                }
            } else {
                file = null;
            }
        }
        
        public int getColumnCount() { return 3; }
        public int getRowCount() { 
            if (file == null) return 0;
            else return file.size();
        }

        public String getColumnName(final int col) {
            return COLUMN_NAMES[col].toString();
        }

        public Object getValueAt(final int row, final int col) {
            switch(col) {
            case 0:
                return file.get(row).goal;
            case 1:
                return file.get(row).getStart();
            case 2:
                return file.get(row).getPlannedRoute();
            }
            return "ERR(" + row + ", " + col + ")";
        }
        
        public OBNode getStart(final int row) {
            return file.get(row).getStartObject();
        }
    }
    AgentGenerationDataModel generationDataModel = null;

    JPanel button_panel;
    public ScenarioPanel(NetworkMapEditor _editor, Random _random) {
        editor = _editor;
        random = _random;
        setLayout(new BorderLayout());
        button_panel = new JPanel(new GridLayout(2, 2));
        /* agent generation scenario */
        generateFileLabel = new JLabel("Generate:");
        button_panel.add(generateFileLabel);
        generateFileButton = new JButton("Open Generation File");
        generateFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                open_generation_file();
            }
        });
        button_panel.add(generateFileButton);
        
        /* emergency response scenario */
        responseFileLabel = new JLabel("Response:");
        button_panel.add(responseFileLabel);

        responseFileButton = new JButton("Open Response File");
        responseFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                open_response_file();
            }
        });
        button_panel.add(responseFileButton);
        add(button_panel, BorderLayout.NORTH);

        generationDataModel = new AgentGenerationDataModel();
        generationTable = new JTable(generationDataModel);
        generationTable.getSelectionModel().addListSelectionListener(this);

        JScrollPane scrollpane = new JScrollPane(generationTable,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        add(scrollpane, BorderLayout.CENTER);
    }

    private void open_generation_file() {
        FileDialog fd = new FileDialog(editor.getFrame(), 
                "Set agent generation scenario", FileDialog.LOAD);

        fd.setFile("");
        fd.setDirectory(editor.getDirName());
        fd.setVisible (true);
        if (fd.getFile() == null) return;

        String filename = fd.getDirectory() + fd.getFile();
        editor.getMap().setGenerationFile(filename);

        refresh();
    }

    private void open_response_file() {
        FileDialog fd = new FileDialog(editor.getFrame(), 
                "Set emergency response scenario", FileDialog.LOAD);

        fd.setFile("");
        fd.setDirectory(editor.getDirName());
        fd.setVisible (true);
        if (fd.getFile() == null) return;

        String filename = fd.getDirectory() + fd.getFile();
        editor.getMap().setResponseFile(filename);

        refresh();
    }

    @Override
    public String getName() {
        return "ScenarioPanel";
    }

    public void refresh() {
        if (editor.getMap().getGenerationFile() != null) {
            File generation_file = new File(editor.getMap().getGenerationFile());
            String message = "";
            if (!generation_file.exists()) {
                message += " (error: does not exist)";
            } else if (generation_file.isDirectory()) {
                message += " (error: is directory)";
            } else if (!generation_file.canRead()) {
                message += " (error: unreadable)";
            }
            generateFileLabel.setText("Generation: " +
                    generation_file.getName() +
                    message);
            generateFileButton.setToolTipText(generation_file.getPath());
            generationDataModel.setFile(generation_file);
        } else {
            generateFileLabel.setText("Generation: not given");
            generateFileButton.setToolTipText(null);
            generationDataModel.setFile(null);
        }

        if (editor.getMap().getResponseFile() != null) {
            File response_file = new File(editor.getMap().getResponseFile());
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    adjustColumnPreferredWidth(generationTable);
                    generationTable.revalidate();
                }
            });
            String message = "";
            if (!response_file.exists()) {
                message += " (error: does not exist)";
            } else if (response_file.isDirectory()) {
                message += " (error: is directory)";
            } else if (!response_file.canRead()) {
                message += " (error: unreadable)";
            }
            responseFileLabel.setText("Response  : " +
                    response_file.getName() +
                    message);
            responseFileButton.setToolTipText(response_file.getPath());
        } else {
            responseFileLabel.setText("Response  : not given");
            responseFileButton.setToolTipText(null);
        }
        repaint();
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        ListSelectionModel lsm = generationTable.getSelectionModel();
        for (int r = 0; r < generationTable.getRowCount(); ++r) {
            if (lsm.isSelectedIndex(r)) {
                generationDataModel.getStart(r).selected = true;
            }
        }
    }

    public void setRandom(Random _random) {
        random = _random;
    }
}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
