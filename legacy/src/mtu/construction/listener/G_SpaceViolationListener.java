package mtu.construction.listener;

import java.util.HashMap;
import java.util.Vector;

import mtu.construction.project.MaterialType;
import mtu.construction.gui.wrapper.G_ResourceAlloc;

public interface G_SpaceViolationListener {
	public void spaceViolation(double spaceOccupied, double spaceAllowed, double deliveryspace, HashMap<MaterialType, Integer> delivery, Vector<G_ResourceAlloc> request);
}
