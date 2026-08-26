package mtu.construction.interpreter;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;

import mtu.construction.project.Activity;
import mtu.construction.project.CSIDivision;
import mtu.construction.project.Constraint;
import mtu.construction.project.LaborCrew;
import mtu.construction.project.LaborType;
import mtu.construction.project.MaterialType;
import mtu.construction.project.Media;
import mtu.construction.project.PlanException;
import mtu.construction.project.Project;
import mtu.construction.project.Responsibility;
import mtu.construction.project.Skill;
import mtu.construction.variable.Rule;
import mtu.construction.variable.RuleResource;
import mtu.construction.variable.Variable;


/**
 * This is a dummy object for interpreting XML files. It is not implemented yet. If each of the
 * functions presented here worked properly, then an instance of this class could be fed to a plan,
 * and it would construct the plan object from the XML file properly.
 * 
 * @author Matt Watkins
 */
public class XMLInterpreter implements PlanInterpreter
{
	public Project getProject() throws PlanException
	{
		return new Project(0, "", new GregorianCalendar(), "", 0,.9, .1, null);
	}
	
	public LaborType[] getLaborTypes() throws PlanException
	{
		return new LaborType[0];
	}
	
	public ArrayList<RuleResource> getRuleResources(int ruleid) throws PlanException
	{
		return null;
	}
	
	public MaterialType[] getMaterialTypes() throws PlanException
	{
		return new MaterialType[0];
	}
	
	public Rule[] getRules(Variable[] v) throws PlanException
	{
		return new Rule[0];
	}
	
	public Activity[] getActivities(CSIDivision[] div, Responsibility[] resp) throws PlanException
	{
		return new Activity[0];
	}
	
	public CSIDivision[] getCSIDivision() throws PlanException
	{
		return new CSIDivision[0];
	}
	
	public Responsibility[] getResponsibility() throws PlanException
	{
		return new Responsibility[0];
	}
	
	public Constraint[] getConstraints(Activity[] activities) throws PlanException
	{
		return new Constraint[0];
	}
	
	/**
	 * Returns a list of variables
	 * 
	 * @return an array of variables
	 */
	public Variable[] getVariables(int projectid, MaterialType[] mat) throws PlanException
	{
		return new Variable[0];
	}
	
	public void populateLaborCrew(LaborType[] labor, LaborCrew crew) throws PlanException
	{
	}
	
	public HashSet<LaborCrew> getLaborCrewUse(LaborCrew[] crews, Activity a) throws PlanException
	{
		return null;
	}
	
	public LaborCrew[] getLaborCrews(LaborType[] labor) throws PlanException
	{
		return null;
	}
	
	public HashMap<MaterialType, Integer> getMaterialUse(MaterialType[] materials, Activity activity) throws PlanException
	{
		return null;
	}
	
	public void finalize() throws PlanException
	{
	}

	public Media[] getMedia(int id, String tablename) throws PlanException
	{
		return null;
	}

	public Skill[] getSkills() throws PlanException
	{
		return null;
	}

	public void setSkill(Skill s){}
}
