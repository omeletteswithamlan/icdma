package mtu.construction.gui.wrapper;

import java.util.Set;

import mtu.construction.project.LaborCrew;
import mtu.construction.project.LaborType;

//Wrapper class for LaborCrew
//Restrict GUI access
public class G_LaborCrew {

	private LaborCrew crew;
	
	//Constructor
	public G_LaborCrew(LaborCrew c){
		crew = c;
	}
	
	//Get the unique ID for this crew
	public int getID(){
		return crew.getID();
	}
	
	//Get Name of this labor crew
	public String getLabel(){
		return crew.getName();
	}
	
	//Get the amount of a particular LaborType in this crew
	public int getAmount(LaborType t){
		return crew.getAmt(t);
	}
	
	//Get a list of the laborTypes in this crew
	public Set<LaborType> getLaborerTypes(){
		return crew.getTypes();
	}
	
	//Returns a copy of the contained crew
	public LaborCrew getCrew(){
		return crew.clone();
	}
	
	public double compareProductivity(G_LaborCrew c){
		return c.requestCompare(crew);
	}
	
	public double requestCompare(LaborCrew c){
		return c.compareProductivity(crew);
	}
	
	//...
	public void clear(){
		crew.clear();
	}
	
	public LaborCrew unwrap(){
		return crew;
	}
}
