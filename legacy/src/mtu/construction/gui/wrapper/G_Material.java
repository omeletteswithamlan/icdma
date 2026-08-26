package mtu.construction.gui.wrapper;

import mtu.construction.gui.old.Graphable;
import mtu.construction.project.MaterialType;
import mtu.construction.project.Media;

//Wrapper class for materialType
//Restrict GUI access
public class G_Material implements Graphable {

	private MaterialType mat;
	
	public G_Material(MaterialType t){
		mat = t;
	}
	
	//Get the unique ID for this material
	public int getID(){
		return mat.getID();
	}
	
	//Get the name of this material
	public String getLabel(){
		return mat.getDescription();
	}
	
	//Get the media associated with this material
	public Media[] getMedia(){
		return mat.getMedia();
	}
	
	//Get whether or not this material is perishable
	public boolean isPerishable(){
		return mat.getPerishable();
	}
	
	//Get the size of this material
	public double getSize(){
		return mat.getSize();
	}
	
	//Get the per-unit cost of this material
	public double getCost(){
		return mat.getCost();
	}
	
	public MaterialType getMaterial(){
		return mat;
	}
	
	public MaterialType unwrap(){
		return mat;
	}
	
}
