package mtu.construction.variable;

import java.io.Serializable;

import mtu.construction.project.Activity;

/*****************
 * Condition represents either a precondition or a postcondition
 * @author Matt Watkins
 *
 */
public class Condition implements Serializable
{
	public String varname;
	public String action;
	public int time;						//span of time over which the change takes place (DEPRECATED)
	private VariableComparator cmp;			//comparator for variable
	private VariableMutator mut;			//changes(mutates) the variable
	public String dstate;
	public double cstate;
	
	/****************
	 * Builds a new precondition
	 * @param varname		name of variable
	 * @param state			state of the variable
	 * @param action		action to take (must be add, mul, or set)
	 */
	public Condition(String varname, String state, String action)
	{
		this(varname, state, 0, action);
	}
	
	/****************
	 * Builds a new precondition
	 * @param varname		name of variable
	 * @param state			state of the variable
	 * @param action		action to take (must be add, mul, or set)
	 */
	public Condition(String varname, double state, String action)
	{
		this(varname, state, 0, action);
	}
	
	/****************
	 * Builds a new postcondition
	 * @param varname		name of variable
	 * @param state			state of the variable
	 * @param action		action to take (must be add, mul, or set)
	 * @param time 			time over which the action takes place (deprecated)
	 */
	public Condition(String varname, String state, int time, String action)
	{
		this.varname = varname;
		this.dstate = state;
		this.time = time;
		this.action = action;
		setAction();
	}
	
	/****************
	 * Builds a new postcondition
	 * @param varname		name of variable
	 * @param state			state of the variable
	 * @param action		action to take (must be add, mul, or set)
	 * @param time 			time over which the action takes place (deprecated)
	 */
	public Condition(String varname, double state, int time, String action)
	{
		this.varname = varname;
		this.cstate = state;
		this.time = time;
		this.action = action;
		setAction();
	}
	
	/*********************
	 * Sets the action object for this class
	 *
	 */
	private void setAction()
	{
		if(action.equals("gte"))
			cmp = new GTE();
		else if(action.equals("gt"))
			cmp = new GT();
		else if(action.equals("lte"))
			cmp = new LTE();
		else if(action.equals("lt"))
			cmp = new LT();
		else if(action.equals("eq"))
			cmp = new EQ();
		else if(action.equals("neq"))
			cmp = new NEQ();
		else if(action.equals("set"))
			mut = new VSet();
		else if(action.equals("add"))
			mut = new VAdd();
		else if(action.equals("mul"))
			mut = new VMul();
	}
	
	/********************
	 * String representation of this condition
	 */
	public String toString()
	{
		return "Condition: " + varname + " State: " + dstate;
	}
	
	/***********************
	 * Checks to see if this precondition is met
	 * @param e		environment
	 * @param a		activity to context variable name
	 * @return		true if the condition is met
	 */
	public boolean isMet(Environment e, Activity a)
	{
		Variable var = e.getVariable(a, varname);
		if(var instanceof DiscreteV)
			return cmp.compare((DiscreteV)var, dstate);
		else if(var instanceof ContinV)
			return cmp.compare((ContinV)var, cstate);
		else
			throw new Error("Invalid Varaible type");
	}
	
	/************************
	 * Checks to see if this precondition is met. Uses global variables as context
	 * @param e		environment
	 * @return		true if the condition is met
	 */
	public boolean isMet(Environment e)
	{
		Variable var = e.getGlobalVariable(varname);
		
		if(var instanceof DiscreteV)
			return cmp.compare((DiscreteV)var, dstate);
		else if(var instanceof ContinV)
			return cmp.compare((ContinV)var, cstate);
		else {
			if(var == null){
				System.out.println(varname + " is null.\n");
			}
			else
				System.out.println(var.getLabel()+'\n');
			throw new Error("Invalid Varaible type");
		}
	}
	
	/***************
	 * Apply this post condition to the environment using activity as a context
	 * @param e		environment
	 * @param a		activity context
	 */
	public void apply(Environment e, Activity a)
	{
		Variable var = e.getVariable(a, varname);
		if(var instanceof DiscreteV)
			mut.mutate((DiscreteV)var, dstate, time);
		else if(var instanceof ContinV)
			mut.mutate((ContinV)var, cstate, time);
	}
	
	/******************
	 * Apply this post condition to the environment using global variables as the context
	 * @param e		environment
	 */
	public void apply(Environment e)
	{
		Variable var = e.getGlobalVariable(varname);
		if(var instanceof DiscreteV)
			mut.mutate((DiscreteV)var, dstate, time);
		else if(var instanceof ContinV)
			mut.mutate((ContinV)var, cstate, time);
	}
}

/****************
 * Class for variable comparisons
 * @author Matt Watkins
 *
 */
abstract class VariableComparator implements Serializable
{
	public boolean compare(DiscreteV var, String state)
	{
		return var.getState().equals(state);
	}
	
	public abstract boolean compare(ContinV var, double state);
}

class GTE extends VariableComparator implements Serializable
{
	public boolean compare(DiscreteV var, String state)
	{
		return super.compare(var, state);
	}
	
	public boolean compare(ContinV var, double state)
	{
		return (var.getState() >= state);
	}
}

class GT extends VariableComparator implements Serializable
{
	public boolean compare(DiscreteV var, String state)
	{
		return !super.compare(var, state);
	}
	
	public boolean compare(ContinV var, double state)
	{
		return (var.getState() > state);
	}
}

class LTE extends VariableComparator implements Serializable
{
	public boolean compare(DiscreteV var, String state)
	{
		return super.compare(var, state);
	}
	
	public boolean compare(ContinV var, double state)
	{
		return (var.getState() <= state);
	}
}

class LT extends VariableComparator implements Serializable
{
	public boolean compare(DiscreteV var, String state)
	{
		return !super.compare(var, state);
	}
	
	public boolean compare(ContinV var, double state)
	{
		return (var.getState() < state);
	}
}

class EQ extends VariableComparator implements Serializable
{
	public boolean compare(DiscreteV var, String state)
	{
		return super.compare(var, state);
	}
	
	public boolean compare(ContinV var, double state)
	{
		return (var.getState() == state);
	}
}

class NEQ extends VariableComparator implements Serializable
{
	public boolean compare(DiscreteV var, String state)
	{
		return !super.compare(var, state);
	}
	
	public boolean compare(ContinV var, double state)
	{
		return (var.getState() != state);
	}
}

/*******************
 * Class for changing variables
 * @author Matt Watkins
 */
abstract class VariableMutator implements Serializable
{
	public void mutate(DiscreteV var, String val, int time)
	{
		var.setState(val, time);
	}
	
	public abstract void mutate(ContinV var, double val, int time);
}

class VSet extends VariableMutator implements Serializable
{
	public void mutate(ContinV var, double val, int time)
	{
		var.setState(val, time);
	}
}

class VAdd extends VariableMutator implements Serializable
{
	public void mutate(ContinV var, double val, int time)
	{
		var.setState(var.getState() + val, time);
	}
}

class VMul extends VariableMutator implements Serializable
{
	public void mutate(ContinV var, double val, int time)
	{
		var.setState(var.getState() * val, time);
	}
}