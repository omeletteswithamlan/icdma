package mtu.construction.gui.old;

import mtu.construction.gui.wrapper.G_Activity;
import mtu.construction.gui.wrapper.G_LaborCrew;
import mtu.construction.gui.wrapper.GanttChartInfo;
import mtu.construction.icdma.Simulator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JPanel;

import mtu.construction.project.Activity;
import mtu.construction.project.LaborCrew;

/**
 * PlotPlan, PlotPlanCrew were plotting two of the same graph
 * @author kkaaikal
 *
 */
public class GanttChartPanel extends JPanel
{
	private static final long serialVersionUID = -7340201728741678981L;
	private int leftpad = 10;
	private int toppad = 0;
	private int rightpad = 30;
	private int bottompad = 20;
	private int textwidth;
	private int top = toppad;
	private int left = textwidth + leftpad;
	private int linewidth = 3;
	private int maxbottomtext;
	private int lastday;
	private int curday;
	
	private int right = getWidth() - rightpad;
	private int bottom = getHeight() - bottompad;
	
	//private TreeMap<G_Activity, Integer> abstart; //start for an activity
	//private TreeMap<G_Activity, Integer> abfinish; //end for an activity
	//private TreeMap<G_Activity, Boolean> critical;
	HashMap<G_Activity, GanttChartInfo> ganttinfo;
	private Vector<JLabel> sidelabels;
	private Vector<JLabel> bottomlabels;
	private Vector<G_Activity> activities;
	
	private LegendPanel legendpanel;
	
	private boolean updating, lockupdate;
	private boolean draw = false;
	protected boolean grouped;
	private Simulator sim;
	
	public GanttChartPanel(Simulator sim, boolean grouped)
	{
		super();
		
		this.sim = sim;
		this.grouped = grouped;
		
		legendpanel = new LegendPanel();
		
		//The legend
		//legendpanel.addEntry("As-Planned", Color.RED);
		legendpanel.addEntry("As-Projected", Color.ORANGE);
		legendpanel.addEntry("As-Built", Color.BLUE);
		legendpanel.addEntry("Baseline", Color.GREEN);
		
		
		setLayout(null);

		updating = false;
		lockupdate = false;
		update();
	}
	
	public void update()
	{
		if(updating || lockupdate)
			return;
		
		lastday = sim.getLastTimeStep() + 1;
		curday = sim.getCurrentTimeStep();

		updating = true;
		
		//abstart = new TreeMap<G_Activity, Integer>();
		//abfinish = new TreeMap<G_Activity, Integer>();
		sidelabels = new Vector<JLabel>();
		//critical = new TreeMap<G_Activity, Boolean>();
		
		//activities = sim.getActivityList();
		activities = sim.getSortedActivityList();
		
		ganttinfo = sim.getGanttChart();
		
		textwidth = 0;
		//Setup side labels
		for(G_Activity a : activities)
		{
			JLabel j = new JLabel(a.getLabel());
			j.setSize(j.getPreferredSize());
			if(j.getWidth() > textwidth)
				textwidth = j.getWidth();
			sidelabels.add(j);
		}
		
		GregorianCalendar g = (GregorianCalendar)sim.getCalendar();
		
		bottomlabels = new Vector<JLabel>();
		maxbottomtext = 0;
		//Setup bottom labels
		for(int x = 0; x < sim.getLastTimeStep(); x++)
		{
			DateFormat d = new SimpleDateFormat("MM-dd-yyyy");
			
			JLabel j = new JLabel(d.format(g.getTime()));
			j.setSize(j.getPreferredSize());
			
			if(j.getWidth() > maxbottomtext)
				maxbottomtext = j.getWidth();
			
			g.add(Calendar.HOUR, 24 * sim.getTimeFrame().getInterval());
			
			bottomlabels.add(j);
		}
		
		updating = false;
	}
	
	public void lockUpdate()
	{
		lockupdate = true;
	}
	
	public void unlockUpdate()
	{
		lockupdate = false;
		update();
	}
	
	protected void drawRect(int left, int top, int right, int bottom, Color c, Graphics g)
	{
		g.setColor(c);
		g.fillRect(left, top, right - left, bottom - top);

		g.setColor(Color.BLACK);
		g.drawRect(left, top, right - left, bottom - top);
	}
	
