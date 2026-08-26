package mtu.construction.gui.old;

import mtu.construction.icdma.Simulator;

import java.awt.GridLayout;
import java.util.Map.Entry;

import javax.swing.JPanel;

import ptolemy.plot.Plot;

public class RLCCPanel extends JPanel
{
	protected Plot plot;
	protected String title;
	protected String xaxis;
	protected String yaxis;
	private Simulator sim;
	private final int AS_BUILT_PLOT = 1;
	private final int AS_PROJECTED_PLOT = 4;
	private final int PLOT_LINES = 3;

	public RLCCPanel(String title, String xaxis, String yaxis, Simulator sim)
	{
		this.title = title;
		this.xaxis = xaxis;
		this.yaxis = yaxis;
		this.sim = sim;
		
		plot = new Plot();
		plot.setMarksStyle("dots");
		plot.setImpulses(false);
		plot.addLegend(AS_BUILT_PLOT,"As-Built");
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

		int max = 3000;
		for(Entry<Integer, Double> e : sim.getResourceLoadedCommodityCurve().entrySet())
		{
			if(e.getValue() > max)
				max = e.getValue().intValue();
			if(e.getKey() < currentday)
				plot.addPoint(AS_BUILT_PLOT, e.getKey(), e.getValue(), (e.getKey() != 1));
			else if(e.getKey() == currentday)
			{
				//plot both on the current day.
				plot.addPoint(AS_BUILT_PLOT, e.getKey(), e.getValue(), (e.getKey() != 1));
				plot.addPoint(AS_PROJECTED_PLOT, e.getKey(), e.getValue(), (e.getKey() != 1));
			}
			else
				plot.addPoint(AS_PROJECTED_PLOT, e.getKey(), e.getValue(), (e.getKey() != 1));
		}
		
		//Draw the black lines
		plot.addPoint(PLOT_LINES, 1, sim.getSpace(), false);
		plot.addPoint(PLOT_LINES, sim.getLastTimeStep(), sim.getSpace(), true);
		plot.addPoint(PLOT_LINES, sim.getCurrentTimeStep(), 0, false);
		plot.addPoint(PLOT_LINES, sim.getCurrentTimeStep(), max, true);
		
		plot.addLegend(AS_BUILT_PLOT,"As-Built");
		//System.out.println("Added Legend "+plot.getLegend(AS_BUILT_PLOT));
		plot.addLegend(AS_PROJECTED_PLOT,"As-Projected");
		//System.out.println("Added Legend "+plot.getLegend(AS_PROJECTED_PLOT));
		
		plot.setTitle(title);
		plot.setXLabel(xaxis);
		plot.setYLabel(yaxis);
		
		plot.repaint();
	}
}
