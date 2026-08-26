package mtu.construction.gui.old;

import mtu.construction.project.LaborType;

public class LaborContainer
{
	private LaborType labtype;
	
	public LaborContainer(LaborType t)
	{
		labtype = t;
	}
	
	public LaborType getType()
	{
		return labtype;
	}
	
	public  String toString()
	{
		return labtype.getDescription();
	}
}