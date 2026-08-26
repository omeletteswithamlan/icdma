package mtu.construction.variable;

import java.io.Serializable;
import java.util.HashSet;

import mtu.construction.project.MaterialType;

/************************
 * Continuous Variable
 * @author Matt Watkins
 *
 */
public class ContinV extends Variable implements Serializable
{
	protected double state;
	protected double defstate;
	
	/*****************
	 * Builds a continuous variable
	 * @param id		variable id (from database)
	 * @param label		label for variable
	 * @param global	global indicator
	 * @param state		state of the variable
	 */
	public ContinV(int id, String label, boolean global, double state)
	{
		super(label, global, id);
		
		this.state = state;
		this.defstate = state;
	}
	
	private ContinV(int id, String label, boolean global, double state, HashSet<MaterialType> mat) //add your list to this constructor
	{
		this(id, label, global, state);
		matassoc = (HashSet<MaterialType>)mat.clone();
		//corey, here, clone the list of laborcrewid/laborid pairs
	}
	
	public ContinV clone()
	{
		ContinV var = new ContinV(id, label, global, state, matassoc);
		var.timespan = timespan;
		var.defstate = defstate;
		return var;
	}
	
	/*****************
	 * Get the state of the variable
	 * @return		state
	 */
	public double getState()
	{
		return state;
	}
	
	/****************
	 * Get the state as a string
	 */
	public String getStringState()
	{
		return "" + getState();
	}
	
	/****************************
	 * Sets the state for time units of time, before it reverts to the default state
	 * @param d			new state
	 * @param time		length of time
	 */
	public void setState(double d, int time)
	{
		state = d;
		this.timespan = time;
	}
	
	/***************************
	 * revert the state to the default state
	 */
	protected void revert()
	{
		state = defstate;
	}
	
	/***************************
	 * Gets a string representation of this variable
	 */
	public String toString()
	{
		return "ContinV[" + label + ", " + global + ", " + timespan + ", " + state + "]";
	}
}
