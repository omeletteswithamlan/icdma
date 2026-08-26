package mtu.construction.gui.old;

import mtu.construction.gui.wrapper.G_Activity;

public class ActivityContainer
{
	private G_Activity node;
	
	public ActivityContainer(G_Activity node)
	{
		this.node = node;
	}
	
	public G_Activity getNode()
	{
		return node;
	}
	
	public String toString()
	{
		if(node == null)
			return "Stock";
		else
			return node.getLabel();
	}
}