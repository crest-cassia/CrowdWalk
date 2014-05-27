package nodagumi.ananPJ.misc;

import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

public class GetDoublesDialog extends JDialog 
implements ActionListener, Serializable {
	private JSpinner[] values;
	public boolean proceed = false;
	public GetDoublesDialog(String title,
			String[] labels,
			double[][] range) {
		this.setModal(true);
		Container panel = getContentPane();
		panel.setLayout(new GridLayout(3, labels.length));
		
		values = new JSpinner[labels.length];

		for (int i = 0; i < labels.length; ++i) {
			panel.add(new JLabel(labels[i]));
		}

		for (int i = 0; i < labels.length; ++i) {
			values[i] = new JSpinner(new SpinnerNumberModel(range[i][2],
					range[i][0], range[i][1], 0.1));
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
    	final double[][] range = {{-1000, 1000, -1000},
    			{-1000, 1000, -1000},
    			{-1000, 1000, 1000}}; 

		return showDialog(title, labels, range);
	}
	public static double[] showDialog(String title,
			String[] labels,
			double[][] range) {
		GetDoublesDialog gdd = new GetDoublesDialog("", labels, range);
    	gdd.setVisible(true);

    	if (!gdd.proceed) return null;

    	double[] ret = new double[labels.length];
    	for (int i = 0; i < labels.length; ++i) {
    		ret[i] = (Double)(gdd.values[i].getValue());
    	}
    	return ret;
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
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
