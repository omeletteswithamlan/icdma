package mtu.construction.tonae;

import java.io.Serializable;

/**
 * 
 * @author  Ryan Anderson
 */
public class Arc implements Serializable
{
	// Reference to tail end node
	private Node tailNode;

	// Reference to head end node
	private Node headNode;

	// Upper and lower bounds of constraint, such that 
	// lower <= headNode - tailNode <= upper
	private int lower;
	private int upper;

	// Threshold of hardness for this arc
	private int threshold;

	// Label of the arc
	private String label;

	// Penalty information of this arc
	private int penIndex;
	private int penBase;
	private double penRate;

	public Arc(Arc pArc)
	{

		lower = pArc.getLower();
		upper = pArc.getUpper();

		threshold = pArc.getThreshold();

		label = pArc.getLabel();

		penBase = pArc.getPenaltyBase();
		penRate = pArc.getPenaltyRate();

	}

	public Arc()
	{

		tailNode = null;
		headNode = null;

		lower = -1;
		upper = -1;

		label = "";

	}

	public void setTailNode(Node node) { tailNode = node; }
	public Node getTailNode() { return tailNode; }
	public void setHeadNode(Node node) { headNode = node; }
	public Node getHeadNode() { return headNode; }

	public void setLower(Integer low) { lower = low; }
	public Integer getLower() { return lower; }

	public void setUpper(Integer upp) { upper = upp; }
	public Integer getUpper() { return upper; }

	public void setLabel(String lab) { label = lab; }
	public void setLabel() 
	{
		String prefix = tailNode.getLabel();
		String suffix = headNode.getLabel();
		label = prefix + "-Arc-" + suffix;
	}
	public String getLabel() { return label; }

	public void setThreshold(int thres) { threshold = thres; }
	public int getThreshold() { return threshold; }

	public void setPenaltyIndex(int index) {penIndex = index; } 
	public int getPenaltyIndex() { return penIndex; }

	public void setPenaltyRate(double rate) {penRate = rate; }
	public double getPenaltyRate() { return penRate; }

	public void setPenaltyBase(int base) { penBase = base; }
	public int getPenaltyBase() { return penBase; }

}

