package mtu.construction.gui.old;

import java.awt.GridLayout;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.swing.JPanel;

import ptolemy.plot.Plot;

public class GraphPanel extends JPanel
{
	protected Plot plot;
	protected String title;
	protected String xaxis;
	protected String yaxis;
	protected boolean plotzero;
	
	public GraphPanel(String title, String xaxis, String yaxis)
	{
		this(title, xaxis, yaxis, false);
	}
	
	public GraphPanel(String title, String xaxis, String yaxis, boolean plotzero)
	{
		this.title = title;
		this.xaxis = xaxis;
		this.yaxis = yaxis;
		this.plotzero = plotzero;
		
		plot = new Plot();
		plot.setMarksStyle("dots");
		plot.setImpulses(false);
		plot.addLegend(1,"As-Built");
		plot.addLegend(2,"As-Planned");
		plot.addLegend(4,"As-Projected");
		plot.setConnected(false,0);
		setLayout(new GridLayout(1, 1));
		add(plot);
		
		plot.repaint();
	}
	
	public void update(int currentday, TreeMap<Integer, Double> apcost, TreeMap<Integer, Double> abcost)
	{
		plot.clear(false);
		plot.setMarksStyle("none");
		plot.setImpulses(false);
		plot.setConnected(false,0);
		plot.clearLegends();
		plot.addLegend(1,"As-Built");
		plot.addLegend(2,"As-Planned");
		plot.addLegend(4,"As-Projected");

		plot.setTitle(title);
		plot.setXLabel(xaxis);
		plot.setYLabel(yaxis);
		
		Iterator<Entry<Integer, Double>> iter = apcost.entrySet().iterator();
		while(iter.hasNext())
		{
			Entry<Integer, Double> e = iter.next();
			if(e.getValue() != 0 || plotzero)
			{
				plot.addPoint(2, e.getKey(), e.getValue(), (e.getKey() != 1));
			}
		}
		
		iter = abcost.entrySet().iterator();
		while(iter.hasNext())
		{
			Entry<Integer, Double> e = iter.next();
			if(e.getKey() < currentday)
				plot.addPoint(1, e.getKey(), e.getValue(), (e.getKey() != 1));
			else if(e.getKey() == currentday)
			{
				//plot both on the current day.
				plot.addPoint(1, e.getKey(), e.getValue(), (e.getKey() != 1));
				plot.addPoint(4, e.getKey(), e.getValue(), (e.getKey() != 1));
			}
			else
				plot.addPoint(4, e.getKey(), e.getValue(), (e.getKey() != 1));
		}
		//System.out.println("Repainting "+plot.getTitle());
		//System.out.println(plot.getLegend(1));
		plot.repaint();
	}
}
