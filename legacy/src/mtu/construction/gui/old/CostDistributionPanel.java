package mtu.construction.gui.old;

import mtu.construction.icdma.Simulator;

import java.awt.GridLayout;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.swing.JPanel;

import mtu.construction.project.TONAE;
import ptolemy.plot.Plot;
import mtu.construction.tonae.QueryResult;
import mtu.construction.tonae.QueryResult2;

public class CostDistributionPanel extends JPanel
{
	protected Plot plot;
	protected String title;
	protected String xaxis;
	protected String yaxis;

	public CostDistributionPanel(String title, String xaxis, String yaxis)
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

	public void update(QueryResult2 results)
	{
		plot.clear(false);
		plot.setMarksStyle("none");
		plot.setImpulses(false);
		plot.setConnected(false,0);
		plot.clearLegends();
		plot.addLegend(4,"As-Projected");
		plot.setTitle(title);
		plot.setXLabel(xaxis);
		plot.setYLabel(yaxis);

		TreeMap<Double, Integer> data = results.getDistribution(Simulator.QUERY_FUTURES_QUANTINIZATION);
		double delta=Simulator.QUERY_FUTURES_QUANTINIZATION;//results.getBucketSize();
		double last	=-1;
		//System.out.println("e: " + data.entrySet().size());
		for(Entry<Double, Integer> e : data.entrySet())
		{
			plot.addPoint(4, e.getKey(), (double)e.getValue()/Simulator.numFutures*100, true);
			//if (TONAE.paperGant&&!TONAE.querymode)
			//{
				if (last<0)
				{
					last=e.getKey();
					if(TONAE.debugText) System.out.println("cost: "+e.getKey()+" probability in percent: "+e.getValue()+"/"+Simulator.numFutures+" * 100 = "+(double)e.getValue()/Simulator.numFutures*100);
					continue;
				}
				for(int i=1;i<Math.round((e.getKey()-last)/delta);i++)
				{
					last=last+delta;
					if(TONAE.debugText) System.out.println("cost: "+last+" probability in percent: "+0);
				}
				if(TONAE.debugText) System.out.println("cost: "+e.getKey()+" probability in percent: "+e.getValue()+"/"+Simulator.numFutures+" * 100 = "+(double)e.getValue()/Simulator.numFutures*100);
				last=e.getKey();
			//}
		}
	}
}
