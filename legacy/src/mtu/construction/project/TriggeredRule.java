package mtu.construction.project;

import java.io.Serializable;

import mtu.construction.variable.Rule;

/**
 * A rule that has been triggered. Used to save rules in order to easily record them to the database.
 * @author kkaaikal
 *
 */
public class TriggeredRule implements Serializable {
	private Rule rule;
	private String message;
	
	public TriggeredRule(Rule r){
		this(r, "");
	}
	public TriggeredRule(Rule r, String s){
		rule = r;
		message = s;
	}
	
	public Rule getRule(){
		return rule;
	}
	public String getMessage(){
		return message;
	}
}