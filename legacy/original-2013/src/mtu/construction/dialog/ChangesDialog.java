package mtu.construction.dialog;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


public class ChangesDialog extends JDialog implements ActionListener
{
	private String question;
	private JPanel infopanel;
	private JScrollPane scrollpane;
	private JTextArea textarea;
	
	public ChangesDialog(String q)
	{
		System.out.println("Dialogue opened");
		setTitle("Confirm Changes");
		
		question = q;
		
		textarea = new JTextArea(5,30);
		infopanel = new JPanel();
		infopanel.setLayout(null);
		
		scrollpane = new JScrollPane(textarea);
		
		scrollpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		add(scrollpane, BorderLayout.CENTER);
		
		JButton okbutton = new JButton("OK");
		okbutton.addActionListener(this);
		add(okbutton, BorderLayout.SOUTH);
		
		setBounds(100, 100, 500, 500);
		setModal(true);
		JLabel theQuestion = new JLabel(question);

		add(theQuestion, BorderLayout.NORTH);
		setVisible(true);
	}
	
	
	public void actionPerformed(ActionEvent e)
	{
		if(!textarea.getText().equals(""))
		{
			setVisible(false);
		}
	}
	
}
