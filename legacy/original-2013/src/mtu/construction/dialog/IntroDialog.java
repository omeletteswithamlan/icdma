package mtu.construction.dialog;

import mtu.construction.gui.InitListener;
import mtu.construction.gui.old.ConfigPanel;
import mtu.construction.gui.old.MainWindow;

import mtu.construction.icdma.Simulator;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class IntroDialog extends JDialog implements ActionListener
{
	JButton button;
	ConfigPanel config;
	InitListener init;
	
	private IntroDialog()
	{
		setTitle("Welcome to iCDMA");
		setLayout(new BorderLayout());
		setModal(true);
		
		JLabel textlabel = new JLabel("<html><center>iCDMA<br>By: Matt Watkins, Amlan Mukherjee, <br> Kekoa Kaaikala, Corey Tebo, Jessica Anderson <br>Copyright Michigan Technological University<center></html>");
		textlabel.setHorizontalAlignment(SwingConstants.CENTER);
		add(textlabel, BorderLayout.CENTER);
		
		button = new JButton("Begin Simulation");
		button.addActionListener(this);
		
		JButton configure=new JButton("Configure Simulation");
		configure.addActionListener(this);
		
		JPanel buttons= new JPanel(new BorderLayout());
		buttons.add(button,BorderLayout.CENTER);
//		buttons.add(configure,BorderLayout.SOUTH);
		add(buttons, BorderLayout.SOUTH);
		
		config = new ConfigPanel();
		add(config);
		
		setBounds(500, 500, 300, 200);
	}
	
	public IntroDialog(InitListener l)
	{
		this();
		init = l;
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource()!=button)
		{
			System.exit(0);
			//return ;//(new ConfigureDialog.showdialog());
		}
		else
		{
			config.setVariables();
			if(init != null) init.onInit(config.getHost(), config.getPort(), config.getDatabase(),config.getProject(), config.getUsername(), config.getPassword());
			//config.setVariables();
			//Simulator sim = new Simulator(config.getHost(), config.getPort(), config.getDatabase(), config.getUsername(), config.getPassword());
			//MainWindow win = new MainWindow(sim);
			//win.setVisible(true);
			setVisible(false);
			dispose();
			return;
		}
	}
	
	public static void showdialog()
	{
		(new IntroDialog()).setVisible(true);
	}
}
