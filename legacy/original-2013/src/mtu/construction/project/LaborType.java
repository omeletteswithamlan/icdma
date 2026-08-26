package mtu.construction.project;

import mtu.construction.gui.old.Graphable;

import java.io.Serializable;

/**
 * Contains information about labor types used in a construction project.
 * 
 * @author   Matt Watkins
 */
public class LaborType extends ResourceType implements Comparable<LaborType>, Serializable, Graphable
{
	public LaborType(int l_id, String descr, double cost)
	{
		super(l_id, descr, cost);
	}
	
	public boolean equals(Object obj)
	{
		if(obj instanceof LaborType)
			return super.equals(obj);
		return false;
	}

	public int compareTo(LaborType o)
	{
		return id - o.id;
	}
	
	public String toString()
	{
		return description;
	}
	
	public String getLabel(){
		return description;
	}
}
