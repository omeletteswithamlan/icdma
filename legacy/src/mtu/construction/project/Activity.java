package mtu.construction.project;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import mtu.construction.tonae.AgentM;

/**
 * This class specifies a particular activity which is to occur in a construction project.
 * It contains information about the projected duration of the project, how much labor
 * will be needed for the project, what materials will be needed for the project, and
 * which activities must be complete before this activity can begin.
 * 
 * @author  Matt Watkins
 * @author  Ryan Anderson
 * @author	Jessica Anderson
 */
public class Activity implements Serializable, Comparable
{
	//Label for this activity
	private String description = "";
	private String code = "";

	//As planned duration
	private int duration;
	
	//identifier for this activity
	private int id;		//id is id according to simulation. Each activity has an id 1-n, and n is number of activities.
	private int realid;	//realid is primary key from database
	
	//critical material for this activity
	private HashSet<Integer> drivingMaterials;
	
	//start time for this activity
	private int start;
	
	//CSI Division
	private CSIDivision div;
	
	//Responsibility
	private Responsibility respons;
	
	//this activity proceeds the activities in the Constraint objects
	protected HashSet<Constraint> backlinks;
	
	//this activity succeeds the activities in the Constraint objects
	protected HashSet<Constraint> links;
	
	protected HashMap<MaterialType, Integer> materialuse;
	protected HashMap<MaterialType, MaterialInfo> materialinfo;
	protected HashSet<LaborCrew> laboruse;
	
	private Media[] media;
	
	public Activity(int a_id, String p_label, String code, int p_duration, CSIDivision csidiv, Responsibility responsibility)
	{
		id = a_id;
		realid = a_id;
		description = p_label;
		this.code=code;
		duration = p_duration;

		div = csidiv;
		respons = responsibility;
		
		backlinks = new HashSet<Constraint>();
		links = new HashSet<Constraint>();
		
		materialuse = new HashMap<MaterialType, Integer>();
		materialinfo = new HashMap<MaterialType, MaterialInfo>();
		laboruse = new HashSet<LaborCrew>();
		
		start = 1;
	}
	
	public Activity(int a_id, String p_label, String code, int p_duration, CSIDivision csidiv, Responsibility responsibility, HashSet<Integer> drivingMaterials)
	{
		this(a_id, p_label, code, p_duration, csidiv, responsibility);
		this.drivingMaterials = drivingMaterials;
	}
	
	
	public void setDescription(String d)
	{
		description = d;
	}
	
	public String getDescription()
	{
		return description;
	}
	
	public void setDivision(CSIDivision d)
	{
		div=d;
	}
	
	public CSIDivision getDivision()
	{
		return div;
	}
	
	public void setResponsibility(Responsibility r)
	{
		respons = r;
	}
	
	/*public Responsibility getResponsibility())
	{
		return respons;
	}*/

	public void fixSerialize(Responsibility[] res, CSIDivision[] divs, Constraint[] cons, LaborCrew[] crews, MaterialType[] materials)
	{
		for(LaborCrew c : crews)
		{
			for(LaborCrew o : (HashSet<LaborCrew>)(laboruse.clone()))
			{
				if(o.getID() == c.getID())
				{
					laboruse.remove(o);
					laboruse.add(c);
				}
			}
		}
		
		for(MaterialType t : materials)
		{
			for(Entry<MaterialType, Integer> e : ((HashMap<MaterialType, Integer>)(materialuse.clone())).entrySet())
			{
				if(t.getID() == e.getKey().getID())
				{
					materialuse.remove(e.getKey());
					materialuse.put(t, e.getValue());
				}
			}
		}
		
		for(Constraint c : cons)
		{
			for(Constraint o : (Set<Constraint>)(backlinks.clone()))
			{
				if(o.getID() == c.getID())
				{
					backlinks.remove(o);
					backlinks.add(c);
				}
			}
			
			for(Constraint o : (Set<Constraint>)(links.clone()))
			{
				if(o.getID() == c.getID())
				{
					links.remove(o);
					links.add(c);
				}
			}
		}
		
		for(CSIDivision d : divs)
		{
			if(div.getCSIId() == d.getCSIId())
				div = d;
		}
		
		for(Responsibility r : res)
		{
			if(respons.getResponsibilityId() == r.getResponsibilityId())
				respons = r;
		}
	}

	public int getRealID()
	{
		return realid;
	}
	
	public Media[] getMedia()
	{
		return media;
	}
	
