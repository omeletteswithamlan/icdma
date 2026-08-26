package mtu.construction.variable;

import java.io.Serializable;

/**********************
 * Discrete Variable class
 * @author Matt Watkins
 */
public class DiscreteV extends Variable implements Serializable
{
	protected String state;
	protected String defstate;
	
	/*********************
	 * Builds a new discrete variable
	 * @param id		id of the variable (From the database)
	 * @param label		label for this variable (name)
	 * @param global	global indicator
	 * @param state		state of the variable
	 */
	public DiscreteV(int id, String label, boolean global, String state)
	{
		super(label, global, id);
		
		this.state = state;
		this.defstate = state;
	}
	
	/******************
	 * Copy this discrete variable
	 */
	public DiscreteV clone()
	{
		DiscreteV var = new DiscreteV(id, label, global, state);
		var.timespan = timespan;
		var.defstate = defstate;
		return var;
	}
	
	/**********
	 * Get the state of the variable
	 * @return	state
	 */
	public String getState()
	{
		return state;
	}
	
	/*************************
	 * Get the state of the variable as a string
	 */
	public String getStringState()
	{
		return "" + getState();
	}
	
	/****************************
	 * Set the state of the variable for time units of time
	 * @param d			new state
	 * @param time		length of time
	 */
	public void setState(String d, int time)
	{
		state = d;
		if(time == 0)
			defstate = d;
		this.timespan = time;
	}

	/********************
	 * Revert the state of the variable to the default state
	 */
	protected void revert()
	{
		state = defstate;
	}
	
	/********************************
	 * String representation of the variable
	 */
	public String toString()
	{
		return "DiscreteV[" + label + ", " + global + ", " + timespan + ", " + state + "]";
	}
}