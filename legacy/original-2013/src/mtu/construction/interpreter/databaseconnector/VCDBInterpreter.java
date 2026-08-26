package mtu.construction.interpreter.databaseconnector;

import java.util.HashMap;
import java.util.HashSet;

import mtu.construction.project.Activity;
//import mtu.construction.plan.AvailibleProject;
import mtu.construction.project.CSIDivision;
import mtu.construction.project.Constraint;
import mtu.construction.interpreter.databaseconnector.DBConnection;
import mtu.construction.project.LaborCrew;
import mtu.construction.project.LaborType;
import mtu.construction.project.MaterialType;
import mtu.construction.project.Project;
import mtu.construction.project.Media;
import mtu.construction.project.PlanException;
import mtu.construction.project.Responsibility;
import mtu.construction.project.Skill;
import mtu.construction.interpreter.DatabaseInterpreter;
import mtu.construction.variable.Rule;
import mtu.construction.variable.Variable;

/**
 * This class interprets the VCDB database in order to retrieve plan information
 * 
 * @author  Matt Watkins
 */
public class VCDBInterpreter extends DatabaseInterpreter
{
	protected int projectid;

	public VCDBInterpreter(DBConnection conn, int projectid) throws PlanException
	{
		this.conn = conn;
		this.projectid = projectid;
	}
	
	/* Unused method
	public static HashSet<Project> getAvailibleProjects(DBConnection conn) throws PlanException
	{
		String query = "";
		query += "select * from project;";
		
		return getAvailibleProjects(query, conn);
	}*/
	
	public Project getProject() throws PlanException
	{
		String query = "";
		query += "select * from ";
		query += "project p where ";
		query += "p.projectid = " + projectid +";";
		
		return getProject(query);
	}
	
	public Skill[] getSkills() throws PlanException
	{
		String query = "select * from skill;";
		
		return getSkills(query);
	}
	
	public void setSkill(Skill s)
	{
		skill = s;
	}
	
	public LaborType[] getLaborTypes() throws PlanException
	{
		String query = "";
		query += "select distinct l.laborid as laborid, l.description as description, l.unitcost as unitcost from ";
		query += "activity a, ";
		query += "project p, ";
		query += "labor l, ";
		query += "laboruse lu ";
		query += "where ";
		query += "p.projectid = a.projectid and ";
		query += "a.activityid = lu.activityid and ";
		query += "lu.laborid = l.laborid and ";
		query += "p.projectid = " + projectid + " order by l.laborid;";
		return getLaborTypes(query);
	}

	public MaterialType[] getMaterialTypes() throws PlanException
	{
		String query = "";
		query += "select distinct m.* from ";
		query += "activity a, ";
		query += "project p, ";
		query += "material m, ";
		query += "materialuse mu ";
		query += "where ";
		query += "p.projectid = a.projectid and ";
		query += "a.activityid = mu.activityid and ";
		query += "mu.materialid = m.materialid and ";
		query += "p.projectid = " + projectid + " order by m.materialid;";
		return getMaterialTypes(query);
	}
	
	public Activity[] getActivities(CSIDivision[] div, Responsibility[] resp) throws PlanException
	{
		String query = "";
		query += "select * from ";
		query += "activity a, ";
		query += "project p, ";
		query += "csidivision c, ";
		query += "responsibility r ";
		query += "where ";
		query += "a.projectid = p.projectid and ";
		query += "a.csidivisionid = c.csiid and ";
		query += "a.responsibilityid = r.responsibilityid and ";
		query += "p.projectid = " + projectid + " order by a.activityid;";
		
		return getActivities(query, div, resp);
	}

	public Constraint[] getConstraints(Activity[] activities) throws PlanException
	{
		String query = ""; 
		query += "select distinct c.constraintid as constraintid, c.fromactivityid as from, c.toactivityid as to, c.length as duration, c.soft as soft from ";
		query += "constraints c, ";
		query += "activity a, ";
		query += "project p ";
		query += "where ";
		query += "p.projectid = a.projectid and ";
		query += "(a.activityid = c.fromactivityid or a.activityid = c.toactivityid) and ";
		query += "p.projectid = " + projectid + ";";
		
		return getConstraints(query, activities);
	}
	
	public LaborCrew[] getLaborCrews(LaborType[] labor) throws PlanException
	{
		String query = "";
		query += "select distinct l.laborcrewid as laborcrewid, l.description as description from laborcrew l";
		query += ", laborcrewuse u, activity a, project p ";
		query += "where p.projectid = "+projectid+" ";
		query += "and a.projectid = p.projectid ";
		query += "and a.activityid = u.activityid ";
		query += "and u.laborcrewid = l.laborcrewid;";
		return getLaborCrews(labor, query);
	}
	
