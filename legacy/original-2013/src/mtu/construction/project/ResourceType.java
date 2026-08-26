package mtu.construction.project;

import java.io.Serializable;
/**
 * Contains information about resources used in a construction project.
 * 
 * @author   Matt Watkins
 */
public class ResourceType implements Serializable
{
	//identifier for this material type
	protected int id;
	
	//description of this material type
	protected String description;
	
	//cost per unit for this material type
	protected double unitcost;
	
	protected Media[] media;
	
	//in order to make sure that set objects will store multiple copies of this object as the same object, we will keep a hash code
	//for the object from its initial creation.
	private int hash;
	
	/**
	 * Creates a new resource (material, labor) type
	 * 
	 * @param r_id   unique identifier for this resource type
	 * @param descr  description of this resource type. May be null
	 * @param cost   cost per unit of this resource type
	 */
	public ResourceType(int r_id, String descr, double cost)
	{
		id = r_id;
		description = descr;
		unitcost = cost;
		
		hash = id ^ description.hashCode() ^ (new Double(unitcost)).hashCode();
	}
	
	public int hashCode()
	{
		return hash;
	}
	
	
	public Media[] getMedia()
	{
		return media;
	}
	
	public void setMedia(Media[] media)
	{
		this.media = media;
	}
	
	
	/**
	 * Returns the unique identifier for this resource type
	 * 
	 * @return identifier for this resource type
	 */
	public int getID()
	{
		return id;
	}
	
	/**
	 * Returns the description of this resource type
	 * 
	 * @return description of this resource type
	 */
	public String getDescription()
	{
		return description;
	}
	
	/**
	 * Returns the unit cost of this resource type
	 * 
	 * @return cost per unit of this resource type
	 */
	public double getCost()
	{
		return unitcost;
	}

	public boolean equals(Object obj)
	{
		if(obj instanceof ResourceType)
		{
			ResourceType r = (ResourceType)obj;
			return (r.id == id && r.unitcost == unitcost);
		}
		return false;
	}
}
