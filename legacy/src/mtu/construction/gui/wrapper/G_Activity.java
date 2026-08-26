package mtu.construction.gui.wrapper;

import mtu.construction.icdma.Simulator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Map.Entry;

import mtu.construction.project.Activity;
import mtu.construction.project.CSIDivision;
import mtu.construction.project.LaborCrew;
import mtu.construction.project.MaterialType;
import mtu.construction.project.Media;
import mtu.construction.project.TONAE;
import mtu.construction.tonae.ANode;
import mtu.construction.tonae.PNode;
import mtu.construction.variable.Variable;

//Wrapper class for activity
//Gives Restricted access to the GUI
//TODO: null PNode check...
//TODO: add more features
//NOTE: Using second constructor (Activity) does not set PNode node, so getPercentCompletion() and isActive() will return 0 and false
public class G_Activity {

	private PNode node; //Reference to PNode for this activity
	private Activity act; //Reference to the activity
	private TONAE tonae; //Reference to TONAE for CVI, SVI, and other info
	private Simulator sim;
	
	//Constructor
	public G_Activity(PNode p, TONAE t, Simulator sim){
		tonae = t;
		this.sim = sim;
		node = p;
		act = p.getParentAct();
	}
	
	//Constructor 2
	public G_Activity(Activity a, TONAE t, Simulator sim){
		this.sim = sim;
		tonae = t;
		act = a;
	}
	
	//Get the unique ID for the activity
	public int getID(){
		return act.getID();
	}
	
	//Get the Name of the activity
	public String getLabel(){
		return act.getLabel();
	}
	
	//Get the start time (Simulation Step) for the activity
	public int getStart(){ //is this for the baseline only???
		return act.getStart();
	}
	
	public int getEarlyStart(){
		if(node == null){
			//for(PNode p : tonae.getReadyList()){
			//	if(p.getParentAct().getID() == act.getID()){
			//		node = p;
			//		return p.getInPrimaryArc().getTailNode().getEarlyStart();
			//	}
			//}
			for(ANode a : tonae.getANodeSet()){
				if(a.getParentAct().getID() == act.getID() && a.getOutPrimaryArc() != null){
					return a.getEarlyStart();
				}
			}
			return act.getStart();
		}
		else
			return node.getInPrimaryArc().getTailNode().getEarlyStart();
	}
	
	//Get the end time (Simulation Step) for the activity
	public int getEnd(){
		return act.getEnd();
	}
	
	//Get the media for the activity
	public Media[] getMedia(){
		return act.getMedia();
	}
	
	//Get total overall cost for the activity
	public double getTotal(){
		return act.getTotal();
	}
	
	//Get the total cost of labor for the activity
	public double getTotalLabor(){
		return act.getTotalLabor();
	}
	
	//Get the total cost of materials for the activity
	public double getTotalMaterial(){
		return act.getTotalMaterial();
	}
	
	//Get the CSI Division of the activity
	public CSIDivision getCSIDivision(){
		return act.getCSIDivision();
	}
	
	//Get the base labor use for this activity
	public HashSet<G_LaborCrew> getAsPlannedLaborUse(){
		
		HashSet<G_LaborCrew> crews = new HashSet<G_LaborCrew>();
		
		for(LaborCrew c : act.getLaborUse()){
			for(G_LaborCrew c2 : sim.getLaborCrews()){
				if(c2.getID() == c.getID())
					crews.add(c2);
			}
		}
		return crews;
	}
	
	//Get the base material use for this activity
	public HashMap<G_Material, Integer> getAsPlannedMaterialUse(){
		
		HashMap<G_Material, Integer> materials = new HashMap<G_Material, Integer>();
	
		for(Entry<MaterialType, Integer> e : act.getMaterialUse().entrySet()){
			for(G_Material m : sim.getMaterialTypes()){
				if(m.getID() == e.getKey().getID()){
					materials.put(m, e.getValue());
					break;
				}
			}
		}
		return materials;
	}
	
	//Get the progress of the activity
	public double getPercentCompletion(){
		if(node != null)
			return node.getPercentCompletion();
		return 0;
	}
	
	public double getTotalMaterialsOrdered(){
		if(node != null)
			return node.getPercentOrdered();
		return 0;
	}
	
	public void setTotalMaterialsOrdered(double d){
		if(node != null)
			node.setOrdered(d);
	}
	
	//Whether or not the activity is Active
	public boolean isActive(){
		if(node != null)
			return node.getActive();
		//return node.getStart() >= tonae.getCurrentTimeStep() && node.getEnd() <= tonae.getCurrentTimeStep();
		//return false;
		for(PNode p : tonae.getReadyList()){
			if(p.getParentAct().getID() == act.getID()){
				this.node = p;
				return p.getActive();
			}
		}
		return false;
	}
	
	/* TONAE USAGE */
	public double getSVI(){
		return tonae.SVI(act);
	}
	public double getCVI(){
		return tonae.CVI(act);
	}
	public TreeMap<Integer, Double> getAsPlannedTotal(){
		return tonae.getMathAgent().getAsPlanned().getTotalByActivity(act, tonae);
	}
	public TreeMap<Integer, Double> getAsBuiltTotal(){
		return tonae.getMathAgent().getAsBuilt().getTotalByActivity(act, tonae);
	}
	public TreeMap<Integer, Double> getAsPlannedProgress(){
		return tonae.getAsPlannedProgress(act);
	}
	public TreeMap<Integer, Double> getAsBuiltProgress(){
		return tonae.getAsBuiltProgress(act);
	}
	
	//Get variables for this activity
	public HashSet<G_Variable> getLocalVariables(){
		HashSet<G_Variable> vars = new HashSet<G_Variable>();
		for(Variable v : tonae.getEnvironment().getVariables(act)){
			vars.add(new G_Variable(v));
		}
		return vars;
	}
	
	//Get whether or not this activity is Critical
	public boolean isCritical(){
		return tonae.isCritical(act);
	}
	
	//public void setPNode(PNode p){
	//	this.node = p;
	//}
	
	public Activity unwrap(){
		return act;
	}

}
