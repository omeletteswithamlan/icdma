package mtu.construction.gui.old;

import mtu.construction.gui.wrapper.G_Variable;

import mtu.construction.icdma.Simulator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.net.URL;
import java.util.Calendar;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class CenterInfoPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	
	private Simulator sim;
	
	private JLabel msg1, msg2, msg3;
	
	private PicturePanel weather;
	private PicturePanel worker;
	private JButton simStep;
	
	public CenterInfoPanel(MainWindow win, Simulator s)
	{
		sim = s;
		setLayout(new BorderLayout());
		
		JPanel iconpanel = new JPanel(new GridLayout(1, 2));
		URL[] pictures = new URL[]{this.getClass().getResource("/image/sun.png"), this.getClass().getResource("/image/rain.png"), this.getClass().getResource("/image/snow.png")};
		//String[] pictures = new String[]{"/image/sun.png", "image/rain.png", "image/snow.png"};
		weather = new PicturePanel(pictures);
		weather.setBackground(Color.blue);
		iconpanel.add(weather);

		URL[] pictures2 = new URL[]{this.getClass().getResource("/image/work.png"), this.getClass().getResource("/image/no-work.png")};
		//String[] pictures2 = new String[]{"image/work.png", "image/no-work.png"};
		worker = new PicturePanel(pictures2);
		worker.setBackground(Color.blue);
		iconpanel.add(worker);
		
		add(iconpanel, BorderLayout.WEST);
		
		simStep = new JButton("Sim Step");
		simStep.addActionListener(win);
		add(simStep, BorderLayout.EAST);
		
		setOpaque(false);
		
		JPanel centerinfopanel = new JPanel();
		centerinfopanel.setLayout(new GridLayout(1, 3));
		msg1 = new JLabel("Test 1");
		msg2 = new JLabel("Test 2");
		msg3 = new JLabel("Test 3");
		msg1.setOpaque(true);
		msg2.setOpaque(true);
		msg3.setOpaque(true);
		msg1.setHorizontalAlignment(JLabel.CENTER);
		msg2.setHorizontalAlignment(JLabel.CENTER);
		msg3.setHorizontalAlignment(JLabel.CENTER);
		centerinfopanel.add(new PaddedPanel(10, msg1));
		centerinfopanel.add(new PaddedPanel(10, msg2));
		centerinfopanel.add(new PaddedPanel(10, msg3));
		
		centerinfopanel.setOpaque(false);
		
		add(centerinfopanel, BorderLayout.CENTER);
		
		update();
	}
	
	/**
	 * Enable and disable the "Sim Step" button
	 * @param enabled - true to enable, false to disable
	 */
	public void enableSimButton(boolean enabled){
		simStep.setEnabled(enabled);
	}
	
	//Change picture based on the weather variable
	//Change picture based on labor strike variable
	//--Maybe make a variableListener interface...?
	public void update()
	{
		G_Variable var;
		
		//Weather Picture
		var = sim.getGlobalVariable("Weather");
		if(var.getStringState().equals("Rainy"))
			weather.setPicture(1);
		else if(var.getStringState().equals("Snowy"))
			weather.setPicture(2);
		else
			weather.setPicture(0);
		
		//Work Picture
		var = sim.getGlobalVariable("Global Labor Strike");
		if(var.getStringState().equals("True"))
			worker.setPicture(1);
		else
			worker.setPicture(0);
		
		//Date, Current Step, Sim Button
		msg1.setText("Week: " + sim.getCurrentTimeStep());
		msg2.setText("Remaining: " + (sim.getLastTimeStep()-sim.getCurrentTimeStep()));
		msg3.setText(""+(sim.getCalendar().get(Calendar.MONTH)+1)+"/"+sim.getCalendar().get(Calendar.DATE)+"/"+sim.getCalendar().get(Calendar.YEAR));
	}
}
