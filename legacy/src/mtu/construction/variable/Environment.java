package mtu.construction.variable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import mtu.construction.project.Activity;
import mtu.construction.project.MaterialType;
import mtu.construction.project.Project;
import mtu.construction.project.TONAE;
import mtu.construction.tonae.ANode;

/*****************************
 * Environment is a collection of all variables, continuous and discrete, global and local.
 * 
 * @author Matt Watkins
 *
 */
public class Environment implements Serializable
{
	protected HashMap<Activity, HashSet<DiscreteV>> discreteset;
	protected HashMap<Activity, HashSet<ContinV>> continset;
	
	protected HashSet<DiscreteV> globaldiscrete;
	protected HashSet<ContinV> globalcontin;
	
	/*********************
	 * Builds a new environment with one context for global + one context for each activity in activities
	 * @param activities		list of contexts
	 */
	public Environment(Activity[] activities)
	{
		discreteset = new HashMap<Activity, HashSet<DiscreteV>>();
		continset = new HashMap<Activity, HashSet<ContinV>>();
		
		globaldiscrete = new HashSet<DiscreteV>();
		globalcontin = new HashSet<ContinV>();
		
		for(Activity a : activities)
		{
			discreteset.put(a, new HashSet<DiscreteV>());
			continset.put(a, new HashSet<ContinV>());
		}
	}
	
	/**********************
	 * Copy constructor for the environment
	 * 
	 * @param e		environment
	 */
	protected Environment(Environment e)
	{
		discreteset = new HashMap<Activity, HashSet<DiscreteV>>();
		continset = new HashMap<Activity, HashSet<ContinV>>();
		
		globaldiscrete = new HashSet<DiscreteV>();
		globalcontin = new HashSet<ContinV>();
		
		for(DiscreteV var : e.globaldiscrete)
			globaldiscrete.add(var.clone());
		for(ContinV var : e.globalcontin)
			globalcontin.add(var.clone());
		
		for(Activity a : e.discreteset.keySet())
		{
			discreteset.put(a, new HashSet<DiscreteV>());
			for(DiscreteV var : e.discreteset.get(a))
				discreteset.get(a).add(var.clone());
			continset.put(a, new HashSet<ContinV>());
			for(ContinV var : e.continset.get(a))
				continset.get(a).add(var.clone());
		}
	}
	
	/**************
	 * Copies this environment
	 * 
	 *  -corey never utilized, unimplement feature? -4/21/2009
	 */
	public Environment clone()
	{
		return new Environment(this);
	}
	
	/***********************
	 * Adds a variable to the environment, one for each activity if it is local
	 * @param var			variable to add
	 * @param activities	activity list for contexts
	 */
	public void addVariable(Variable var, Activity[] activities)
	{
		if(var instanceof DiscreteV)
			addVariable((DiscreteV)var, activities);
		else if(var instanceof ContinV)
			addVariable((ContinV)var, activities);
		else
			throw new Error("Internal error 1902389");
	}
	
	/*****************************
	 * adds a discrete varaible to the environment
	 * @param var				discrete variable to add
	 * @param activities		list of activities for contexts
	 */
	public void addVariable(DiscreteV var, Activity[] activities)
	{
		if(var.isGlobal())
			globaldiscrete.add(var.clone());
		else
		{
			for(Activity a : activities)
				discreteset.get(a).add(var.clone());
		}
	}
	
	/*******************************
	 * Adds a continuous variable to the environment
	 * 
	 * @param var			continuous variable to add
	 * @param activities	list of activities for contexts
	 */
	public void addVariable(ContinV var, Activity[] activities)
	{
		if(var.isGlobal())
			globalcontin.add(var.clone());
		else
		{
			for(Activity a : activities)
				continset.get(a).add(var.clone());
		}
	}
	
	/*************************
	 * Get the set of discrete variables in the environment with context a
	 * @param a		context
	 * @return		set of discrete variables
	 */
	public HashSet<DiscreteV> getDiscreteVariables(Activity a)
	{
		HashSet<DiscreteV> retval = new HashSet<DiscreteV>();
		retval.addAll(discreteset.get(a));
		return retval;
	}
	
