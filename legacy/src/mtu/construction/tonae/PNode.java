package mtu.construction.tonae;


import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import mtu.construction.project.Activity;
import mtu.construction.project.MaterialType;

/**
 * @author  Ryan Anderson
 * @author  Matt Watkins
 */
public class PNode extends Node implements Serializable
{
	private boolean isActive;

	private HashSet<Arc> events;

	private boolean isGlobal;
	
	protected double total_percent_ordered; //total percent of materials ordered for this activity
	protected double total_work; //total amount of work of this activity...
	protected double total_work_left; //At beginning of activity this is no_of_cycles * work/cycle (typically cycle=week)(unit:$$)
	
	// Put some event containing stuff in here
	public PNode(PNode pNode)
	{
		// Copy the early occurence time
		setEarlyStart(pNode.getEarlyStart());

		// Copy the resolved time
		setTimeOfResolution(pNode.getTimeOfResolution());

		// Copy the reference to the parent activity
		setParentAct(pNode.getParentAct());

		// Copy the label
		setLabel(pNode.getLabel());

		// Set the active and delay variables
		isActive = pNode.getActive();;

		// Set the global variable
		isGlobal = pNode.getGlobal();

		events = new HashSet<Arc>();

		// Copy the event arcs
		copyEventArcs(pNode);
		
		total_work_left = pNode.total_work_left;
	}
	
	public PNode()
	{
		super();
		isActive = false;
		isGlobal = false;

		events = new HashSet<Arc>();
	}
	
	public void setParentAct(Activity a)
	{
		super.setParentAct(a);
		
		//a will be null if this is the global p-node
		if(a != null){
			setTotalWorkLeft(a.getTotalMaterial());
			total_work = total_work_left;
		}
	}
	
	public void setTotalWorkLeft(double d)
	{
		total_work_left = d;
	}
	
	public double getTotalWorkLeft()
	{
		return total_work_left;
	}
	
	public boolean isFirstDay()
	{
		return getEarlyStart() == getStart();
	}
	
	public boolean isLastDay()
	{
		return getEarlyStart() == getEnd();
	}
	
	//gets the as-built start day of the project
	public int getStart()
	{
		return this.getInPrimaryArc().getTailNode().getEarlyStart();
	}
	
	//gets the as-built end day of the project
	public int getEnd()
	{
		return this.getOutPrimaryArc().getHeadNode().getEarlyStart();
	}
	
	public int getDuration()
	{
		return getEnd() - getStart();
	}
	
	public void advance()
	{
		setEarlyStart(getEarlyStart() + 1);
	}

	public void setActive(boolean active)
	{
		isActive = active;
	}
	
	public boolean getActive()
	{
		return isActive;
	}

	// Methods for managing events
	public void addEvent(Arc arc)
	{
		events.add(arc);
	}

	public HashSet<Arc> getEvents()
	{
		return events;
	}

	// Methods for getting and setting the global status of this node
	public void setGlobal(boolean globe)
	{
		isGlobal = globe;
	}
	
	public boolean getGlobal()
	{
		return isGlobal;
	}

	private void copyEventArcs(PNode pNode)
	{
		Iterator<Arc> i_sourceArcs = pNode.getEvents().iterator();

		Arc currentSource;

		while (i_sourceArcs.hasNext())
		{
			currentSource = i_sourceArcs.next();

			Arc currentCopy = new Arc(currentSource);

			currentCopy.setHeadNode(this);
			currentCopy.setTailNode(currentSource.getTailNode());

			addEvent(currentCopy);
		}
	}
	
	//Return the percentage of work that has been done on this project so far
	public int getPercentCompletion(){
		return 100 - (int)(total_work_left/total_work * 100);
	}
	
	//Set the percent of total materials ordered
	public void setOrdered(double d){
		//if(d >= total_percent_ordered)
			total_percent_ordered = d;
		//else
		//	throw new Error("Bad Input: PNode.setOrdered: "+d+" < "+total_percent_ordered);
	}
	
	//Get the percent of total materials ordered
	public double getPercentOrdered(){
		return total_percent_ordered;
	}
	
	public int getOrderedAmount(MaterialType t){
		int amount = getParentAct().getMaterialUse().get(t);
		amount *= getParentAct().getDuration();
		return (int)(amount * total_percent_ordered/100);
	}
}