	private int getRealX(int v, int slices)
	{
		float perc = (float)v / (float)slices;
		
		return left + (int)(perc * (float)(right - left));
	}
	
	private int getRealY(int v, int slices)
	{
		float perc = (float)v / (float)slices;
		
		return top + (int)(perc * (float)(bottom - top));
	}
	
	private void plotPlan(Color c, Graphics g, int actnum, int lastday, int bp)
	{
		g.setColor(c);
		int fid = 1;
		//for(G_Activity a : sim.getActivities())
		for(G_Activity a : sim.getSortedActivityList())
		{
			int ybase = getRealY(fid, actnum);
			for(int x = 0; x < linewidth; x++)
			{
				int yv = ybase + bp - x;
				g.drawLine(getRealX(a.getStart(), lastday), yv, getRealX(a.getEnd(), lastday), yv);
			}
			fid++;
		}
	}
	
	private void plotPlanCrew(Color c, Graphics g, int actnum, int lastday, int bp)
	{
		g.setColor(c);
		int fid = 1;
		for(G_LaborCrew cr : sim.getLaborCrews())
		{//
			for(G_Activity a : sim.getSortedActivityList())//sim.getActivities())
			{
				if(a.getAsPlannedLaborUse().contains(cr))//
				{//
					int ybase = getRealY(fid, actnum);
					for(int x = 0; x < linewidth; x++)
					{
						int yv = ybase + bp - x;
						g.drawLine(getRealX(a.getStart(), lastday), yv, getRealX(a.getEnd(), lastday), yv);
					}
					fid++;
				}
			}
		}
	}
	
	private void plotABPlan(Color c, Graphics g, int actnum, int lastday, boolean shift, boolean drawPercent)
	{
		g.setColor(c);
		int fid = 1;
		//for(G_Activity a : abstart.keySet())//Get the start and finish for each activity
		//{
			//int start = abstart.get(a);
			//int end = abfinish.get(a);
		//for(G_Activity act : sim.getActivities()){
		for(G_Activity act : sim.getSortedActivityList()){
			GanttChartInfo i = ganttinfo.get(act);
			
			if(i==null){
				System.out.println("Missing "+act.getLabel());
				continue;
			}
			
			int start = i.getStart();
			int end = i.getEnd();
			
			if(start < curday || !shift)
			{
				if(end > curday && shift)
					end = curday;
				
				int yb = getRealY(fid, actnum);
				int sx = getRealX(start, lastday);
				int ex = getRealX(end, lastday);
				for(int x = 0; x < linewidth; x++)//and draw the line for it
				{
					int yv = yb - x - linewidth + 1;
					g.drawLine(sx, yv, ex, yv);
					if(drawPercent /*&& act.getPercentCompletion() < 99*/ && act.getPercentCompletion() > 1)
						g.drawString(""+act.getPercentCompletion(), ex, yv);
				}
				
				if(i.getCritical())//and put circles on it if it is critical
				{
					g.setColor(Color.MAGENTA);
					g.fillOval(sx - 5, yb - 5, 10, 10);
					g.fillOval(ex - 5, yb - 5, 10, 10);
					g.setColor(c);
				}
			}
			fid++;
		}
	}
	
	private void plotABPlanCrew(Color c, Graphics g, int actnum, int lastday, boolean shift, boolean drawPercent)
	{
		g.setColor(c);
		int fid = 1;
		for(G_LaborCrew cr : sim.getLaborCrews())
		{//
			//for(G_Activity a : abstart.keySet())
			//{
			for(G_Activity a : sim.getSortedActivityList()){//sim.getActivities()){
				GanttChartInfo i = ganttinfo.get(a);
				
				if(i == null){
					System.out.println("No Gantt info for activity "+a.getLabel());
				}
				else if(a.getAsPlannedLaborUse().contains(cr))//
				{//
					//int start = abstart.get(a);
					//int end = abfinish.get(a);
					
					int start = i.getStart();
					int end = i.getEnd();
					
					if(start < curday || !shift)
					{
						if(end > curday && shift)
							end = curday;
						
						int yb = getRealY(fid, actnum);
						int sx = getRealX(start, lastday);
						int ex = getRealX(end, lastday);
						for(int x = 0; x < linewidth; x++)
						{
							int yv = yb - x - linewidth + 1;
							g.drawLine(sx, yv, ex, yv);
							
							if(drawPercent && a.getPercentCompletion() < 99 && a.getPercentCompletion() > 1)
								g.drawString(""+a.getPercentCompletion(), ex, yv);
						}
						
						if(i.getCritical())
						{
							g.setColor(Color.MAGENTA);
							g.fillOval(sx - 5, yb - 5, 10, 10);
							g.fillOval(ex - 5, yb - 5, 10, 10);
							g.setColor(c);
						}
					}
					fid++;
				}//
			}
		}//
	}
	