	/**************************
	 * Get the set of continuous variables in the environment with context a
	 * @param a		context
	 * @return		set of continuous variables
	 */
	public HashSet<ContinV> getContinuousVariables(Activity a)
	{
		HashSet<ContinV> retval = new HashSet<ContinV>();
		retval.addAll(continset.get(a));
		return retval;
	}
	
	/***************************
	 * Gets a continuous variable from a context
	 * @param a		activity context
	 * @param s		label of the variable
	 * @return		the variable
	 */
	public ContinV getContinuousVariable(Activity a, String s)
	{
		for(ContinV v : continset.get(a))
		{
			if(v.getLabel().equals(s))
				return v;
		}
		
		for(ContinV v : globalcontin)
		{
			if(v.getLabel().equals(s))
				return v;
		}
		
		throw new Error("Failed to find variable");
	}
	
	/*******************************
	 * Get the set of all variables for a context
	 * @param a		activity context
	 * @return		set of variables
	 */
	public HashSet<Variable> getVariables(Activity a)
	{
		HashSet<Variable> retval = new HashSet<Variable>();
		
		retval.addAll(getDiscreteVariables(a));
		retval.addAll(getContinuousVariables(a));
		
		return retval;
	}
	
	/******************************
	 * Gets all the global discrete variables
	 * @return	set of global discrete variables
	 */
	public HashSet<DiscreteV> getGlobalDiscreteVariables()
	{
		HashSet<DiscreteV> retval = new HashSet<DiscreteV>();
		retval.addAll(globaldiscrete);
		return retval;
	}
	
	/*********************************
	 * Gets all the global continuous variables
	 * @return		set of global continuous variables
	 */
	public HashSet<ContinV> getGlobalContinuousVariables()
	{
		HashSet<ContinV> retval = new HashSet<ContinV>();
		retval.addAll(globalcontin);
		return retval;
	}
	
	/**********************************
	 * Gets all the global variables
	 * @return		set of all global variables
	 */
	public HashSet<Variable> getGlobalVariables()
	{
		HashSet<Variable> retval = new HashSet<Variable>();
		retval.addAll(getGlobalDiscreteVariables());
		retval.addAll(getGlobalContinuousVariables());
		
		return retval;
	}
	
	/**************************
	 * Gets all discrete variables
	 * @return		set of all discrete variables
	 */
	public HashSet<DiscreteV> getDiscreteVariables()
	{
		HashSet<DiscreteV> retval = new HashSet<DiscreteV>();
		
		for(Entry<Activity, HashSet<DiscreteV>> e : discreteset.entrySet())
			retval.addAll(e.getValue());
		
		retval.addAll(globaldiscrete);
		
		return retval;
	}
	
	/*******************
	 * gets all continuous variables
	 * 
	 * @return	set of all continuous variables
	 */
	public HashSet<ContinV> getContinVariables()
	{
		HashSet<ContinV> retval = new HashSet<ContinV>();
		
		for(Entry<Activity, HashSet<ContinV>> e : continset.entrySet())
			retval.addAll(e.getValue());
		
		retval.addAll(globalcontin);
		
		return retval;
	}
	
	/****************************
	 * Gets the set of all variables
	 * @return		all variables
	 */
	public HashSet<Variable> getVariables()
	{
		HashSet<Variable> retval = new HashSet<Variable>();
		
		retval.addAll(getDiscreteVariables());
		retval.addAll(getContinVariables());
		
		return retval;
	}
	
	/************************
	 * Gets a variable identified by name
	 * @param name		label for the variable
	 * @return			the variable, or null if it doesn't exist
	 */
	public Variable getGlobalVariable(String name)
	{
		for(DiscreteV var : globaldiscrete)
		{
			if(var.label.equals(name))
				return var;
		}
		
		for(ContinV var : globalcontin)
		{
			if(var.label.equals(name))
				return var;
		}
		
		return null;
	}
	
