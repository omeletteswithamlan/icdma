package mtu.construction.tonae;

import java.io.Serializable;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import mtu.construction.project.Activity;
import mtu.construction.project.TONAE;

/**
 * Holds the cost schedule, the cost of materials, labor, and indirect costs
 * indexed by day 
 */
public class CostSchedule implements Serializable
{
	private TreeMap<Activity, TreeMap<Integer, Double>> act_material;
	private TreeMap<Activity, TreeMap<Integer, Double>> act_labor;
	private TreeMap<Activity, TreeMap<Integer, Double>> act_indirect;
	
	//
	private TreeMap<Integer, Double> query_futures_total_track;
	
	private double query_futures_total;
	
	//the stock does not have an activity associated with it, so it has a value
	//each day of purchased material which has not been used on a project
	private TreeMap<Integer, Double> stockvalue;
	
	public void setqueryFuturesTotal(double d)
	{
		query_futures_total = d;
	}

	/**
	 * constructor, creates treemaps for material, labor, indirect, stockvalue, fast total track and an int of fast day total
	 */
	public CostSchedule()
	{
		act_material = new TreeMap<Activity, TreeMap<Integer, Double>>();
		act_labor = new TreeMap<Activity, TreeMap<Integer, Double>>();
		act_indirect = new TreeMap<Activity, TreeMap<Integer, Double>>();
		stockvalue = new TreeMap<Integer, Double>();
		
		query_futures_total = 0;
		query_futures_total_track = new TreeMap<Integer, Double>();
	}
	
	
	public void updateDayTotal(int i)
	{
		query_futures_total_track.put(i, query_futures_total);
		query_futures_total = 0;
	}
	
	public void setLabor(Activity a, int day, double cost)
	{
		query_futures_total += cost;
		if(TONAE.querymode)
			return;
		
		if(!act_labor.containsKey(a))
			act_labor.put(a, new TreeMap<Integer, Double>());
		
		TreeMap<Integer, Double> sched = act_labor.get(a);
		sched.put(day, cost);
//		if(cost>0&&tonae.getCurrentTimeStep()==day)
//			System.out.println("activity: "+a+ " day "+day+" Labor: $"+cost);
	}
	
	/**
	 * set Material cost in schedule for a day, one indexed,  used to track cumulative cost in the simulator.
	 * 
	 * @param a
	 * @param day
	 * @param cost
	 */
	public void setMaterial(Activity a, int day, double cost)
	{
		//if we are querying futures then add the total and do nothing else
		query_futures_total += cost;
		if(TONAE.querymode)
			return;
		
		//if no entry exists for activity a
		if(!act_material.containsKey(a))
			//make the entry
			act_material.put(a, new TreeMap<Integer, Double>());
		
		//for activity a's cost schedule tree enter the day's material cost
		TreeMap<Integer, Double> sched = act_material.get(a);
		sched.put(day, cost);
//		if(cost>0&&tonae.getCurrentTimeStep()==day)
//			System.out.println("activity: "+a.getID()+ " day "+day+" material: $"+cost);
	}
	
	public void setIndirect(Activity a, int day, double cost)
	{
		query_futures_total += cost;
		if(TONAE.querymode)
			return;

		//if no entry exists for activity a
		if(!act_indirect.containsKey(a))
			//make the entry
			act_indirect.put(a, new TreeMap<Integer, Double>());
		
		//for activity a's cost schedule tree enter the day's indirect cost
		TreeMap<Integer, Double> sched = act_indirect.get(a);
		sched.put(day, cost);
//		if(cost>0&&tonae.getCurrentTimeStep()==day)
//		{
//			//System.out.println("activity: "+a.getID()+ " day "+day+" indirect: $"+cost+" as built");
//			System.out.println("activity: "+a.getID()+ " day "+day+" indirect: $"+cost);//+" as planned");
//			//System.out.println("AC: "+tonae.AC(a));
//			//System.out.println("CV: "+tonae.CV(a));
//			//System.out.println("CVI: "+tonae.CVI(a));
//			//System.out.println("CPI: "+tonae.CPI(a));
//		}
	}
	