	public void setDraw(boolean b){
		draw = b;
	}
	
	public void paintComponent(Graphics g)
	{
//		if(!draw) return;
		removeAll();

		top = toppad;
		left = textwidth + leftpad;
		
		right = getWidth() - rightpad - 100;
		bottom = getHeight() - bottompad;
		
		legendpanel.setSize(legendpanel.getPreferredSize());
		legendpanel.setBounds(right + 10, toppad, legendpanel.getWidth(), legendpanel.getHeight());
		add(legendpanel);
		
		super.paintComponent(g);

		//Draw the background
		drawRect(left, top, right, bottom, Color.WHITE, g);
		
		g.setColor(new Color(220, 220, 220));
		
		//Draw Vertical Lines
		for(int x = 1; x < lastday; x++)
			g.drawLine(getRealX(x, lastday), bottom, getRealX(x, lastday), top);
		
		//Draw Horizontal Lines
		int actnum = sim.getActivities().length + 1;
		for(int x = 1; x < actnum; x++)
			g.drawLine(left, getRealY(x, actnum), right, getRealY(x, actnum));
		
		int index = 1;
		
		if(grouped)//Plot crew stuff
		{
			for(G_LaborCrew cr : sim.getLaborCrews())
			{//
				for(G_Activity a : sim.getSortedActivityList())//sim.getActivities())
				{
					if(a.getAsPlannedLaborUse().contains(cr))//
					{//
						JLabel j = sidelabels.get(a.getID() - 1);
						add(j);
						j.setBounds(0, getRealY(index, actnum) - j.getHeight() / 2, j.getWidth(), j.getHeight());
						index++;
					}
				}
			}
			
			// Plot baseline Schedule
			plotPlanCrew(Color.GREEN, g, actnum, lastday, 1);

			//plot as-planned schedule;
			//plotPlanCrew(tonae.getAsPlannedSched(), Color.RED, g, actnum, lastday, 4);
			
			// Plot as-projected Schedule
			plotABPlanCrew(Color.ORANGE, g, actnum, lastday, false, false);
	
			// Plot as built schedule
			plotABPlanCrew(Color.BLUE, g, actnum, lastday, true, true);
		}
		else //Plot normal stuff
		{
			for(JLabel j : sidelabels)
			{
				add(j);
				j.setBounds(0, getRealY(index, actnum) - j.getHeight() / 2, j.getWidth(), j.getHeight());
				index++;
			}

			// Plot baseline Schedule
			plotPlan(Color.GREEN, g, actnum, lastday, 1);

			//plot as-planned schedule
			//plotPlan(tonae.getAsPlannedSched(), Color.RED, g, actnum, lastday, 4);
			
			// Plot as-projected Schedule
			plotABPlan(Color.ORANGE, g, actnum, lastday, false, false);
	
			// Plot as built schedule
			plotABPlan(Color.BLUE, g, actnum, lastday, true, true);
		}
		
		g.setColor(Color.BLACK);
		g.drawLine(getRealX(curday, lastday), bottom, getRealX(curday, lastday), top);
		
		int space = getRealX(2, lastday) - getRealX(1, lastday);
		
		if(space != 0)
		{
			int amt = maxbottomtext * 2 / space;
			int x = 0;
			for(JLabel l : bottomlabels)
			{
				if(amt != 0 && x % amt == 0)
				{
					add(l);
					l.setBounds(getRealX(x + 1, lastday) - l.getWidth() / 2, bottom, l.getWidth(), l.getHeight());
				}
				x++;
			}
		}
		
		g.setColor(Color.BLACK);
		g.drawRect(left, top, right - left, bottom - top);
	}
}
