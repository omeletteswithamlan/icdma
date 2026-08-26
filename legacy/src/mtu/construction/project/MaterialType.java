package mtu.construction.project;

import java.io.Serializable;
/**
 * Contains information about material types used in a construction project.
 * 
 * @author   Matt Watkins
 */
public class MaterialType extends ResourceType implements Comparable<MaterialType>, Serializable
{
	private double size;
	private boolean perishable;
	
	public MaterialType(int m_id, String descr, double cost, double size, boolean perishable)
	{
		super(m_id, descr, cost);
		this.size = size;
		this.perishable = perishable;
	}
	
	public boolean equals(Object obj)
	{
		if(obj instanceof MaterialType)
			return super.equals(obj);
		return false;
	}

	public int compareTo(MaterialType o)
	{
		return id - o.id;
	}
	
	public void setSize(double size)
	{
		this.size = size;
	}
	
	public double getSize()
	{
		return size;
	}
	
	public boolean getPerishable()
	{
		return perishable;
	}
}