	public void addLabor(Activity a, int day, double cost)
	{
		query_futures_total += cost;
		if(TONAE.querymode)
			return;
		
		//if no entry exists for activity a
		if(!act_labor.containsKey(a))
			//make the entry
			act_labor.put(a, new TreeMap<Integer, Double>());
		
		//for activity a's cost schedule tree accumulate the day's labor cost
		TreeMap<Integer, Double> sched = act_labor.get(a);
		sched.put(day, cost + getLabor(a, day));
	}
	
	public void addMaterial(Activity a, int day, double cost)
	{
		query_futures_total += cost;
		if(TONAE.querymode)
			return;

		if(!act_material.containsKey(a))
			act_material.put(a, new TreeMap<Integer, Double>());
		
		TreeMap<Integer, Double> sched = act_material.get(a);
		sched.put(day, cost + getMaterial(a, day));
	}
	
	/**
	 * this function sums up the cost of 
	 * @param a
	 * @param day
	 * @param cost
	 */
	public void addIndirect(Activity a, int day, double cost)
	{
		query_futures_total += cost;
		if(TONAE.querymode)
			return;

		if(!act_indirect.containsKey(a))
			act_indirect.put(a, new TreeMap<Integer, Double>());
		
		TreeMap<Integer, Double> sched = act_indirect.get(a);
		sched.put(day, cost + getIndirect(a, day));
		//TODO: figure out when/how this method is used.
		//NOTE: It is used sometimes, seemingly randomly
		//System.out.println("Activity "+a.getDescription()+" indirect: "+sched.get(day));
	}
	
	public double getLabor(Activity a, int day)
	{
		if(!act_labor.containsKey(a))
			return 0;
		if(!act_labor.get(a).containsKey(day))
			return 0;
		return act_labor.get(a).get(day);
	}
	
	public double getMaterial(Activity a, int day)
	{
		if(!act_material.containsKey(a))
			return 0;
		if(!act_material.get(a).containsKey(day))
			return 0;
		return act_material.get(a).get(day);
	}
	
	public double getIndirect(Activity a, int day)
	{
		if(!act_indirect.containsKey(a))
			return 0;
		if(!act_indirect.get(a).containsKey(day))
			return 0;
		return act_indirect.get(a).get(day);
	}
	
	public double getStockValue(int day)
	{
		if(stockvalue.containsKey(day))
			return stockvalue.get(day);
		
		return 0;
	}
	
	public void setStockValue(int day, double val)
	{
		stockvalue.put(day, val);
	}
	
	public HashMap<Activity, Double> getLabor(int day, TONAE tonae)
	{
		HashMap<Activity, Double> actmap = new HashMap<Activity, Double>();
		for(Activity a : tonae.getProject().getActivities())
			actmap.put(a, getLabor(a, day));
		
		return actmap;
	}
	
	public HashMap<Activity, Double> getMaterial(int day, TONAE tonae)
	{
		HashMap<Activity, Double> actmap = new HashMap<Activity, Double>();
		for(Activity a : tonae.getProject().getActivities())
			actmap.put(a, getMaterial(a, day));
		
		return actmap;
	}
	
	public HashMap<Activity, Double> getIndirect(int day, TONAE tonae)
	{
		HashMap<Activity, Double> actmap = new HashMap<Activity, Double>();
		for(Activity a : tonae.getProject().getActivities())
			actmap.put(a, getIndirect(a, day));
		
		return actmap;
	}
	
	public double getLaborTotal(int day, TONAE tonae)
	{
		double d = 0;
		for(Activity a : tonae.getProject().getActivities())
			d += getLabor(a, day);
		return d;
	}

	public double getMaterialTotal(int day, TONAE tonae)
	{
		double d = 0;
		for(Activity a : tonae.getProject().getActivities())
			d += getMaterial(a, day);
		
		d += getStockValue(day);
		
		return d;
	}

	public double getIndirectTotal(int day, TONAE tonae)
	{
		double d = 0;
		for(Activity a : tonae.getProject().getActivities())
			d += getIndirect(a, day);
		return d;
	}
	
	public TreeMap<Integer, Double> getDirect(TONAE tonae)
	{
		TreeMap<Integer, Double> daymap = new TreeMap<Integer, Double>();
		
		int ld = tonae.getLastTimeStep();
		for(int x = 1; x < ld; x++)
			daymap.put(x, getMaterialTotal(x, tonae) + getLaborTotal(x, tonae));
		
		return daymap;
	}
	
