package mtu.construction.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

public class ConfigureDialog extends JDialog implements ActionListener{
	JButton button;
	public ConfigureDialog()
	{
		setTitle("Simulator Settings");
		setLayout(new BorderLayout());
		setModal(true);
		
		JLabel textlabel = new JLabel("Configure Setings For this Session");
		textlabel.setHorizontalAlignment(SwingConstants.CENTER);
		add(textlabel, BorderLayout.NORTH);
		
		button = new JButton("Close");
		button.addActionListener(this);
		
		JToggleButton dbWrite = new JToggleButton("Write To The Data Base");
		JToggleButton fileWrite = new JToggleButton("Write a transcript file");
		JToggleButton consoleWrite = new JToggleButton("Write transcript to console");
		
		JPanel boxes= new JPanel(new FlowLayout());
		boxes.add(dbWrite);
		boxes.add(fileWrite);
		boxes.add(consoleWrite);
		//boxes.add(new JLabel("Feature Coming Soon"),BorderLayout.NORTH);
		add(boxes, BorderLayout.CENTER);
		add(button, BorderLayout.SOUTH);
	
		
		setBounds(500, 500, 300, 200);
		return;
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource()==button);
		{
			setVisible(false);
			return;
		}

	}
	
	public static void showdialog()
	{
		(new ConfigureDialog()).setVisible(true);
	}
}
