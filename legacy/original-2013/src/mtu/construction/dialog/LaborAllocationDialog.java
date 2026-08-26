package mtu.construction.dialog;

import mtu.construction.gui.old.LaborCrewPanel;
import mtu.construction.gui.old.MainWindow;
import mtu.construction.gui.old.ModalThread;

import mtu.construction.icdma.Simulator;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;

import mtu.construction.project.TONAE;

import mtu.construction.project.LaborCrew;

public class LaborAllocationDialog extends JDialog implements ActionListener
{
	private LaborCrewPanel p;
	private ModalThread thread;
	private long timeStart = 0;
	
	public LaborAllocationDialog(Simulator s, LaborCrew[] c, LaborCrew u, LaborCrew h)
	{
		setTitle("Labor Crew Changes");
		setLayout(new BorderLayout());
		p = new LaborCrewPanel(s);
		p.update();//must populate the lists befor displaying them // fixes null pointer that began when we added threding gui.
		p.setCrewList(c, u);
		
		add(p, BorderLayout.CENTER);
		JButton b = new JButton("Done");
		b.addActionListener(this);
		add(b, BorderLayout.SOUTH);
		setBounds(100, 100, 500, 500);
		
		//setModal(true);
		setModal(false);
		timeStart = System.currentTimeMillis();
		setVisible(true);
		
		thread = new ModalThread();
		thread.start();
	}
	
	public static void show(Simulator s, LaborCrew[] c, LaborCrew u, LaborCrew h)
	{
		new LaborAllocationDialog(s, c, u, h);
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if(TONAE.paperGant){
			System.out.println("Decision took "+ ((System.currentTimeMillis() - timeStart)/1000) + " seconds.");
			System.out.println("Time since \"Sim\" button pressed: "+ ((System.currentTimeMillis() - MainWindow.lastSimPressed())/1000) + " seconds.");
		}
		p.getCrews();
		p.getUnmapped();
		p.getHired();
		setVisible(false);
		thread.die();
	}
}
