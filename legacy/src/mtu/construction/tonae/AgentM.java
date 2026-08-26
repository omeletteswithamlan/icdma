package mtu.construction.tonae;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;

import mtu.construction.project.Activity;
import mtu.construction.project.LaborCrew;
import mtu.construction.project.LaborType;
import mtu.construction.project.MaterialType;
import mtu.construction.project.TONAE;
import mtu.construction.tonae.ANode;
import mtu.construction.tonae.PNode;
//import tonae.resource.ResourceAllocation;

/**
 * Does cost computations. This class has evolved a lot since its inception, AgentM may no
 * longer be a good name for it. -matt
 * 
 * @author Amlan Mukherjee
 * @author Matt Watkins
 * @author Corey Tebo
 */
public class AgentM implements Serializable
{
	private static final long serialVersionUID = 732812484345691759L;
	
	// thees are constents that should be paramiterised, to either the database or the config pannel
	public static final double OVER_TIME_RATE = 2.0;
	public static final int OVER_TIME_DAYS = 5;
	public static final int OVER_TIME_HOURS= 8;
	public static double overhead = 0.1;
	public static double overstockLoss = 0.9;// loss of material value
	
	private CostSchedule asPlanned;
	private CostSchedule asBuilt;

	//map of an activity, to a map of days, and progress: 
	//for each activity how done will you be on a given day
	private HashMap<Activity, TreeMap<Integer, Double>> asplannedprogress;
	private HashMap<Activity, TreeMap<Integer, Double>> asbuiltprogress;

	/**
	 * this class is called by TONAE upon connection to the database in order 
	 * to populate the schedules of compleation for each activity,
	 * our assumptions about the compleation have changed since this class 
	 * was written and I am now evaluating the class for potential bugs.
	 *      Corey Tebo 7/14/2009
	 * @param TONAE
	 */
	public AgentM(TONAE TONAE)
	{
		overstockLoss = TONAE.getProject().getOverstockPenalty();// constent
		overhead = TONAE.getProject().getOverhead();//percentage of materials

		asPlanned = new CostSchedule();
		asBuilt = new CostSchedule();

		asplannedprogress = new HashMap<Activity, TreeMap<Integer, Double>>();
		asbuiltprogress = new HashMap<Activity, TreeMap<Integer, Double>>();
		Activity[] a = TONAE.getProject().getActivities();
		int last = TONAE.getLastTimeStep();
		for(Activity act : a)
		{
			//this Map links days to progress for an activitiy
			TreeMap<Integer, Double> map = new TreeMap<Integer, Double>();
			for(int x = 1; x < last; x++)
			{
				if(act.getStart() > x)
					map.put(x, 0.0);
				else if(act.getEnd() < x)
					map.put(x, 1.0);
				else
				{
					double totalduration = act.getDuration();
					double remainingduration = x - act.getStart();
					map.put(x, remainingduration / totalduration);
				}
			}

			asplannedprogress.put(act, map);
			// this map links the above maps(of time to progress for an activity) to there respective activity objects 
			TreeMap<Integer, Double> t = new TreeMap<Integer, Double>();
			t.put(1, 0.0);
			asbuiltprogress.put(act, t);
		}
		computeCost(asPlanned, 1, TONAE.getLastTimeStep(), TONAE);
		computeCost(asBuilt, 1, TONAE.getLastTimeStep(), TONAE);
	}

	//this function takes a vector of resources and and labor crew hired 