	public void populateLaborCrew(LaborType[] labor, LaborCrew crew) throws PlanException
	{
		String query = "";
		query += "select laborid, amount from laborcrewentry where laborcrewid = " + crew.getID() + ";";
		populateLaborCrew(labor, crew, query);
	}
	
	public HashSet<LaborCrew> getLaborCrewUse(LaborCrew[] crews, Activity a) throws PlanException
	{
		String query = "";
		query += "select laborcrewid from laborcrewuse where activityid = " + a.getID() + ";";
		return getLaborCrewUse(crews, query);
	}
	
	public HashMap<MaterialType, Integer> getMaterialUse(MaterialType[] materials, Activity activity) throws PlanException
	{
		String query = "";
		query += "select m.materialid as materialid, m.quantity as quantity from materialuse m ";
		query += "where ";
		query += "m.activityid = " + activity.getID() +";";
		return getMaterialUse(materials, query);
	}

	public CSIDivision[] getCSIDivision() throws PlanException {
		
		String query = "";
		query += "select c.csiid as csiid, c.name as name, c.description as description from ";
		query += "csidivision c ";

		
		return getCSIDivisions(query);
		
	}

	public Responsibility[] getResponsibility() throws PlanException
	{
		String query = "";
		query += "select r.responsibilityid as respid, r.responsibilityname as name from ";
		query += "responsibility r;";
		return getResponsibility(query);
	}
	
	public Rule[] getRules(Variable[] v) throws PlanException
	{
		String query = "";
		query += "select r.* from projectrule projr, rule r where ";
		query += "r.ruleid = projr.ruleid and ";
		query += "projr.projectid = " + projectid +" ";
		query += "order by projr.ordering;";
		return getRules(v, query);
	}
	
	public Variable[] getVariables(int projectid, MaterialType[] mat) throws PlanException
	{
		String query = "";
		query += "select distinct v.* from projectrule projr, rule r, ";
		query += "ruleprecondition prer, precondition pre, variable v ";
		query += "where projr.ruleid = r.ruleid and ";
		query += "prer.ruleid = r.ruleid and ";
		query += "prer.preconditionid = pre.preconditionid and ";
		query += "pre.variableid = v.variableid and ";
		query += "projr.projectid = "+ projectid +" ";
		query += "union ";
		query += "select distinct v.* from projectrule projr, rule r, ";
		query += "rulepostcondition postr, postcondition post, variable v ";
		query += "where projr.ruleid = r.ruleid and ";
		query += "postr.ruleid = r.ruleid and ";
		query += "postr.postconditionid = post.postconditionid and ";
		query += "post.variableid = v.variableid and ";
		query += "projr.projectid = "+ projectid +";";
		return getVariables(query, projectid, mat);
	}
	
	public Media[] getMedia(int id, String tablename) throws PlanException
	{
		String query = "";
		query += "select m.* from media m, " + tablename + " l, " + tablename + "media lm, skill s, mediaskill ms ";
		query += "where m.mediaid = ms.mediaid and ";
		query += "s.skillid = ms.skillid and ";
		query += "l." + tablename + "id = lm." + tablename + "id and ";
		query += "lm.mediaskillid = ms.mediaskillid and ";
		query += "s.skillid = " + skill.getID() + " and ";
		query += "l." + tablename + "id = " + id + ";";
		
		return getMedia(query);
	}
	
	public Project buildProject() throws PlanException{
		Project project;
		
		Skill[] skills = getSkills();
		
		//temporarily set the skill to the first one returned
		Skill skill = skills[0];		//skill level of the user
		
		setSkill(skill);
		
		project = getProject();
		project.setSkill(skill);
		//get each of the basic elements needed for a plan
		project.setLaborTypes(getLaborTypes());
		project.setMaterialTypes(getMaterialTypes());
		project.setCSIDivisions(getCSIDivision());
		project.setResponsibilities(getResponsibility());
		project.setActivities(getActivities(project.getCSIDivisions(), project.getResponsibilities()));
		project.setVariables(getVariables(project.getID(), project.getMaterialTypes()));
		project.setRules(getRules(project.getVariables()));
		project.setLaborCrews(getLaborCrews(project.getLaborTypes()));
		
		//constraint links will be added as the constraints are created
		project.setConstraints(getConstraints(project.getActivities()));
		
		for(Activity a : project.getActivities())
			project.addResourceUsage(a, this);
				
		int i = 1;
		for(Activity a : project.getActivities())
			a.setID(i++);

		return project;
	}
}
