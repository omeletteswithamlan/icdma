package mtu.construction.gui.wrapper;

import mtu.construction.icdma.Simulator;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;

import mtu.construction.project.LaborCrew;
import mtu.construction.project.MaterialType;
import mtu.construction.project.TONAE;

import mtu.construction.tonae.ResourceAllocation;

public class G_ResourceAlloc {
	
	private G_Activity activity;
	private HashMap<G_Material, Integer> requested_material;
	private HashSet<G_LaborCrew> requested_labor;
	
	private int workhours, workdays;
	private double wageincentive=1.0;
	private double order =1.0;// this is the percentage of default material use to be bought new
	
	public G_ResourceAlloc(ResourceAllocation r, TONAE t, Simulator sim){
		workhours = r.getWorkHours();
		workdays = r.getWorkDays();
		wageincentive = r.getWageIncentive();
		requested_material = new HashMap<G_Material, Integer>();
		requested_labor = new HashSet<G_LaborCrew>();
		order = r.getOrder();
		
		//Get Activity
		if(r.getActivity() != null){
			//activity = new G_Activity(r.getActivity(), t, sim);
			for(G_Activity a : sim.getActivities()){
				if(r.getActivity().getID() == a.getID()){
					activity = a;
				}
			}
			if(activity == null) activity = new G_Activity(r.getActivity(), t, sim);
		}
		
		//Requested Material
		for(G_Material m : sim.getMaterialTypes()){
			int amt = r.getRequested(m.getMaterial());
			if(amt > 0){
				requested_material.put(m, amt);
			}
		}
		//Requested Labor
		for(LaborCrew c : r.getLaborCrews()){
			for(G_LaborCrew c2 : sim.getLaborCrews()){
				if(c2.getID() == c.getID())
					requested_labor.add(c2);
			}
		}
	}
	
	//Has a crippled activity?
	public G_ResourceAlloc(G_Activity a){
		requested_material = new HashMap<G_Material, Integer>();
		requested_labor = new HashSet<G_LaborCrew>();
		activity = a;
		workhours = 8;
		workdays = 5;
	}
	
	public int getRequested(G_Material m){
		//if(requested_material.containsKey(m)) System.out.println("Contained.");
		//else  System.out.println("Gone.");
		if(requested_material.containsKey(m))
		return requested_material.get(m);
		else return 0;
	}
	
	public void requestMaterial(G_Material t, int amount){
		//if(t==null) System.out.println("fail.");
		//if(requested_material == null) System.out.println("fail2");
		int oldAmt = 0;
		if(requested_material.containsKey(t))
			oldAmt = requested_material.get(t);
		
		requested_material.put(t, oldAmt+amount);
	}
	
	public void requestLaborCrew(G_LaborCrew c){
		requested_labor.add(c);
	}
	
	public HashSet<G_LaborCrew> getRequestedLabor(){
		return requested_labor;
	}
	
	public G_Activity getActivity(){
		return activity;
	}
	
	public void setWorkDays(int days){
		workdays = days;
	}
	public int getWorkDays(){
		return workdays;
	}
	
	public void setWorkHours(int hours){
		workhours = hours;
	}
	public int getWorkHours(){
		return workhours;
	}
	
	public void setWageIncentive(double incentive){
		wageincentive = incentive;
	}
	public double getWageIncentive(){
		return wageincentive;
	}
	
	public double computeWorkQuantityMultiplier(int interval, int dayofweek)
	{
		double work = -1;
		double constent=2;
		
		if (activity == null) return 0;
		
		double hourfactor = workhours * workdays/ 40.0;
		double wagefactor = constent-(constent-1.0)/wageincentive;
		
		if(hourfactor > 1)
		{
			//overtime work is only half as productive
			hourfactor -= 1;
			hourfactor *= .5;
			hourfactor += 1;
		}
		for(G_LaborCrew c : activity.getAsPlannedLaborUse())
		{
			for(G_LaborCrew o : requested_labor)
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
		
//		int interval = getProject().getTimeFrame().getInterval();
//		int dayofweek = getCalendar().get(Calendar.DAY_OF_WEEK);
		
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
	
	//Set the total percentage of materials ordered
	public void setTotalOrdered(double d){
		activity.setTotalMaterialsOrdered(d);
	}
	
	//Get the total percentage of materials ordered
	public double getTotalOrdered(){
		return activity.getTotalMaterialsOrdered();
	}
}
