package mtu.construction.listener;

import mtu.construction.project.LaborCrew;

public interface LaborChangeListener
{
	public void laborChanged(LaborCrew[] c, LaborCrew u, LaborCrew h);
}
