package mtu.construction.variable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import mtu.construction.project.Activity;
import mtu.construction.project.Media;
import mtu.construction.project.TONAE;
import mtu.construction.listener.RuleListener;
import mtu.construction.tonae.PNode;

/*****************************
 * Represents a rule, which is a 5-tuple (name, precondition set, post condition set, probability, global
 * 
 * @author Matt Watkins
 *
 */
public class Rule implements Serializable
{
	protected String name;		//name from db
	protected String warning;	//string displayed by the gui when the rule fires. If null, nothing fires
	
	protected Condition[] precond;
	protected Condition[] postcond;
	
	protected double prob;
	
	protected int newtime;
	protected boolean global;
	
	protected int ruleID;
	
	protected Media[] media;
	
	/**********************
	 * Construct a new rule
	 * @param name			name of the rule
	 * @param warning		warning to pass to rule listener
	 * @param precond		set of preconditions
	 * @param postcond		set of post conditions
	 * @param prob			probability of occurance
	 * @param global		global indicator
	 */
	public Rule(String name, String warning,
					Condition[] precond,
					Condition[] postcond,
					double prob, boolean global, int ruleID)
	{
		this.name = name;
		this.warning = warning;
		this.precond = precond;
		this.postcond = postcond;
		this.prob = prob;
		this.global = global;
		this.ruleID = ruleID;
	}
	
	/******************
	 * Gets the name of the variable
	 * @return	name
	 */
	public String getName()
	{
		return name;
	}
	
	/***************
	 * Gets the preconditions
	 * @return	preconditions
	 */
	public Condition[] getPreConditions()
	{
		return postcond;
	}

	/***************
	 * Gets the post conditions
	 * @return postconditions
	 */
	public Condition[] getPostConditions()
	{
		return postcond;
	}
	
	/****************************
	 * String representation of this rule. Only for printing
	 */
	public String toString()
	{
		String s = "Name: " + name + " Warning: " + warning + " Preconditions: ["; 
		
		for(Condition c : precond)
		{
			s += c.toString();
		}
		
		s += "]";
		
		return s;
	}
	
	/*****************
	 * Media associated with this rule
	 * @return		media set
	 */
	public Media[] getMedia()
	{
		return media;
	}
	
	/******************
	 * Set media list
	 * @param media		new media list
	 */
	public void setMedia(Media[] media)
	{
		this.media = media;
	}
	
	/**********************
	 * Trigger this rule.
	 * This method simply notifies ruleListeners that this rule triggered.
	 * 
	 * @param listeners		rule listeners to call
	 * @param act			activity set to call rule listeners on
	 * @param o				Object specific to the type of rule this is. DEPRECATED
	 */
	public void trigger(ArrayList<RuleListener> listeners, HashSet<Activity> act, Object o)
	{
		if(TONAE.querymode)
			return;
		
		if(listeners != null)
		{
			for(RuleListener r : listeners)
			{
				if(act.size() == 0)
					r.ruleTriggered(this, null, o);
				else
				{
					for(Activity a : act)
					{
						r.ruleTriggered(this, a, o);
					}
				}
			}
		}
	}
	
	/******************
	 * Apply this rule to the environment
	 * @param e			environment
	 * @param tonae		tonae
	 * @return			activities to which this rule applied, or empty if global, or null if not applied
	 */
	public HashSet<Activity> apply(Environment e, Set<PNode> readyList)
	{
		Random random = new Random();
		HashSet<Activity> retval = new HashSet<Activity>();
		if(global)
		{
			//make sure that the random probability occurs
			if(random.nextFloat() >= prob)
				return null;
			
			//make sure the preconditions are met
			for(Condition c : precond)
			{
				if(!c.isMet(e))
					return null;
			}
			
			for(Condition c : postcond)
				c.apply(e);
			
			return retval;
		}
		else
		{
			for(PNode n : readyList)
			{
				if(apply(e, n.getParentAct()))
				{
					retval.add(n.getParentAct());
				}
			}
			
			if(retval.size() == 0)
				return null;
			
			return retval;
		}
	}
	
	/***********************
	 * Apply this rule to the environment for an activity
	 * @param e		environment
	 * @param a		activity context
	 * @return		true if the rule was applied, false otherwise
	 */
	public boolean apply(Environment e, Activity a)
	{
		Random random = new Random();
		if (!TONAE.querymode)
		{
			int b=1;
		}
		
		if(random.nextFloat() >= prob)
			return false;
		
		for(Condition c : precond)
		{
			if(!c.isMet(e, a))
				return false;
		}
		for(Condition c : postcond)
			c.apply(e, a);
		
		return true;
	}
	
	/********************
	 * Checks to see if this rule is global
	 * @return		true if its global, false otherwise
	 */
	public boolean isGlobal()
	{
		return global;
	}
	
	/**********************
	 * Gets the rule warning.
	 * @return		warning
	 */
	public String getWarning()
	{
		return warning;
	}
	
	public int getRuleID()
	{
		return ruleID;
	}
}