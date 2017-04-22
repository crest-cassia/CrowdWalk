// -*- mode: java; indent-tabs-mode: nil -*-
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

import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.GuiSimulationEditorLauncher;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.misc.AgentGenerationFile;

import nodagumi.Itk.*;

public class ScenarioPanel extends PanelWithTable { 
    GuiSimulationEditorLauncher editor = null;
    JLabel generateFileLabel = null;
    JButton generateFileButton = null;
    JLabel scenarioFileLabel = null;
    JButton scenarioFileButton = null;
	JLabel fallbackFileLabel = null ;
	JButton fallbackFileButton = null ;
    Random random = null;

    JTable generationTable = null;
    final static String[] COLUMN_NAMES = { "Goal", "Start", "Path" };
    private class AgentGenerationDataModel extends AbstractTableModel {
        AgentGenerationFile file = null;
        public AgentGenerationDataModel() {
        }

        public void setFile(File _file) {
            if (_file != null) {
                try {
					Term fallbackParameters =
						editor.getSetupFileInfo().fallbackParameters ;
                    file = new AgentGenerationFile(_file.getPath(),
												   editor.getMap(),
												   fallbackParameters,
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
                return file.get(row).getStartInfo();
            case 2:
                return file.get(row).getPlannedRoute();
            }
            return "ERR(" + row + ", " + col + ")";
        }
        
        public OBNode getStartObject(final int row) {
            return file.get(row).getStartObject();
        }
    }
    AgentGenerationDataModel generationDataModel = null;

    JPanel button_panel;
    public ScenarioPanel(GuiSimulationEditorLauncher _editor, Random _random) {
        editor = _editor;
        random = _random;
        setLayout(new BorderLayout());
        button_panel = new JPanel(new GridLayout(3, 2));
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
        
        /* emergency scenario scenario */
        scenarioFileLabel = new JLabel("Scenario:");
        button_panel.add(scenarioFileLabel);

        scenarioFileButton = new JButton("Open Scenario File");
        scenarioFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                open_scenario_file();
            }
        });
        button_panel.add(scenarioFileButton);

        /* fallback file */
        fallbackFileLabel = new JLabel("Fallback:");
        button_panel.add(fallbackFileLabel);

        fallbackFileButton = new JButton("Open Fallback File");
        fallbackFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                open_fallback_file();
            }
        });
        button_panel.add(fallbackFileButton);

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

        editor.setFileDialogPath(fd, editor.getGenerationFile(), "generation");
        fd.setVisible (true);
        if (fd.getFile() == null) return;

        String filename = fd.getDirectory() + fd.getFile();
        editor.setGenerationFile(filename);

        refresh();
    }

    private void open_scenario_file() {
        FileDialog fd = new FileDialog(editor.getFrame(), 
                "Set emergency scenario", FileDialog.LOAD);

        editor.setFileDialogPath(fd, editor.getScenarioFile(), "scenario");
        fd.setVisible (true);
        if (fd.getFile() == null) return;

        String filename = fd.getDirectory() + fd.getFile();
        editor.setScenarioFile(filename);

        refresh();
    }

	//------------------------------------------------------------
	/**
	 * set fallback file and scan it.
	 */
    private void open_fallback_file() {
        FileDialog fd = new FileDialog(editor.getFrame(), 
                "Set emergency scenario", FileDialog.LOAD);

        editor.setFileDialogPath(fd, editor.getFallbackFile(), "fallback");
        fd.setVisible (true);
        if (fd.getFile() == null) return;

        String filename = fd.getDirectory() + fd.getFile();
        editor.setFallbackFile(filename, null) ;

        refresh();
    }

    @Override
    public String getName() {
        return "ScenarioPanel";
    }

    public void refresh() {
        if (editor.getSetupFileInfo().getGenerationFile() != null) {
            File generation_file =
                new File(editor.getSetupFileInfo().getGenerationFile());
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

        if (editor.getSetupFileInfo().getScenarioFile() != null) {
            File scenario_file =
                new File(editor.getSetupFileInfo().getScenarioFile());
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    adjustColumnPreferredWidth(generationTable);
                    generationTable.revalidate();
                }
            });
            String message = "";
            if (!scenario_file.exists()) {
                message += " (error: does not exist)";
            } else if (scenario_file.isDirectory()) {
                message += " (error: is directory)";
            } else if (!scenario_file.canRead()) {
                message += " (error: unreadable)";
            }
            scenarioFileLabel.setText("Scenario  : " +
                    scenario_file.getName() +
                    message);
            scenarioFileButton.setToolTipText(scenario_file.getPath());
        } else {
            scenarioFileLabel.setText("Scenario  : not given");
            scenarioFileButton.setToolTipText(null);
        }
        repaint();
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        ListSelectionModel lsm = generationTable.getSelectionModel();
        for (int r = 0; r < generationTable.getRowCount(); ++r) {
            if (lsm.isSelectedIndex(r)) {
                generationDataModel.getStartObject(r).selected = true;
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
