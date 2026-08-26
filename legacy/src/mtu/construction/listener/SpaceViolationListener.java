package mtu.construction.listener;

import java.util.HashMap;
import java.util.Vector;

import mtu.construction.project.MaterialType;
import mtu.construction.tonae.ResourceAllocation;

public interface SpaceViolationListener
{
	public void spaceViolation(double spaceOccupied, double spaceAllowed, double deliveryspace, HashMap<MaterialType, Integer> delivery, Vector<ResourceAllocation> request);
}
