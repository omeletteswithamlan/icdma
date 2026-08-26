package mtu.construction.project;

import java.io.Serializable;

//Info of an activity's usage of a particular material
//Useful for determining:
// -- How much more of a particular materialtype an activity needs
// -- How much of a particular materialtype an activity has used
//
//TODO: Add a "daily_use" field, use the commented constructor,
// and replace the materialuse structure in Activity
public class MaterialInfo implements Serializable {
	private int total_need;
	private int total_used;
	private MaterialType material;
	
	/*
	public MaterialInfo(MaterialType t, int daily_use, int actDuration){
		this.material = t;
		this.total_need = daily_use * actDuration;
		//this.daily_use = daily_use
		this.total_used = 0;
	*/
	
	public MaterialInfo(MaterialType t, int total_need){
		this.material = t;
		this.total_need = total_need;
		this.total_used = 0;
	}
	
	public MaterialType getMaterial(){
		return material;
	}
	
	public int getTotalNeed(){
		return total_need;
	}
	
	public int getTotalUsed(){
		return total_used;
	}
	
	public void addUse(int amount){
		total_used += amount;
	}
}