	public void setMedia(Media[] media)
	{
		this.media = media;
	}
	
	public Set<Constraint> getConstraints()
	{
		return links;
	}
	
	public Set<Constraint> getBackConstraints()
	{
		return backlinks;
	}
	
	public boolean equals(Object o)
	{
		if(o instanceof Activity)
		{
			Activity a = (Activity)o;
			return a.id == id;
		}
		return false;
	}
	
	public int hashCode()
	{
		return id;
	}
	
	public void setID(int i)
	{
		id = i;
	}
	
	//Accessor method for label
	public String getLabel()
	{
		if(description == null || description.equals(""))
			return "Activity " + id;
		return description;
	}

	public String getCode()
	{
		return code;
	}
	
	public void setCode(String code)
	{
		this.code=code;
	}
	
	//Accessor method for duration
	public int getDuration()
	{
		return duration;
	}
	
	//gets the start time of the activity
	public int getStart()
	{
		return start;
	}
	
	public int getEnd()
	{
		return start + duration;
	}
	
	
	/**
	 * 
	 * @param d
	 * @param total_work_left
	 */
	public void setDuration(int d, double total_work_left)
	{
		double base_rate = computeDailyMaterialCost();
		double remainingdur = total_work_left / base_rate;
		
		double perc = remainingdur / (remainingdur + (d - duration));
		
		for(Entry<MaterialType, Integer> e : materialuse.entrySet()) 
			materialuse.put(e.getKey(), (int)Math.round(e.getValue() * perc));
		
		duration = d;
		
		//set start will cause the delay to propogate through the schedule
		setStart(start);
	}
	
	/**
	 * setStart
	 *  moves back the start date of an activity if
	 * @param s
	 */
	public void setStart(int s)
	{
		start = s;
		Iterator<Constraint> i = links.iterator();
		while(i.hasNext())
		{
			Constraint c = i.next();
			if(c.getTo().getStart() < start + duration + c.getDuration())
				c.getTo().setStart(start + duration + c.getDuration());
		}
	}
	
	//gets the ID of the activity
	public int getID()
	{
		return id;
	}
	
	public boolean hasLaborUse()
	{
		return laboruse!=null;
	}
	public HashMap<MaterialType, Integer> getMaterialUse()
	{
		return materialuse;
	}
	
	public HashSet<LaborCrew> getLaborUse()
	{
		return laboruse;
	}
	
	public void setMaterialUse(int amt, MaterialType material)
	{
		materialuse.put(material, amt);
		materialinfo.put(material, new MaterialInfo(material, amt*duration));
	}
	
	public MaterialInfo getMaterialInfo(MaterialType t){
		return materialinfo.get(t);
	}
	
	public void addLaborUse(LaborCrew crew)
	{
		laboruse.add(crew);
	}
	
	public void removeLaborUse(LaborCrew crew)
	{
		laboruse.remove(crew);
	}
	
	public CSIDivision getCSIDivision(){
		return div;
	}
	
	public Responsibility getResponsibility(){
		return respons;
	}
	
	public double getRate()
	{
		return computeDailyCost();
	}
	
	public double getTotal()
	{
		return computeDailyCost() * duration;
	}
	
	public double getTotalMaterial()
	{
		return computeDailyMaterialCost() * duration;
	}
	
	public double getTotalLabor()
	{
		return computeDailyLaborCost() * duration;
	}
	
	public double computeDailyCost()
	{
		return computeDailyMaterialCost() * (AgentM.overhead + 1) + computeDailyLaborCost();
	}
	
	public double computeDailyLaborCost()
	{
		double cost = 0.0;
		
		for(LaborCrew c : laboruse)
			cost += c.getDailyCost();
		
		return cost;
	}
	
	public double computeDailyMaterialCost()
	{
		double cost = 0.0;
		
		Iterator<Entry<MaterialType, Integer>> i = materialuse.entrySet().iterator();
		while(i.hasNext())
		{
			Entry<MaterialType, Integer> e = i.next();
			cost += e.getValue() * e.getKey().getCost();
		}
		
		return cost;
	}
	
	public String toString()
	{
		return getLabel();
	}

	public int compareTo(Object o)
	{
		if(!(o instanceof Activity))
			return 0;
		Activity a = (Activity)o;
		
		return getID() - a.getID();
	}

	public HashSet<Integer> getDrivingMaterials()
	{
		return drivingMaterials;
	}
}
