package mtu.construction.gui.old;

import mtu.construction.icdma.Simulator;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

//TODO: Update MainWindow
public class SchedulePanel extends JPanel
{
	
	private GanttChartPanel ganttchart,
							ganttchartg;
	private RLCCPanel rlccpanel;
	private RLLCPanel rllcpanel;
	private JTabbedPane ganttTabs;
	private Simulator sim;

	public void lock()
	{
		ganttchartg.lockUpdate();
		ganttchart.lockUpdate();
	}
	
	public void unlock()
	{
		ganttchartg.unlockUpdate();
		ganttchart.unlockUpdate();
	}
	
	public SchedulePanel(Simulator sim)
	{
		this.sim = sim;
		setLayout(new BorderLayout());
		
		JPanel mainpanel2 = new JPanel();
		mainpanel2.setLayout(new BorderLayout());
		
		JPanel panel = new JPanel();
		panel.setLayout( new BorderLayout());
		panel.add( new JLabel("Schedule"), BorderLayout.NORTH);
		
		ganttTabs = new JTabbedPane();
		ganttTabs.setTabPlacement(JTabbedPane.LEFT);
		ganttTabs.add(ganttchart = new GanttChartPanel(sim, false));
		ganttTabs.setTitleAt(0,"Sequence Grouping");
		ganttTabs.add(ganttchartg = new GanttChartPanel(sim, true));
		ganttTabs.setTitleAt(1,"Crew Grouping");
		ganttTabs.setPreferredSize(new Dimension(200, 300));
		panel.add(ganttTabs, BorderLayout.CENTER);
		mainpanel2.add(panel, BorderLayout.CENTER);
		
		add(mainpanel2, BorderLayout.NORTH);
		
		JPanel mainpanel = new JPanel();
		mainpanel.setLayout(new GridLayout(2, 1));
		
		rlccpanel = new RLCCPanel("Resource Loaded Commodity Curve", "Time (Weeks)", "Space Used (sq. yards)", sim);
		mainpanel.add(rlccpanel);
		rllcpanel = new RLLCPanel("Resource Loaded Labor Curve", "Time (Weeks)", "Activities (#)", sim);
		mainpanel.add(rllcpanel);

		add(mainpanel, BorderLayout.CENTER);
	}
	
	public void update()
	{
		updatePlots();
	}
	
	public boolean anyvaluechanged()
	{	
		return false;
	}
	
	public void drawCharts(boolean b){
		ganttchart.setDraw(b);
		ganttchartg.setDraw(b);
	}
	
	private void updatePlots()
	{
		ganttchart.update();
		ganttchartg.update();
		rlccpanel.update(sim.getCurrentTimeStep());
		rllcpanel.update(sim.getCurrentTimeStep());

//		if(Simulator.stats != null)
//			Simulator.stats.repaint();
		//this.getParent().repaint();
	}
}