	public TreeMap<Integer, Double> getIndirect(TONAE tonae)
	{
		TreeMap<Integer, Double> daymap = new TreeMap<Integer, Double>();
		
		int ld = tonae.getLastTimeStep();
		for(int x = 1; x < ld; x++)
			daymap.put(x, getIndirectTotal(x, tonae));
		
		return daymap;
	}
	
	public TreeMap<Integer, Double> getMaterial(TONAE tonae)
	{
		TreeMap<Integer, Double> daymap = new TreeMap<Integer, Double>();
		
		int ld = tonae.getLastTimeStep();
		for(int x = 1; x < ld; x++)
			daymap.put(x, getMaterialTotal(x, tonae));
		
		return daymap;
	}
	
	public TreeMap<Integer, Double> getLabor(TONAE tonae)
	{
		TreeMap<Integer, Double> daymap = new TreeMap<Integer, Double>();
		
		int ld = tonae.getLastTimeStep();
		for(int x = 1; x < ld; x++)
			daymap.put(x, getLaborTotal(x, tonae));
		
		return daymap;
	}
	
	public TreeMap<Integer, Double> getTotal(TONAE tonae)
	{
		TreeMap<Integer, Double> daymap = new TreeMap<Integer, Double>();
		
		int ld = tonae.getLastTimeStep();
		for(int x = 1; x < ld; x++)
			daymap.put(x, getMaterialTotal(x, tonae) + getLaborTotal(x, tonae) + getIndirectTotal(x, tonae));
		
		return daymap;
	}
	
	public double getMaterial(Activity a, TONAE tonae)
	{
		if(!act_material.containsKey(a))
			return 0;
		
		double total = 0;
		for(Entry<Integer, Double> d : act_material.get(a).entrySet())
		{
			if(d.getKey() < tonae.getCurrentTimeStep())
				total += d.getValue();
		}
		
		return total;
	}
	
	public double getLabor(Activity a, TONAE tonae)
	{
		if(!act_labor.containsKey(a))
			return 0;
		
		double total = 0;
		for(Entry<Integer, Double> d : act_labor.get(a).entrySet())
		{
			if(d.getKey() < tonae.getCurrentTimeStep())
				total += d.getValue();
		}
		
		return total;
	}
	
	public double getIndirect(Activity a, TONAE tonae)
	{
		if(!act_indirect.containsKey(a))
			return 0;
		
		double total = 0;
		for(Entry<Integer, Double> d : act_indirect.get(a).entrySet())
		{
			if(d.getKey() < tonae.getCurrentTimeStep())
				total += d.getValue();
		}
		
		return total;
	}
	
	public double getTotal(Activity a, TONAE tonae)
	{
		System.out.println(getIndirect(a, tonae) + getLabor(a, tonae) + getMaterial(a, tonae));
		System.out.println(getIndirect(a, tonae) + getLabor(a, tonae) + getMaterial(a, tonae));
		System.out.println(getIndirect(a, tonae) + getLabor(a, tonae) + getMaterial(a, tonae));
		System.out.println(getIndirect(a, tonae) + getLabor(a, tonae) + getMaterial(a, tonae));
		System.out.println(getIndirect(a, tonae) + getLabor(a, tonae) + getMaterial(a, tonae));
		return getIndirect(a, tonae) + getLabor(a, tonae) + getMaterial(a, tonae);
	}
	
	public TreeMap<Integer, Double> getTotalByActivity(Activity a, TONAE tonae)
	{
		TreeMap<Integer, Double> costmap = new TreeMap<Integer, Double>();
		
		for(Entry<Integer, Double> e : act_labor.get(a).entrySet())
			costmap.put(e.getKey()+1, e.getValue());

		for(Entry<Integer, Double> e : act_material.get(a).entrySet())
		{
			if(costmap.containsKey(e.getKey()))
				costmap.put(e.getKey()+1, costmap.get(e.getKey()+1) + e.getValue());
			else
				costmap.put(e.getKey()+1, e.getValue());
		}

		for(Entry<Integer, Double> e : act_indirect.get(a).entrySet())
		{
			if(costmap.containsKey(e.getKey()))
				costmap.put(e.getKey()+1, costmap.get(e.getKey()+1) + e.getValue());
			else
				costmap.put(e.getKey()+1, e.getValue());
		}
		
		return costmap;
	}
	
	public TreeMap<Integer, Double> getQueryFuturesTotal()
	{
		return query_futures_total_track;
	}
}
