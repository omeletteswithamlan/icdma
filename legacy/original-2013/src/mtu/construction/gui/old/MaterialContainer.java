package mtu.construction.gui.old;

import mtu.construction.project.MaterialType;

public class MaterialContainer
{
	private MaterialType mattype;
	private double quant;
	private Object obj;
	
	public MaterialContainer(MaterialType t)
	{
		this(t, 0.0);
	}
	
	public MaterialContainer(MaterialType t, double quant)
	{
		this(t, quant, null);
	}
	
	public MaterialContainer(MaterialType t, double quant, Object obj)
	{
		mattype = t;
		this.quant = quant;
		this.obj = obj;
	}
	
	public MaterialType getType()
	{
		return mattype;
	}
	
	public double getQuantity()
	{
		return quant;
	}
	
	public Object getObj()
	{
		return obj;
	}
	
	public  String toString()
	{
		return mattype.getDescription();
	}
}