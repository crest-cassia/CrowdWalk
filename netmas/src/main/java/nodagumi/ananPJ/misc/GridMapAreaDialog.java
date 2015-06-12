// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.misc;

import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/* tkokada
 * This class create a dialog to ask how many grids create with selected area.
 * The dialog is quite similar with GetDoublesDialog class, but it includes
 * a division method which separate a selected rectangle area with vertical 
 * division and horizontal division number. Then the rectangle is divided with
 * division number.
 */

public class GridMapAreaDialog extends JDialog 
    implements ActionListener {
	
	private JSpinner[] values;
	
	public boolean proceed = false;
	
	public GridMapAreaDialog(String title,
			String[] labels,
			double min_height[],
			double max_height[],
			int vertical_division[],
			int horizontal_division[],
			double rotation[]) {
		this.setModal(true);
		Container panel = getContentPane();
		panel.setLayout(new GridLayout(3, labels.length));
		
		values = new JSpinner[labels.length];

		for (int i = 0; i < labels.length; ++i) {
			panel.add(new JLabel(labels[i]));
		}

		values[0] = new JSpinner(new SpinnerNumberModel(min_height[2],
				min_height[0], min_height[1], 0.1));
		values[1] = new JSpinner(new SpinnerNumberModel(max_height[2],
				max_height[0], max_height[1], 0.1));
		values[2] = new JSpinner(new SpinnerNumberModel(vertical_division[2],
				vertical_division[0], vertical_division[1], 1));
		values[3] = new JSpinner(new SpinnerNumberModel(horizontal_division[2],
				horizontal_division[0], horizontal_division[1], 1));
		values[4] = new JSpinner(new SpinnerNumberModel(rotation[2],
				rotation[0], rotation[1], 1.0));
		/*values[4].addChangeListener(new ChangeListner() {
			public void stateChanged(ChangeEvent ce) {
				panel.
			}
		});*/
		
		for (int i = 0; i < labels.length; ++i) {
			panel.add(values[i]);
		}
		
		for (int i = 0; i < labels.length - 2; ++i) {
			panel.add(new JLabel());
		}

		JButton ok = new JButton("OK");
		ok.addActionListener(this);
		panel.add(ok);
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(this);
		panel.add(cancel);
		
		this.pack();
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() == "OK") {
			proceed = true;
			dispose();
		} else if (e.getActionCommand() == "Cancel") {
			proceed = false;
			dispose();
		}
	}
	
	public static double[] showDialog(String title, String[] labels) {
    	final double[] min_height = {-1000, 1000, -1000};
    	final double[] max_height = {-1000, 1000, -1000};
    	final int[] vertical_division = {0, 1000, 0};
    	final int[] horizontal_division = {0, 1000, 0};
    	final double[] rotation = {0, 360, 0};

		return showDialog(title, labels, min_height, max_height,
				vertical_division, horizontal_division, rotation);
	}
	
	public static double[] showDialog(String title,
			String[] labels,
			double[] min_height, 
			double[] max_height,
			int[] vertical_division,
			int[] horizontal_division,
			double[] rotation) {
		GridMapAreaDialog gdd = new GridMapAreaDialog("", labels,
				min_height, max_height, vertical_division, horizontal_division,
				rotation);
    	gdd.setVisible(true);

    	if (!gdd.proceed) return null;

    	double[] ret = new double[labels.length];
    	ret[0] = (Double)(gdd.values[0].getValue());
    	ret[1] = (Double)(gdd.values[1].getValue());
    	ret[2] = (Integer) (gdd.values[2].getValue());
    	ret[3] = (Integer) (gdd.values[3].getValue());
    	ret[4] = (Double) (gdd.values[4].getValue());
    	return ret;
	}
}
