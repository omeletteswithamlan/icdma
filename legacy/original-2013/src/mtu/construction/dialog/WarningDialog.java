package mtu.construction.dialog;

import mtu.construction.gui.old.ModalThread;
import mtu.construction.gui.old.PicturePanel;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.awt.Dialog;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class WarningDialog extends Dialog implements ActionListener
{
	private ModalThread thread; //Needed to enable tab switching for the MainWindow
	private boolean modal;
	private JButton okbutton;
	
	public WarningDialog(Window parent, String title, String message, String image, boolean modal, boolean ok_button)
	{
		//super();
		super(parent);
		this.modal=modal;
		setTitle(title);
		
		setLayout(new BorderLayout());
		
		//format the message properly
		message = message.replace("\n", "<br>");
		message = "<html>" + message + "</html>";
		
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(2, 1));
		add(panel, BorderLayout.CENTER);
		
		JLabel label = new JLabel(message);
		panel.add(label);
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setVerticalAlignment(JLabel.CENTER);

		//panel.add(new PicturePanel(new String[]{image}));
		if (image!=null)
			panel.add(new PicturePanel(new URL[]{this.getClass().getResource(image)}));
		
		if(ok_button){
			okbutton = new JButton("OK");
			okbutton.addActionListener(this);
			add(okbutton, BorderLayout.SOUTH);
		}
		
		setBounds(100, 100, 500, 500);
		//if(modal){ setModal(true); modal = false; this.modal = false;} //don't use thread
		setModal(false);//use thread
		setVisible(true);

		if (modal)
		{
			thread = new ModalThread();
			thread.start();
		}
	}
	
	public void display(){
		System.out.println("Display");
	}
	
	public static void show(String title, String message, String image, boolean modal, Window parent)
	{
		new WarningDialog(parent, title, message, image, modal, true);
	}
	
	/*
	public void killbutton()
	{
		this.remove(okbutton);
	}*/
	
	public void exit()
	{
		if (modal)
			thread.die();
		this.setVisible(false);
	}
	
	public void actionPerformed(ActionEvent arg0)
	{
		setVisible(false);
		if (modal)
			thread.die();
	}
}