	/**
	 * this function takes the resources as allocated by the user for this turn, 
	 * applies the cost to the activities, and updates the stock
	 * 
	 * @param alloc - a vector of resource allocations for each activity to be calculated.
	 *        ResourceAllocation is a data object holding the labor, materials that an 
	 *        activity needs as well as the number of hours and days that the activity is
	 *        to be worked on during the following time period
	 * @param hired - the labor crew to be removed for the labor pool
	 * @param tonae
	 */
/*	public void computeCost(Vector<ResourceAllocation> alloc, LaborCrew hired, TONAE tonae)
	{
		CostSchedule sched = asBuilt;
		int day = tonae.getCurrentTimeStep();

		//set today's cost here
		for(Activity a : tonae.getProject().getActivities())
		{
			sched.setMaterial(a, day, sched.getMaterial(a, day - 1),tonae);
			sched.setLabor(a, day, sched.getLabor(a, day - 1),tonae);
			sched.setIndirect(a, day, sched.getIndirect(a, day - 1),tonae);
		}

		double cost = 0;

		for(ResourceAllocation r : alloc)
		{
			Activity a = r.getActivity();
			if(a != null)
			{
				for(LaborCrew c : r.getLaborCrews())
				{

					double time=0;
					double todayCost = 0; //the cost for the labor crew

					int interval = tonae.getProject().getTimeFrame().getInterval();
					switch (interval)
					{
					case 1:
						time=r.getWorkHours();//default
						int cday = tonae.getCalendar().get(Calendar.DAY_OF_WEEK);

						if ((r.getWorkDays()>5 && cday == Calendar.SATURDAY) || (r.getWorkDays()>6 && cday == Calendar.SUNDAY))
							time*=OVER_TIME_RATE; //if working overtiem else day off
						else if(cday == Calendar.SATURDAY || cday == Calendar.SUNDAY)
							time=0;
						else if (time > OVER_TIME_HOURS)//regularday with over time
							time=((time - OVER_TIME_HOURS)*OVER_TIME_RATE)+OVER_TIME_HOURS;

						time /= OVER_TIME_HOURS;

						//the only case remaining is regularday no overtime which needs no special adjustment
						break;

					case 7:
						time = r.getWorkHours()*r.getWorkDays();
						if (time>OVER_TIME_HOURS*OVER_TIME_DAYS)//overtime
							time = ((time -(OVER_TIME_HOURS*OVER_TIME_DAYS))*OVER_TIME_RATE) + (OVER_TIME_HOURS*OVER_TIME_DAYS);
						time /= OVER_TIME_HOURS*OVER_TIME_DAYS;
						break;

					case 28:
						time = r.getWorkHours()*r.getWorkDays()*4;
						if (time>OVER_TIME_HOURS*OVER_TIME_DAYS*4)//overtime
							time = ((time -(OVER_TIME_HOURS*OVER_TIME_DAYS*4))*OVER_TIME_RATE) + (OVER_TIME_HOURS*OVER_TIME_DAYS*4);
						time /= OVER_TIME_HOURS*OVER_TIME_DAYS*4;
						break;
					default:
						throw new Error("Unspecified time frame");
					}

					//Get the correct cost for the labor crew, in case we hire extra or fire people, etc.
					for(LaborType b : c.getTypes()){
						//System.out.println(b.getDescription()+": "+c.getAmt(b)+" ,Cost: "+c.getAmt(b)*b.getCost());
						todayCost += c.getAmt(b)*b.getCost();
					}
					//System.out.println("Total Cost for this crew: "+todayCost);

					//System.out.println(" ajusted hours :"+time+" wage :"+c.getDailyCost()+" incentive :"+r.getWageIncentive()+" total cost :"+time*c.getDailyCost()*r.getWageIncentive());
					//adujsted work hours times by wage and wage incentive, double pay rate for overtime
					sched.addLabor(a, day, time * c.getDailyCost() * r.getWageIncentive(),tonae); // should day be c?
					cost += time * c.getDailyCost() * r.getWageIncentive();
				}

				for(MaterialType t : tonae.getProject().getMaterialTypes())
				{
					sched.addMaterial(a, day, r.getUsed(t) * t.getCost(),tonae); //here as well
					cost += r.getUsed(t) * t.getCost();
				}

				sched.addIndirect(a, day, a.computeDailyMaterialCost() * overhead,tonae);// is this incorect? day is used to spesify crew. FROM MATT: What? this comment makes no sense to me. Day is used to specify day for the schedule
				cost += a.computeDailyMaterialCost() * overhead;
			}
		}

		sched.setStockValue(day, tonae.getStock().getValue());

		//doing this circumvents how this was intended to work, but the intended way has a bug and I don't have
		//the time to fix it at the moment. Normally, fast_day_total gets updated by the addIndirect, addMaterial,
		//and addLabor calls to sched. But, there is a bug with that. So, I just compute the cost change
		if(sched.getQueryFuturesTotal().containsKey(day-1))
			cost += sched.getQueryFuturesTotal().get(day - 1);
		sched.setqueryFuturesTotal(cost);

		sched.updateDayTotal(tonae.getCurrentTimeStep());

		//do not do any further calculations if we are in query mode, since that is useless
		if(TONAE.querymode)
			return;

		//calculate a projection for the remainder of the project  
		computeCost(asBuilt, tonae.getCurrentTimeStep() + 1, tonae.getLastTimeStep(), tonae);

		Set<ANode> anodes = tonae.getANodeSet();
		for(ANode node : anodes)
		{
			TreeMap<Integer, Double> map = asbuiltprogress.get(node.getParentAct());
			//This is a starting a-node
			if(node.getOutPrimaryArc() != null)
			{
				PNode p = (PNode)node.getOutPrimaryArc().getHeadNode();
				double start = p.getStart();
				double end = p.getEnd();
				double now = p.getEarlyStart();

				if(now > end)
					map.put(tonae.getCurrentTimeStep() + 1, 1.0);
				else if(now < start)
					map.put(tonae.getCurrentTimeStep() + 1, 0.0);
				else
					map.put(tonae.getCurrentTimeStep() + 1, 1.0 - ((end - now) / p.getParentAct().getDuration()));
				//I believe that wage incentive must beaded in this code block to function correctly.
			}

		}
		//		// the following is debug code
		//		System.out.println();
		//		System.out.println(";;ap mat; ab mat; detlta mat;ap labor; ab labor; detlta labor;ap indir; ab indir; detlta indir");
		//		System.out.print("day: "+ tonae.getCurrentTimeStep()+";");
		//		for(Activity a : tonae.getPlan().getActivities())
		//		{
		//			System.out.print("Activity"+a.getID()+";");
		//			if (0.005<asBuilt.getMaterial(a, day)+asBuilt.getLabor(a, day)+asBuilt.getIndirect(a, day)&&a.getEnd()>day) 
		//				System.out.print(""+ a.computeDailyMaterialCost()+""+ asBuilt.getMaterial(a, day)+ ";"+asBuilt.getLabor(a, day)+";"+asBuilt.getIndirect(a, day)+";"+asPlanned.getMaterial(a, day)+asPlanned.getLabor(a, day)+";"+asPlanned.getIndirect(a, day));
		//			else
		//				System.out.print(";;; ;;; ;;; ;;;");
		//		}
	}
*/
	//compute cost 
	/**
	 * use this function to compute the costs of projects in a particular day range
	 * @param sched schedual may be as built as planed etc
	 * @param firstDay
	 * @param lastDay
	 * @param tonae TONAE - the project state
	 */
	public void computeCost(CostSchedule sched, int firstDay, int lastDay, TONAE tonae)
	{
		HashMap<Activity, Double> material = sched.getMaterial(firstDay - 1, tonae);
		HashMap<Activity, Double> labor = sched.getLabor(firstDay - 1, tonae);
		HashMap<Activity, Double> indirect = sched.getIndirect(firstDay - 1, tonae);

		for(int x = firstDay; x <= lastDay; x++)
		{
			sched.setStockValue(x, 0);
			for(ANode anode : tonae.getANodeSet())
			{
				if(anode.getOutPrimaryArc() == null)
					continue;

				Activity a = anode.getParentAct();
				int start = anode.getEarlyStart();
				int end = anode.getOutPrimaryArc().getHeadNode().getOutPrimaryArc().getHeadNode().getEarlyStart();

				if(x >= start && x < end)// potental to start activities before constraints are satisfied
				{
					double mat = a.computeDailyMaterialCost();
					labor.put(a, labor.get(a) + a.computeDailyLaborCost());
					material.put(a, material.get(a) + mat);
					//System.out.println("Indirect cost for "+a.getDescription()+" WAS "+indirect.get(a));
					indirect.put(a, indirect.get(a) + mat * overhead);
					//System.out.println("Material cost for "+a.getDescription()+" is "+mat);
					//System.out.println("Indirect cost for "+a.getDescription()+" is "+indirect.get(a)+" = "+mat+"*"+overhead);
				}
			}

			for(Entry<Activity, Double> e : material.entrySet())
				sched.setMaterial(e.getKey(), x, e.getValue());

			for(Entry<Activity, Double> e : labor.entrySet())
				sched.setLabor(e.getKey(), x, e.getValue());

			for(Entry<Activity, Double> e : indirect.entrySet())
				sched.setIndirect(e.getKey(), x, e.getValue());
		}
	}

