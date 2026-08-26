package mtu.construction.tonae;

import java.io.Serializable;
import java.util.Comparator;

public class PNodeCompare implements Comparator<PNode>, Serializable
{
	public int compare(PNode p1, PNode p2)
	{
		return p1.getLabel().compareTo(p2.getLabel());
	}

	public boolean equals(Object t1)
	{
		return t1 instanceof PNodeCompare;
	}
}
