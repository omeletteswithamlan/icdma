package mtu.construction.project;

import java.io.Serializable;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

/**
 * data object with methods to compute the productivity of a labor crew relative
 * to the crew as specified in the plan. includes inefficiencies for work clustering
 * 
 * @author Mat Watkens
 * @author Corey Tebo
 *
 */
public class LaborCrew implements Serializable, Comparable<LaborCrew>
{
	private int id; //labor crew id
	private String name; //name of the labor crew
	private TreeMap<LaborType, Integer> laboramt; //labor types and associated amounts
	private double lastprod = 0; //unused. What is it for exactly!!??!? productivity?
	private double hours; //Hours...?? Hours for what hmm???

	public LaborCrew(int id, String name)
	{
		this.id = id;
		this.name = name;
		if(name == null)
			name = "Labor Crew " + id;
		laboramt = new TreeMap<LaborType, Integer>();
	}
	
	public void fixSerialize(LaborType[] types)
	{
		for(LaborType t : types)
		{
			if(laboramt.containsKey(t))
			{
				int i = laboramt.get(t);
				laboramt.remove(t);
				laboramt.put(t, i);
			}
		}
	}
	
	public double getHours()
	{
		return hours;
	}
	
	public void setHours(double d)
	{
		hours = d;
	}
	
	//get latest productivity value
	public double getLastProd()
	{
		return lastprod;
	}
	
	//set the productivity value
	public void setLastProd(double d)
	{
		lastprod = d;
	}
	
	//remove all labor from the crew
	public void clear()
	{
		laboramt.clear();
	}
	
	//copy constructor for clone()
	private LaborCrew(LaborCrew crew)
	{
		id = crew.id;
		name = crew.name;
		laboramt = (TreeMap<LaborType, Integer>)crew.laboramt.clone();
	}
	
	public LaborCrew clone()
	{
		return new LaborCrew(this);
	}
	
	/**
	 * Remove a labor type from the labor crew.
	 * 
	 * @param t - the type of labor to remove
	 * @return - the number of laborers removed. If the labor type didn't exist,
	 * 			0 is returned.
	 */
	public int remove(LaborType t)
	{
		if(getAmt(t) > 0)
		{
			laboramt.put(t, getAmt(t) - 1);
			return 1;
		}
		
		return 0;
	}
	
	/**
	 * Sets the number of laborers of a certain type to
	 * the specified value.
	 * 
	 * @param t - type of labor to set
	 * @param amt - the amount to set the labor to
	 */
	public void set(LaborType t, int amt)
	{
		if(amt != 0)
			laboramt.put(t, amt);
		else
			laboramt.remove(t);
	}
	
	/**
	 * Add a single laborer of type t to the labor crew.
	 * 
	 * @param t
	 */
	public void add(LaborType t)
	{
		add(t, getAmt(t) + 1);
	}
	
	/**
	 * Add a number of laborers of type lab to the labor crew.
	 * 
	 * @param lab
	 * @param amt - the number of laborers to add
	 */
	public void add(LaborType lab, int amt)
	{
		laboramt.put(lab, amt);
	}
	
	/**
	 * Get the labor types for this laborCrew
	 * 
	 * @return - Set of labor types in the laborCrew
	 */
	public Set<LaborType> getTypes()
	{
		return laboramt.keySet();
	}
	
	/**
	 * Get the number of laborers of type t on the labor crew.
	 * 
	 * @param t - type of labor to check
	 * @return - if the labor type exists in the labor crew, return
	 * 			number of laborers of that type. Otherwise, return 0.
	 */
	public int getAmt(LaborType t)
	{
		if(laboramt.containsKey(t))
			return laboramt.get(t);
		return 0;
	}
	
	/*********************************************
	 * The following functions contain a lot of hard coded values used for computing the
	 * productivity of one crew relative to another. Ideally the database will be able to
	 * be able to have a more expressive form of holding labor, but for now this will have 
	 * to do 
	 * Functions with hard coded values:
	 * computeMaxLaborers
	 * compareProductivity
	 */
	
	
	/**
	 *  this function assumes that any worker with the string "Foreman" in it's discription
	 *  is a forman and that each forman can handle atmost 10 workers
	 * @param c Labor crew in question
	 * @return
	 */
	private int computeMaxLaborers(LaborCrew c)
	{
		int max = 0;
		
		for(Entry<LaborType, Integer> e : c.laboramt.entrySet())
		{
			if(e.getKey().getDescription().contains("Foreman"))
				max += 10 * e.getValue();
		}
		
		return max;
	}
	
	/**
	 * This method is used to compare this Labor Crew
	 * 
	 * @param c
	 * @return
	 */
	public double compareProductivity(LaborCrew c)
	{
		double cmpwork = -1;
		
		//if a crane operator or an oiler is present, both are needed for any productivity at all. If
		//additional crane crew or oilers are present, no additional productivity is gained
		int needsCraneCrew = 0;
		int needsOilerCrew = 0;
		for(Entry<LaborType, Integer> e : laboramt.entrySet())
		{
			if(e.getKey().getDescription().contains("Crane"))
				needsCraneCrew = e.getValue();//the number of crane workers on the crew
			if(e.getKey().getDescription().contains("Oiler"))
				needsOilerCrew = e.getValue();//the number of oilers on the crew
		}
		
		int hasCraneCrew = 0;
		int hasOilerCrew = 0;
		for(Entry<LaborType, Integer> e : c.laboramt.entrySet())
		{
			if(e.getKey().getDescription().contains("Crane"))
				hasCraneCrew = e.getValue();
			if(e.getKey().getDescription().contains("Oiler"))
				hasOilerCrew = e.getValue();
		}

		int maxlaborers = computeMaxLaborers(c);
		int numlaborers = 0;
		
		for(Entry<LaborType, Integer> e : laboramt.entrySet())
		{
			String name = e.getKey().getDescription();
			
			//logic for foreman and equipment operators are handled elsewhere
			if(!(name.contains("Foreman") || name.contains("Crane") || name.contains("Oiler")))
			{
				if(maxlaborers > numlaborers)
				{
					double base = (double)e.getValue();
					double amt = (double)c.getAmt(e.getKey());
					
					double perc = amt / base;
					
					if(cmpwork == -1 || cmpwork > perc)
						cmpwork = perc;
				}
			}
		}
		
		if(cmpwork == -1)
			cmpwork = 0;
		
		if(hasCraneCrew < needsCraneCrew || hasOilerCrew < needsOilerCrew)
			cmpwork = 0;
		
		return cmpwork;
	}
	
	/**
	 * Get the cost for this labor crew (all the laborers in the crew) to work a day
	 * 
	 * @return the daily cost of this labor crew
	 */
	public double getDailyCost()
	{
		double cost = 0;
		
		for(Entry<LaborType, Integer> e : laboramt.entrySet())
			cost += e.getKey().getCost() * e.getValue();

		return cost;
	}
	
	/**
	 * Get the name of the labor crew
	 * 
	 * @return The name of the labor crew
	 */
	public String getName()
	{
		return name;
	}
	
	//get the id of this laborcrew (from database)
	public int getID()
	{
		return id;
	}
	
	//get the name of this crew (same as getName())
	public String toString()
	{
		return name;
	}

	//compares ID's to see if one crew is the same as another crew
	public boolean equals(Object o)
	{
		if(o instanceof LaborCrew)
		{
			LaborCrew l = (LaborCrew)o;
			
			return l.getID() == getID();
		}
		return false;
	}
	
	//uses ID to compare labor crews
	public int compareTo(LaborCrew o)
	{
		return o.getID() - getID();
	}
}
