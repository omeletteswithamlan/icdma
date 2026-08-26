package mtu.construction.gui.old;

import mtu.construction.gui.wrapper.G_LaborCrew;
import mtu.construction.icdma.Simulator;

import java.awt.GridLayout;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.swing.JPanel;

import ptolemy.plot.Plot;

public class RLLCPanel extends JPanel
{
	protected Plot plot;
	protected String title;
	protected String xaxis;
	protected String yaxis;
	protected Simulator sim;

	public RLLCPanel(String title, String xaxis, String yaxis, Simulator sim)
	{
		this.sim = sim;
		this.title = title;
		this.xaxis = xaxis;
		this.yaxis = yaxis;
		
		plot = new Plot();
		plot.setMarksStyle("dots");
		plot.setImpulses(false);
		plot.addLegend(1,"As-Built");
		plot.setConnected(false,0);
		setLayout(new GridLayout(1, 1));
		add(plot);
	}
	
	public void update(int currentday)
	{
		plot.clear(false);
		plot.setMarksStyle("none");
		plot.setImpulses(false);
		plot.setConnected(false,0);
		plot.clearLegends();
		
		double max = 0;
		for(Entry<G_LaborCrew, TreeMap<Integer, Integer>> en : sim.getResourceLoadedLaborCurve().entrySet())
		{
			G_LaborCrew crew = en.getKey();
			TreeMap<Integer, Integer> curve = en.getValue();
			for(Entry<Integer, Integer> e : curve.entrySet())
			{
				plot.addPoint(crew.getID(), e.getKey(), e.getValue(), true);
				if(e.getValue() > max)
					max = e.getValue();
			}
			//plot.addLegend(crew.getID(), crew.getLabel());
		}

		//plot.addPoint(3, sim.getCurrentTimeStep(), 0, false);
		//plot.addPoint(3, sim.getCurrentTimeStep(), max, true);
		
//		System.out.println("Adding Legends...");
		for(G_LaborCrew c : sim.getLaborCrews()){
			plot.addLegend(c.getID(), c.getLabel());
//			System.out.println(plot.getLegend(c.getID()));
		}
//		System.out.println(plot.getNumDataSets()+" plots.");
//		for(G_LaborCrew c : sim.getLaborCrews()){
//			System.out.println("Legend "+plot.getLegend(c.getID()));
//		}
		
		plot.setTitle(title);
		plot.setXLabel(xaxis);
		plot.setYLabel(yaxis);
		
		plot.repaint();
	}
}
