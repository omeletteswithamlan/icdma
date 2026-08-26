package mtu.construction.project;

import mtu.construction.interpreter.PlanInterpreter;

import java.io.Serializable;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import mtu.construction.variable.Rule;
import mtu.construction.variable.Variable;

public class Project implements Serializable
{
	//a unique identifier for this plan
	private int projectid;
	
	//the name of the project
	private String name;
	
	//the date of the project
	private GregorianCalendar date;
	
	//a description of the project
	private String description;
	
	//the amount of space for this project for storing materials
	private int space;
	
	//the penalty for storing materials off site 10% by default
	private double overstockPenalty= .9;
	
	//the percentage approximate for over head costs
	private double overhead;
	
	//types of labor used in this project
	private LaborType[] labortypes;
	
	//types of materials used in this project
	private MaterialType[] materialtypes;
	
	//activities scheduled for this project
	private Activity[] activities;
	
	//types of csi divisions used in this project
	private CSIDivision[] csidivisions;
	
	//types of responsibilities in this project
	private Responsibility[] responsibilities;
	
	//constraints for the activities
	private Constraint[] constraints;
	
	private Variable[] variables;
	
	private Rule[] rules;
	
	private LaborCrew[] crews;
	
	private Media[] media;
	
	private Skill skill;
	
	private TimeFrame timeframe;
	
	public Project(int projectid, String name, GregorianCalendar date, String description, int space, double overstockPenalty, double overhead, TimeFrame timeframe)
	{
		this.projectid = projectid;
		this.name = name;
		this.date =date;
		this.description = description;
		this.space = space;
		this.overstockPenalty = overstockPenalty;
		this.overhead =overhead;
		this.timeframe = timeframe;
	}
	
	public TimeFrame getTimeFrame()
	{
		return timeframe;
	}
	
	public void fixSerialize()
	{
		//System.out.println("fixing the damage done by serialize...");
		
		for(LaborCrew c : crews)
			c.fixSerialize(labortypes);
		
		for(Constraint c : constraints)
			c.fixSerialize(activities);
		
		for(Activity a : activities)
			a.fixSerialize(responsibilities, csidivisions, constraints, crews, materialtypes);
	}
	
	public GregorianCalendar getDate()
	{
		return date;
	}
	
	public void setDate (GregorianCalendar date)
	{
		this.date = date;
	}
	
	public double getOverstockPenalty()
	{
		return overstockPenalty;
	}
	
	public void setOverstockPenalty(double op)
	{
		overstockPenalty=op;
	}
	
	public double getOverhead()
	{
		return overhead;
	}
	
	public void setOverhead(double overhead)
	{
		this.overhead = overhead;
	}
	
	public Skill getSkill()
	{
		return skill;
	}
	
	public void setSkill(Skill skill)
	{
		this.skill = skill;
	}
	
	
	public Media[] getMedia()
	{
		return media;
	}
	
	public void setMedia(Media[] media)
	{
		this.media = media;
	}
	
	
	public void setLaborTypes(LaborType[] types)
	{
		labortypes = types;
	}
	
	public void setMaterialTypes(MaterialType[] types)
	{
		materialtypes = types;
	}
	
	public void setVariables(Variable[] var)
	{
		variables = var;
	}
	
	public void setActivities(Activity[] act)
	{
		activities = act;
	}

	public void setRules(Rule[] r)
	{
		rules = r;
	}
	
	public void setLaborCrews(LaborCrew[] c)
	{
		crews = c;
	}
	
	public void setCSIDivisions(CSIDivision[] div)
	{
		csidivisions = div;
	}
	
	public void setResponsibilities(Responsibility[] resp)
	{
		responsibilities = resp;
	}
	
	public void setConstraints(Constraint[] con)
	{
		constraints = con;
	}
	
	/**
	 * Queries the interpreter for material and labor use by this activity, and
	 * associates them with this activity object
	 * 
	 * @param activity
	 * @param interpreter
	 */
	public void addResourceUsage(Activity activity, PlanInterpreter interpreter) throws PlanException
	{
		HashMap<MaterialType, Integer> matuse = interpreter.getMaterialUse(materialtypes, activity);
		
		Iterator<Entry<MaterialType, Integer>> matiter = matuse.entrySet().iterator();
		while(matiter.hasNext())
		{
			Entry<MaterialType, Integer> e = matiter.next();
			
			activity.setMaterialUse(e.getValue(), e.getKey());
		}
		
		for(LaborCrew c : interpreter.getLaborCrewUse(crews, activity))
			activity.addLaborUse(c);
	}
	
	public LaborCrew[] getLaborCrews()
	{
		return crews;
	}
	
	/**
	 * Returns all the different types of labor used in this project
	 * 
	 * @return an array of LaborType objects to represent the labor types used in this project
	 */
	public LaborType[] getLaborTypes()
	{
		return labortypes;
	}
	
	/**
	 * Returns all the different types of materials used in this project
	 * 
	 * @return an array of MaterialType objects to represent the material types used in this project
	 */
	public MaterialType[] getMaterialTypes()
	{
		return materialtypes;
	}
	
	/**
	 * Returns all the different activities which are planned for this project
	 * 
	 * @return an array of Activity objects for the activities in this project
	 */
	public Activity[] getActivities()
	{
		return activities;
	}

	public Rule[] getRules()
	{
		return rules;
	}
	
	/**
	 * Returns all the different types of CSI Divisions used in this project
	 * 
	 * @return an array of CSIdivision objects to represent the CSI divisions used in this project
	 */
	public CSIDivision[] getCSIDivisions()
	{
		return csidivisions;
	}
	
	/**
	 * Returns all the different types of responsibilities in this project
	 * 
	 * @return an array of Responsibility objects to represent the sub-contractors used in this project
	 */
	public Responsibility[] getResponsibilities()
	{
		return responsibilities;
	}
	
	/**
	 * Returns a list of constraints that this plan has
	 * 
	 * @return a list of constraints
	 */
	public Constraint[] getConstraints()
	{
		return constraints;
	}
	
	public Variable[] getVariables()
	{
		return variables;
	}
	
	public int getID()
	{
		return projectid;
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getDescription()
	{
		return description;
	}
	
	public int getSpace()
	{
		return space;
	}
}
