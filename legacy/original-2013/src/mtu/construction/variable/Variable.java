package mtu.construction.variable;

import java.io.Serializable;
import java.util.HashSet;
//import java.util.Set;
//import java.util.Vector;

import mtu.construction.project.MaterialType;

/**************************
 * Class representing all variables
 * 
 * @author Matt Watkins
 *
 */
public abstract class Variable implements Serializable
{
	protected int id;
	protected String label;
	protected boolean global;
	protected int timespan;
	protected HashSet<MaterialType> matassoc;
	
	/*******************
	 * Construct a new variable
	 * @param label			label for the variable
	 * @param global		global indicator
	 * @param id			unique id (from the database)
	 */
	public Variable(String label, boolean global, int id)
	{
		this.label = label;
		this.global = global;
		this.id = id;
		this.matassoc = new HashSet<MaterialType>();
	}
	
	/**********************
	 * Set the material linked to this variable
	 * @param t		material to link
	 */
	public void setMaterial(MaterialType t)
	{
		matassoc.add(t);
	}
	
	/***************************
	 * Check to see if this variable is associated with a material
	 * @param t			material to check
	 * @return			true if it is associated, false otherwise
	 */
	public boolean hasMaterial(MaterialType t)
	{
		for(MaterialType ty : matassoc)
		{
			if(ty.getID() == t.getID())
				return true;
		}
		
		return false;
	}
	
	/*********************
	 * Checks to see if this variable is global
	 * @return		true if it is global, false otherwise
	 */
	public boolean isGlobal()
	{
		return global;
	}
	
	/*********************
	 * Gets the label for this variable
	 * @return	label
	 */
	public String getLabel()
	{
		return label;
	}
	
	/************************
	 * Gets the ID for this variable
	 * @return		id
	 */
	public int getID()
	{
		return id;
	}
	
	/***********************
	 * Update the variable by one time step
	 *
	 */
	public void update()
	{
		timespan--;
		if(timespan == 0)
			revert();
	}
	
	/**********************
	 * Get the state of the variable as a string
	 * @return	string state of the variable
	 */
	public abstract String getStringState();
	
	/*************************
	 * Revert to the default value
	 *
	 */
	protected abstract void revert();
	
	/************************
	 * clone this variable
	 */
	public abstract Variable clone();
}
