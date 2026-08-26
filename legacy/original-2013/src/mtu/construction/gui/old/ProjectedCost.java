package mtu.construction.gui.old;

import java.awt.GridLayout;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.swing.JPanel;

import ptolemy.plot.Plot;
import mtu.construction.tonae.QueryResult;
import mtu.construction.tonae.QueryResult2;

public class ProjectedCost extends JPanel
{
	private static final long serialVersionUID = 1L;
	protected Plot plot;
	protected String title;
	protected String xaxis;
	protected String yaxis;
	
	public ProjectedCost(String title, String xaxis, String yaxis)
	{
		this.title = title;
		this.xaxis = xaxis;
		this.yaxis = yaxis;
		
		plot = new Plot();
		plot.setMarksStyle("dots");
		plot.setImpulses(false);
		plot.setConnected(false,0);
		setLayout(new GridLayout(1, 1));
		add(plot);
	}
	
	public void update(int currentday, TreeMap<Integer, Double> apcost, TreeMap<Integer, Double> abcost, QueryResult2 results)
	{
		plot.clear(false);
		plot.setMarksStyle("none");
		plot.setImpulses(false);
		plot.setConnected(false,0);
		plot.clearLegends();
		plot.addLegend(1,"As-Built");
		plot.addLegend(2,"As-Planned");
		plot.addLegend(4,"As-Projected");
		plot.addLegend(5,"Best Case Scenario");
		plot.addLegend(6,"Worst Case Scenario");
		plot.setTitle(title);
		plot.setXLabel(xaxis);
		plot.setYLabel(yaxis);
		plot.invalidate();
		
		Iterator<Entry<Integer, Double>> iter = apcost.entrySet().iterator();
		while(iter.hasNext()) //As-Planned curve
		{
			Entry<Integer, Double> e = iter.next();
			if(e.getValue() != 0)//|| plotzero)
			{
				plot.addPoint(2, e.getKey(), e.getValue(), (e.getKey() != 1));
			}
		}
		
		for(Entry<Integer, Double> e : abcost.entrySet()) //As-Built curve
		{
			if(e.getKey() < currentday - 1)
				plot.addPoint(1, e.getKey(), e.getValue(), (e.getKey() != 1));
			else if(e.getKey() == currentday - 1)
			{
				//plot both on the current day.
				plot.addPoint(1, e.getKey(), e.getValue(), (e.getKey() != 1));
				plot.addPoint(4, e.getKey(), e.getValue(), (e.getKey() != 1));
			}
			else
				plot.addPoint(4, e.getKey(), e.getValue(), (e.getKey() != 1));
		}
		//System.out.println("Best Case:");
		for(Entry<Integer, Double> e : results.getBestCase().entrySet()) //best case
			{plot.addPoint(5, e.getKey(), e.getValue(), true); }//System.out.println(e.getKey()+","+e.getValue());}
		//System.out.println("Worst Case:");
		for(Entry<Integer, Double> e : results.getWorstCase().entrySet()) //worst case
			{plot.addPoint(6, e.getKey(), e.getValue(), true); }//System.out.println(e.getKey()+","+e.getValue());}
	}
}
