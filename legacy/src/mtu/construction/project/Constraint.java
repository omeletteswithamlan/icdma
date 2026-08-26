package mtu.construction.project;

import java.io.Serializable;

public class Constraint implements Serializable
{
	int id;
	
	private Activity from = null;
	private Activity to = null;
	
	private int length;
	private boolean soft;
	
	public Constraint(int id, Activity from, Activity to, int len, boolean c_soft)
	{
		this.id = id;
		length = len;
		soft = c_soft;
		
		setActivities(from, to);
	}
	
	public void fixSerialize(Activity[] act)
	{
		for(Activity a : act)
		{
			if(a.getID() == from.getID())
				from = a;
			if(a.getID() == to.getID())
				to = a;
		}
	}
	
	public int getID()
	{
		return id;
	}
	
	public void setActivities(Activity fromAct, Activity toAct)
	{
		if(from != null)
			from.links.remove(this);
		if(to != null)
			to.backlinks.remove(this);
		
		from = fromAct;
		to = toAct;
		
		from.links.add(this);
		to.backlinks.add(this);
		
		//make sure the plan start times get updated
		from.setStart(from.getStart());
	}
	
	public int getDuration()
	{
		return length;
	}
	
	public boolean isHardConstraint()
	{
		return !soft;
	}
	
	public Activity getFrom()
	{
		return from;
	}
	
	public Activity getTo()
	{
		return to;
	}
}