	/**************************************
	 * Gets a variable identified by name from an activity context
	 * @param a			context
	 * @param name		label
	 * @return			the variable, or null if it doesn't exist
	 */
	public Variable getVariable(Activity a, String name)
	{
		for(DiscreteV var : discreteset.get(a))
		{
			if(var.label.equals(name))
				return var;
		}
		
		for(ContinV var : continset.get(a))
		{
			if(var.label.equals(name))
				return var;
		}
		
		return getGlobalVariable(name);
	}
	
	/***************
	 * Prints the global variables. You probably shouldn't be using this, since it is debug
	 * spam. Yes, that means you. I'm watching you, don't you dare use this function....
	 *
	 */
	public void printGlobal()
	{
		System.out.println("------------ Environment ---------------");
		HashSet<Variable> vars = getGlobalVariables();
		for(Variable var : vars)
			System.out.println(var);
		System.out.println("------------ End Environment -----------");
	}
	
	/***********************
	 * Updates the environment by one time step
	 * @param tonae			tonae
	 * @param matpurchace	material purchace (can be modified by variables)
	 */
	//public void update(TONAE tonae, HashMap<MaterialType, Integer> matpurchace)
	public void update(Project p, int currentTime, HashSet<ANode> anodes, HashMap<MaterialType, Integer> matpurchace)
	{
		//set all material variable values to 0
		for(MaterialType t : p.getMaterialTypes())
		{
			for(Variable v : getVariables())
			{
				if(v instanceof ContinV && v.hasMaterial(t))
				{
					ContinV var = (ContinV)v;
					var.setState(0, 1);
				}
			}
		}
		
		//update the variables
		for(Variable v : getVariables())
			v.update();
		
		//Update discrete local variables
		for(Entry<Activity, HashSet<DiscreteV>> e : discreteset.entrySet())
		{
			for(DiscreteV v : e.getValue())
			{
				//Set ID variable to the ID of the Activity
				if(v.getLabel().equals("ID"))
					v.setState("" + e.getKey().getRealID(), 0);

				//Set ActivityTime depending on whether activity is active
				if(v.getLabel().equals("ActivityTime"))
				{
					for(ANode node : anodes)
					{
						if(node.getOutPrimaryArc() != null && node.getParentAct().getID() == e.getKey().getID())
						{
							int start = node.getEarlyStart();
							int end = node.getOutPrimaryArc().getHeadNode().getOutPrimaryArc().getHeadNode().getEarlyStart();
							
							if(currentTime < start || currentTime > end) //set to -1 if not active
								v.setState("-1", 0);
							else //set to currentTimeStep if active
								v.setState("" + (currentTime - start) , 0);
						}
					}
				}
			}
		}
		
		//set the variables to the material values
		//If a variable has a material from the purchaseList, set it's state to the number of materials to be purchased
		if(matpurchace != null)
		{
			for(Variable v : getVariables())
			{
				for(Entry<MaterialType, Integer> e : matpurchace.entrySet())
				{
					if(v.hasMaterial(e.getKey()) && v instanceof ContinV)
					{
						ContinV var = (ContinV)v;
						var.setState(e.getValue(), 0);
					}
				}
			}
		}
	}
	
	/*************************
	 * Updates the special values which are mapped through the database. This should probably be private
	 * @param tonae				tonae
	 * @param matpurchace		material purchace list
	 */
	//public void updateValues(TONAE tonae, HashMap<MaterialType, Integer> matpurchace)
	public void updateValues(HashMap<MaterialType, Integer> matpurchace)
	{
		//if variable has a material in purchaseList, add the amount from the variable into purchaseList
		if(matpurchace != null)
		{
			for(Variable v : getVariables())
			{
				for(Entry<MaterialType, Integer> e : ((HashMap<MaterialType, Integer>)matpurchace.clone()).entrySet())
				{
					if(v.hasMaterial(e.getKey()) && v instanceof ContinV)
					{
						ContinV var = (ContinV)v;
						matpurchace.put(e.getKey(), (int)var.getState());
					}
				}
			}
		}
		
		//tonae.updateBaselineLaborRequirements(c, l, amount)
	}
}
