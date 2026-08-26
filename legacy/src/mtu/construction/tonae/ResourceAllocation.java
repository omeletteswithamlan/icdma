package mtu.construction.tonae;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import mtu.construction.project.Activity;
import mtu.construction.project.LaborCrew;
import mtu.construction.project.MaterialType;

/**
 * holds all the labor and materials that are allocated to a activity at the moment
 * 
 * Note: this object acts as an accumulator and it's values change over time as
 *       the user and simulator call it's accessors.
 * 
 * @author Mat Watkens
 * @author Corey Tebo
 *
 */
public class ResourceAllocation
{
	/**
	 * reference to the activity class associated to this resource allocation
	 * represents the general stock of the project if null
	 */
	private Activity activity;
	
	private HashMap<MaterialType, Integer> requested_material;
	private HashSet<LaborCrew> requested_labor;
	
//	private HashMap<MaterialType, Integer> allocated_material;
//	private HashMap<MaterialType, Integer> used;
	
	private int workhours, workdays;
	private double wageincentive=1.0;
	private double order = 1.0;//the percentage of default material usage to be orderd new
	
	private double unadjustedprod;
	
	/**
	 * @param a activity; if null activity is the stock
	 * this should totally be the domain of activity and tonae not the gui
	 */
	public ResourceAllocation(Activity a)
	{
		activity = a;
		
		requested_material = new HashMap<MaterialType, Integer>();
		requested_labor = new HashSet<LaborCrew>();
		
//		allocated_material = new HashMap<MaterialType, Integer>();
//		used = new HashMap<MaterialType, Integer>();
		
		// i believe the defaults cover for when the values are not specified in the database
		workhours = 8;
		workdays = 5;
	}	
	
	/**
	 * @return if null, represents general stock for project after turn
	 */
	public Activity getActivity()
	{
		return activity;
	}
	
	//
	public void clearLaborRequest()
	{
		requested_labor.clear();
	}
	
	//Request materials for this activity
	public void request(MaterialType type, int d)
	{
		int amt = 0;
		if(requested_material.containsKey(type))
			amt = requested_material.get(type);
		amt += d;
		requested_material.put(type, amt);
	}

	//Request laborcrew for this activity
	public void request(LaborCrew crew)
	{
		requested_labor.add(crew);
	}
	
	//Get laborcrews requested for this activity
	public HashSet<LaborCrew> getLaborCrews()
	{
		return requested_labor;
	}
	
	//Get the amount of requested material
	public int getRequested(MaterialType type)
	{
		if(requested_material.containsKey(type))
			return requested_material.get(type);
		return 0;
	}
		
	//set the amount of material
//	public void set(MaterialType type, int d)
//	{
//		allocated_material.put(type, d);
//	}
	
	//Get amount of available material
//	public int getAvailable(MaterialType type)
//	{
//		if(allocated_material.containsKey(type))
//			return allocated_material.get(type);
//		return 0;
//	}
	
	//Remove a certain amount of material from "available", put it into "used"
//	public int removeAvailable(MaterialType type, int d)
//	{
//		int quant = getAvailable(type);
//		if(quant < d)
//			d = quant;
		
//		allocated_material.put(type, quant - d);
//		used.put(type, d);
//		return d;
//	}
	
	//Get the amount of material used...
//	public int getUsed(MaterialType type)
//	{
//		if(used.containsKey(type))
//			return used.get(type);
//		return 0;
		
//	}

/*======== The following methods/variables should be moved to Activity or something. ========*/
	
//Work Hours, used by AgentM.computeCost, ScheduleCalculator.ComputeWorkQuantityMultiplier. (set) is used by TONAE.getDefaultResourceAllocation
	
	public void setWorkHours(int d)
	{
		workhours = d;
	}
	
	public int getWorkHours()
	{
		return workhours;
	}


//Work Days, used by AgentM.computeCost, ScheduleCalculator.ComputeWorkQuantityMultiplier. (set) is used by TONAE.getDefaultResourceAllocation
	
	public void setWorkDays(int d)
	{
		workdays = d;
	}
	
	public int getWorkDays()
	{
		return workdays;
	}
	
	
//Wage Incentive, used by AgentM.computeCost, ScheduleCalculator.ComputeWorkQuantityMultiplier. (set) is never used.
	
	public void setWageIncentive(double i)
	{
		wageincentive=i;
	}
	
	public double getWageIncentive()
	{
		return wageincentive;
	}
	
	//computes the percentage of work on an activity which can be completed, based on the available labor
	//requires work hours/days/wageincentive, laboruse, and crew allocation
	public double computeWorkQuantityMultiplier(int interval, int dayofweek)
	{
		double work = -1;
		double constent=2;
		
		double hourfactor = workhours * workdays/ 40.0;
		double wagefactor = constent-(constent-1.0)/wageincentive;
		
		if(hourfactor > 1)
		{
			//overtime work is only half as productive
			hourfactor -= 1;
			hourfactor *= .5;
			hourfactor += 1;
		}
		for(LaborCrew c : activity.getLaborUse())
		{
			for(LaborCrew o : getLaborCrews())
			{
				if(c.getID() == o.getID())
				{
					double perc = c.compareProductivity(o);
					if(perc > 1)
					{
						//congestion causes excess work to be 80% as efficient.
						perc -= 1;
						perc *= .8;
						perc += 1;
					}
					
					perc *= hourfactor;
					
					if(work == -1 || perc < work)
						work = perc;
				}
			}
		}
		
		//int interval = getProject().getTimeFrame().getInterval();
		//int dayofweek = getCalendar().get(Calendar.DAY_OF_WEEK);
		
		if(interval == 1)
		{
			if((dayofweek == Calendar.SATURDAY && workdays <= 5) || (dayofweek == Calendar.SUNDAY && workdays <= 6))
				return 0;
		}
		
		if(work == -1)
			return 0;
		else
			return work*wagefactor;
	}

	public void setOrder(double order) {
		this.order = order;
	}

	public double getOrder() {
		return order;
	}
	
	//computes the percentage of work on an activity which can be completed, based on available materials
	//requires materialuse, available material allocation
/*	public double computeMaterialQuantityMultipler()
	{
		double mat = -1;
		
		if(activity == null) return 0;
		
		for(Entry<MaterialType, Integer> e : activity.getMaterialUse().entrySet())
		{
			double perc = (double)getAvailable(e.getKey()) / (double)e.getValue();
			if(mat == -1 || perc < mat)
				mat = perc;
		}
		
		//work is -1 if the activity doesn't require labor. Shouldn't ever happen, but just in case it does...
		if(mat == -1)
			return 0;
		else
			return mat;
	}
*/	
	
//Unadjusted Prod (productivity?) Not used. Not even set in the constructor.
	
/*	public void setUnadjustedProd(double p)
	{
		unadjustedprod = p;
	}
	
	public double getUnadjustedProd()
	{
		return unadjustedprod;
	}
*/
}
