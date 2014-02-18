package nodagumi.ananPJ.Simulator;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import nodagumi.ananPJ.Settings;
import nodagumi.ananPJ.misc.FilePathManipulation;

public class DumpState implements Serializable {
	protected JPanel dump_panel;
	protected JCheckBox enable_dump;
	protected JLabel dump_filename;
	protected PrintWriter dump_writer;
	
	protected EvacuationModelBase model;
	protected Settings settings;
	
	public DumpState(EvacuationModelBase _model) {
		model = _model;
		settings = Settings.getSettings();

		setup_dump_panel();
	}

	public void setDumpWriter(PrintWriter pw) {
		dump_writer = pw;
	}

	public JPanel getDumpPanel() {
		return dump_panel;
	}

	private void setup_dump_panel() {
		dump_panel = new JPanel(new GridBagLayout());
		dump_panel.setName("Dump");
		
		GridBagConstraints c = null;
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		dump_filename = new JLabel(settings.get("dump-filename", "netmas.dump"));
		dump_filename.setBorder(BorderFactory.createLineBorder(Color.black));
		dump_filename.setPreferredSize(new Dimension(200, 20));
		dump_panel.add(dump_filename, c);

		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		JButton dump_setfile = new JButton("Set");
		dump_setfile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFrame frame = new JFrame("temp");
				FileDialog fd = new FileDialog(frame , "Dumpfile", FileDialog.SAVE);				
		    	fd.setVisible (true);
		        
		    	if (fd.getFile() == null) return;
		    	String filename = fd.getDirectory() + fd.getFile();
				String dir_name = settings.get("inputdir", "");

				String rel_filename = FilePathManipulation.getRelativePath(dir_name, filename);
		    	dump_filename.setText(rel_filename);
			}
		});
		dump_panel.add(dump_setfile, c);

		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 2;
		enable_dump = new JCheckBox("Dump simulation");
		enable_dump.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				update_dump_writer();
			}
		});
		dump_panel.add(enable_dump, c);
	}

	public void preUpdate() {
	}
	
	public void postUpdate() {
		if (dump_writer != null) {
			dump_writer.println("time," + model.getTickCount());
			model.getAgentHandler().dumpStateDiff(dump_writer);
		}
	}
	protected void update_dump_writer() {
		if (enable_dump.isSelected()) {
			if (dump_writer != null) {
				System.err.println("dump writer already enabled");
				return;
			}
	    	try {
	    		FileWriter fw = new FileWriter(dump_filename.getText());
	    		dump_writer = new PrintWriter(fw);
	    		dump_writer.println(model.getMap().getFileName());
				dump_writer.println("initialtime," + model.getTickCount());
				model.getAgentHandler().dumpState(dump_writer);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, e.toString(), "Could not open dumpfile",
						JOptionPane.ERROR_MESSAGE);
				enable_dump.setSelected(false);
				dump_writer = null;
			}
		} else {
			if (dump_writer == null) {
				System.err.println("dump writer already disabled");
				return;
			}
			dump_writer.flush();
			dump_writer.close();
			dump_writer = null;
		}
	}
}
