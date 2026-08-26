package mtu.construction.tonae;

import java.io.Serializable;

/**
 * @author  Matt Watkins
 * @author  Ryan Anderson
 */
public class ANode extends Node implements Serializable
{
	public ANode()
	{
		super();
	}

	public ANode(ANode sourceNode)
	{
		// Call the superclass constructor
		super();

		// Set the early occurence time
		setEarlyStart(sourceNode.getEarlyStart());

		// Set the resolution time
		setTimeOfResolution(sourceNode.getTimeOfResolution());

		// Set the parent activity
		setParentAct(sourceNode.getParentAct());

		// Set the label
		setLabel(sourceNode.getLabel());
	}
}
