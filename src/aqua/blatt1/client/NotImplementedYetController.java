package aqua.blatt1.client;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

public class NotImplementedYetController implements ActionListener {
	private final Component parent;

	public NotImplementedYetController(Component parent) {
		this.parent = parent;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JOptionPane.showMessageDialog(parent, "Functionality not implemented yet.");
	}
}
