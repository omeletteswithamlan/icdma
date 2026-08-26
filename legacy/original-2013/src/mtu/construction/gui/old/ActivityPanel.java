package mtu.construction.gui.old;

import mtu.construction.gui.wrapper.G_Activity;
import mtu.construction.icdma.Simulator;

import java.awt.Color;
import java.awt.GridLayout;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class ActivityPanel extends JPanel
{
	protected Simulator sim;

	protected JTabbedPane activityTabs;
	
	protected Vector<InfoPanel> ipanels;
	
	public ActivityPanel(Simulator sim)
	{
		this.sim = sim;

		update();
	}
	
	public void update()
	{
		String[] pictures = new String[]{
				"/image/activity1.jpg",
				"/image/activity2.jpg",
				"/image/activity3.jpg",
				"/image/activity4.jpg",
				"/image/activity5.jpg",
				"/image/activity6.jpg",
				"/image/activity7.jpg",
				"/image/activity8.jpg",
				"/image/activity9.jpg",
				"/image/activity10.jpg",
				"/image/activity11.jpg",
				"/image/activity12.jpg",
				"/image/activity13.jpg",
				"/image/activity14.jpg"};
		
		removeAll();
		
		setLayout(new GridLayout(1, 1));
		activityTabs = new JTabbedPane();
		activityTabs.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
		activityTabs.setTabPlacement(JTabbedPane.LEFT);
		add(activityTabs);

		ipanels = new Vector<InfoPanel>();
		int x=0;
		int day= sim.getCurrentTimeStep();
		for(G_Activity node : sim.getSortedActivityList())
		{
			InfoPanel i = new InfoPanel(node, "Progress", "Time", "Completeness", "Total Cost", "Time", "Cost", pictures[Math.min(node.getID(), pictures.length - 1)], sim);
			ipanels.add(i);
			activityTabs.add(node.getLabel(), i);
			//set the color of the tabs to reflect the status of compleation
			activityTabs.setBackgroundAt(x++,node.isActive()?Color.GREEN:(day<node.getEarlyStart()?Color.LIGHT_GRAY:Color.GRAY));
//			if(!node.isActive()) System.out.println(node.getLabel()+" Start: "+node.getStart()+" Day: "+day);
//			System.out.println("Activity: "+node.getLabel()+" active?: "+node.isActive()+" Time: "+day+" Start: "+node.getStart());
		}
		
		for(InfoPanel p : ipanels)
			p.update();
	}
}