	public void setOverstockLoss(double osl)
	{
		overstockLoss=osl;
	}

	public double getOverstockLoss()
	{
		return overstockLoss;
	}
	public double computeComputeDailyTotalCost()
	{
		return 0.0;
	}

	public double computeDailyActivityCost(CostSchedule sched, int first, int last, TONAE simulator)
	{
		return 0.0;
	}

	public TreeMap<Integer, Double> getAsPlannedProgress(Activity a)
	{
		return asplannedprogress.get(a);
	}

	public TreeMap<Integer, Double> getAsBuiltProgress(Activity a)
	{
		return asbuiltprogress.get(a);
	}

	public CostSchedule getAsPlanned()
	{
		return asPlanned;
	}

	public CostSchedule getAsBuilt()
	{
		return asBuilt;
	}

	public TreeMap<Integer, Double> getAsPlannedDirectDailyCost(TONAE tonae)
	{
		return asPlanned.getDirect(tonae);
	}

	public TreeMap<Integer, Double> getAsPlannedMaterialDailyCost(TONAE tonae)
	{
		return asPlanned.getMaterial(tonae);
	}

	public TreeMap<Integer, Double> getAsPlannedLaborDailyCost(TONAE tonae)
	{
		return asPlanned.getLabor(tonae);
	}

	public TreeMap<Integer, Double> getAsPlannedIndirectDailyCost(TONAE tonae)
	{
		return asPlanned.getIndirect(tonae);
	}

	public TreeMap<Integer, Double> getAsBuiltDirectDailyCost(TONAE tonae)
	{
		return asBuilt.getDirect(tonae);
	}

	public TreeMap<Integer, Double> getAsBuiltMaterialDailyCost(TONAE tonae)
	{
		return asBuilt.getMaterial(tonae);
	}

	public TreeMap<Integer, Double> getAsBuiltLaborDailyCost(TONAE tonae)
	{
		return asBuilt.getLabor(tonae);
	}

	public TreeMap<Integer, Double> getAsBuiltIndirectDailyCost(TONAE tonae)
	{
		return asBuilt.getIndirect(tonae);
	}
}
