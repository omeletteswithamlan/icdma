package mtu.construction.gui.wrapper;

import mtu.construction.variable.Variable;

//Wrapper class for Variable
//Restrict GUI access
public class G_Variable {

	private Variable var;
	
	//Constructor
	public G_Variable(Variable v){
		var = v;
	}
	
	//Get the unique ID for this variable
	public int getID(){
		return var.getID();
	}
	
	//Get the Name of this variable
	public String getLabel(){
		return var.getLabel();
	}
	
	//Get the state of this variable
	public String getStringState(){
		return var.getStringState();
	}
	
	//Get whether or not this variable is Global
	public boolean isGlobal(){
		return var.isGlobal();
	}
	
}
