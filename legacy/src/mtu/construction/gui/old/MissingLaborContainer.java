package mtu.construction.gui.old;

import mtu.construction.project.LaborCrew;
import mtu.construction.project.LaborType;


public class MissingLaborContainer
{
	private LaborCrew crew;
	private LaborType type;
	public LaborCrew getCrew() {
		return crew;
	}
	public void setCrew(LaborCrew crew) {
		this.crew = crew;
	}
	public LaborType getType() {
		return type;
	}
	public void setType(LaborType type) {
		this.type = type;
	}
	public MissingLaborContainer(LaborCrew crew, LaborType type) {
		super();
		this.crew = crew;
		this.type = type;
	}

}
