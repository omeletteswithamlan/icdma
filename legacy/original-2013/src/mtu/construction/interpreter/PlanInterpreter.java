package mtu.construction.interpreter;

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
import mtu.construction.variable.Variable;


/**
 * PlanInterpreter is intended to act as an interface between the plan, and the
 * various data sources that may hold plan information. Each interpreter should
 * know how to do basic reading operations from whichever data source it is reading
 * from. The plan class itself will request that these basic reading operations
 * be performed, and it will collect the results, and assemble them into a plan.
 * Using this method, the plan does not need to be dependent on one particular
 * data source.
 * 
 * @author   Matt Watkins
 */
public interface PlanInterpreter
{
	
	public Skill[] getSkills() throws PlanException;
	
	public void setSkill(Skill s);

	/**
	 * Returns a project corresponding to this plan interpreter
	 * 
	 * @return a project
	 * @throws PlanException
	 */
	public Project getProject() throws PlanException;
	
	/**
	 * Returns a list of all the labor types required by this plan
	 * 
	 * @return an array of LaborType objects required by this plan
	 */
	public LaborType[] getLaborTypes() throws PlanException;
	
	/**
	 * Returns a list of all the material types required by this plan
	 * 
	 * @return an array of MaterialType objects required by this plan
	 */
	public MaterialType[] getMaterialTypes() throws PlanException;
	
	/**
	 * Returns a list of all the activities required by this plan
	 * 
	 * @return an array of the activities required by this plan
	 */
	public Activity[] getActivities(CSIDivision[] div, Responsibility[] resp) throws PlanException;
	
	/**
	 * Returns a list of all the CSI Divisions required by this plan
	 * 
	 * @return an array of the CSI Divisions required by this plan
	 */
	public CSIDivision[] getCSIDivision() throws PlanException;
	
	/**
	 * Returns a list of all the Responsibilities required by this plan
	 * 
	 * @return an array of the CSI Divisions required by this plan
	 */
	public Responsibility[] getResponsibility() throws PlanException;
	
	/**
	 * Returns a list of plan constraints
	 * 
	 * @return an array of plan constraints
	 */
	public Constraint[] getConstraints(Activity[] activities) throws PlanException;
	
	/**
	 * Returns a list of variables
	 * 
	 * @return an array of variables
	 */
	public Variable[] getVariables(int projectid, MaterialType[] mat) throws PlanException;
	
	public Rule[] getRules(Variable[] v) throws PlanException;
	
	/**
	 * returns a mapping of labor types on to integer quantities, so that
	 * the number of labor resources needed by the specified activity can
	 * be determined
	 * 
	 * @param activity   the activity to query
	 * @return a mapping of labor types on to integer quantities
	 * @throws PlanException
	 */
	public LaborCrew[] getLaborCrews(LaborType[] labor) throws PlanException;
	public void populateLaborCrew(LaborType[] labor, LaborCrew crew) throws PlanException;
	public HashSet<LaborCrew> getLaborCrewUse(LaborCrew[] crews, Activity a) throws PlanException;
	
	/**
	 * returns a mapping of material types on to integer quantities, so that
	 * the number of material resources needed by the specified activity can
	 * be determined
	 * 
	 * @param activity   the activity to query
	 * @return a mapping of material types on to integer quantities
	 * @throws PlanException
	 */
	public HashMap<MaterialType, Integer> getMaterialUse(MaterialType[] materials, Activity activity) throws PlanException;
	public Media[] getMedia(int id, String tablename) throws PlanException;

	/**
	 * This will be called at the end of plan generation. The purpose for this
	 * is to allow for the data source to be closed if necessary. For example,
	 * connections to a database must be closed, or any flat files which were
	 * opened must be closed, so that the system no longer consumes those resources.
	 * The constructor of a PlanInterpreter should allocate the resources needed
	 * to read the plan, and this function will deallocate them
	 *
	 */
	public void finalize() throws PlanException;
}
