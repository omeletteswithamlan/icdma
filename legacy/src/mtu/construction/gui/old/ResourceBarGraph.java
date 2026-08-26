package mtu.construction.gui.old;

import mtu.construction.gui.wrapper.G_Material;

import java.util.Vector;


public class ResourceBarGraph extends BarGraphPanel
{
	private static final long serialVersionUID = -4324139503012899741L;

	private Vector<Graphable> resources = new Vector<Graphable>();
	private Vector<Integer> matamt = new Vector<Integer>();
	
	public ResourceBarGraph()
	{
		super();
	}
	
	public void addMaterial(Graphable t, int i)
	{
		resources.add(t);
		matamt.add(i);
	}
	
	protected int getBarCount()
	{
		return resources.size();
	}
	
	protected int getHeight(int i)
	{
		return matamt.get(i);
	}
	
	protected String getBaseText(int i)
	{
		String s = resources.get(i).getLabel();
		return s;
	}
}
