package mtu.construction.tonae;

import java.io.Serializable;
import java.util.HashSet;

import mtu.construction.project.Activity;

/**
 * A primary arc is an arc which links the beginning node of an activity
 * to the ending node of an activity, specifying the length of the activity
 * 
 * The end of an activity is specified by the fact that the out_primaryArc is null
 * 
 * @author  Ryan Anderson
 * @author  Matt Watkins
 * @author  Corey Tebo
 */
public class Node implements Serializable
{
	/**Outgoing primary arc. NULL if the A-Node ends an activity*/
	private Arc out_primaryArc;
	/** Incoming primary arc. NULL if the A-Node begins an activity*/
	private Arc in_primaryArc;

	/** Set of outgoing arcs. These are non-primary constraints on other nodes*/
	private HashSet<Arc> out_constraintArcs;

	/** Set of incoming arcs. These are non-primary constraints on other nodes*/
	private HashSet<Arc> in_constraintArcs;


	/** Early start time for an activity*/
	private int earlyStart;

	/** Time of node resolution. -1 indicates the node is not resolved yet*/
	private int timeOfResoluton;

	/** Reference to parent activity*/
	private Activity parentActivity;

	/** Label of this Node*/
	private String label;

	/**
	 * makes a node with no Arc links, empty constraints and an early start impossible to satisfy 
	 */
	public Node()
	{
		out_primaryArc = null;
		in_primaryArc = null;

		out_constraintArcs = new HashSet<Arc>();
		in_constraintArcs = new HashSet<Arc>();

		earlyStart = -1;
		timeOfResoluton = -1;

		parentActivity = null;
	}

	// Assignment functions
	public void setOutPrimaryArc(Arc out)
	{
		out_primaryArc = out;
	}
	
	public void setInPrimaryArc(Arc in)
	{
		in_primaryArc = in;
	}
	
	public void setEarlyStart(int p_early)
	{
		earlyStart = p_early;
	}
	
	public void setTimeOfResolution(int p_resolved)
	{
		timeOfResoluton = p_resolved;
	}
	
	public void setParentAct(Activity act)
	{
		parentActivity = act;
	}

	// Accessor functions
	public Arc getOutPrimaryArc()
	{
		return out_primaryArc;
	}
	
	public Arc getInPrimaryArc()
	{
		return in_primaryArc;
	}

	public void addOutConstraint(Arc constraint) 
	{ 
		out_constraintArcs.add(constraint); 
	}
	
	public HashSet<Arc> getOutConstraints()
	{
		return out_constraintArcs;
	}

	public void addInConstraint(Arc constraint) 
	{
		in_constraintArcs.add(constraint);
	}
	
	public HashSet<Arc> getInConstraints()
	{
		return in_constraintArcs;
	}

	public int getEarlyStart()
	{
		return earlyStart;
	}
	
	public int getTimeOfResolution()
	{
		return timeOfResoluton;
	}
	
	public Activity getParentAct()
	{
		return parentActivity;
	}

	public void setLabel(String lab)
	{
		label = lab;
	}
	
	public String getLabel()
	{
		return label;
	}
}
