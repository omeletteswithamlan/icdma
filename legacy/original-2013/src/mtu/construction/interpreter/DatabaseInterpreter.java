package mtu.construction.interpreter;

import mtu.construction.interpreter.databaseconnector.DBConnection;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

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
import mtu.construction.project.TimeFrame;
import mtu.construction.variable.Condition;
import mtu.construction.variable.ContinV;
import mtu.construction.variable.DiscreteV;
import mtu.construction.variable.Rule;
import mtu.construction.variable.Variable;

/**
 * This class interprets databases, retrieving information from them.
 * 
 * @author Matt Watkins
 * @author Jessica Anderson
 */
public abstract class DatabaseInterpreter implements PlanInterpreter
{
	protected DBConnection conn;
	protected Skill skill;
	
	/* Unused function
	public static HashSet<AvailableProject> getAvailibleProjects(String querystring, DBConnection conn) throws PlanException
	{
		HashSet<AvailableProject> projects = new HashSet<AvailableProject>();
		
		try
		{
			ResultSet results = conn.createStatement().executeQuery(querystring);
			while(results.next())
			{
				int id = results.getInt("projectid");
				String description= results.getString("description");
				projects.add(new AvailableProject(id, description));
			}
			
			return projects;
		}
		catch(SQLException e)
		{
			throw new PlanException("An error occured while communicating with the database", e);
		}
		
	}*/
	
	protected Project getProject(String querystring) throws PlanException
	{
		try
		{
			ResultSet results = conn.createStatement().executeQuery(querystring);
			if(results.next())
			{
				int space = results.getInt("space");
				String name = results.getString("name");
				int id = results.getInt("projectid");
				String description = results.getString("description");
				double op= (double)results.getFloat("overstock_penalty");
				double oh= (double)results.getFloat("overhead");
				Date date= results.getDate("startdate");
				int interval = results.getInt("interval");
				
				TimeFrame t;
				if(interval == 1)
					t = TimeFrame.ONE_DAY;
				else if(interval == 7)
					t = TimeFrame.ONE_WEEK;
				else if(interval == 28)
					t = TimeFrame.FOUR_WEEK;
				else
					throw new PlanException("Improper time frame interval in project table", null);
				
				GregorianCalendar c = new GregorianCalendar(date.getYear() + 1900, date.getMonth(), date.getDate());
				
				Project p = new Project(id, name, c, description, space, op, oh, t);
				p.setMedia(getMedia(id, "project"));
				System.out.println("Project Created...");
				return p;
			}
			else
			{
				throw new PlanException("No valid plans were found!", null);
			}
		}
		catch(SQLException e)
		{
			throw new PlanException("An error occured while communicating with the database", e);
		}
	}
	
	protected Skill[] getSkills(String querystring) throws PlanException
	{
		try
		{
			ResultSet results = conn.createStatement().executeQuery(querystring);
			
			ArrayList<Skill> skill = new ArrayList<Skill>();
			while(results.next())
			{
				int skillid = results.getInt("skillid");
				String description = results.getString("description");
				
				Skill s = new Skill(skillid, description);
				skill.add(s);
			}
			
			Skill[] skilllist = new Skill[skill.size()];
			for(int x = 0; x < skilllist.length; x++)
				skilllist[x] = skill.get(x);
			
			return skilllist;
		}
		catch(SQLException e)
		{
			throw new PlanException("An error occured while communicating with the database", e);
		}
	}
	
	protected LaborType[] getLaborTypes(String querystring) throws PlanException
	{
		try
		{
			ResultSet results = conn.createStatement().executeQuery(querystring);
			
			ArrayList<LaborType> labor = new ArrayList<LaborType>();
			while(results.next())
			{
				int laborid = results.getInt("laborid");
				String description;
				double cost = results.getDouble("unitcost");
				
				try
				{
					description = results.getString("description");
				}
				catch(SQLException e)
				{
					//column doesn't exist, so set description to null
					description = null;
				}
				
				LaborType l = new LaborType(laborid, description, cost);
				l.setMedia(getMedia(laborid, "labor"));
				labor.add(l);
			}
			
			LaborType[] labortype = new LaborType[labor.size()];
			for(int x = 0; x < labortype.length; x++)
				labortype[x] = labor.get(x);
			
			return labortype;
		}
		catch(SQLException e)
		{
			throw new PlanException("An error occured while communicating with the database", e);
		}
	}
	
	protected MaterialType[] getMaterialTypes(String querystring) throws PlanException
	{
		try
		{
			ResultSet results = conn.createStatement().executeQuery(querystring);
			
			ArrayList<MaterialType> material = new ArrayList<MaterialType>();
			while(results.next())
			{
				int materialid = results.getInt("materialid");
				String description;
				double cost = results.getDouble("unitcost");
				double size = results.getDouble("area");
				boolean perishable = results.getBoolean("perishable");
				
				try
				{
					description = results.getString("description");
				}
				catch(SQLException e)
				{
					//column doesn't exist, so set description to null
					description = null;
				}
				
				MaterialType m = new MaterialType(materialid, description, cost, size, perishable);
				m.setMedia(getMedia(materialid, "material"));
				material.add(m);
			}
			
			MaterialType[] materialtype = new MaterialType[material.size()];
			for(int x = 0; x < materialtype.length; x++)
				materialtype[x] = material.get(x);
			
			return materialtype;
		}
		catch(SQLException e)
		{
			throw new PlanException("An error occured while communicating with the database", e);
		}
	}
	
	protected Activity[] getActivities(String querystring, CSIDivision[] div, Responsibility[] resp) throws PlanException
	{
		try
		{
			ResultSet results = conn.createStatement().executeQuery(querystring);
			
			ArrayList<Activity> activity = new ArrayList<Activity>();
			while(results.next())
			{
				int activityid = results.getInt("activityid");
				String description;
				int duration = results.getInt("duration");
				int csidivision = results.getInt("csidivisionid");
				int responsibility = results.getInt("responsibilityid");
				/*New Addition
				 * TODO: Add this to the activity
				 * */
				//String critialMaterial = results.getString("material");
				
				
				String code = results.getString("code");
				
				try
				{
					description = results.getString("description");
				}
				catch(SQLException e)
				{
					//column doesn't exist, so set description to null
					description = null;
				}
				
				Activity a = new Activity(activityid, description, code, duration, findCSIDiv(div, csidivision), findResponsibility(resp, responsibility), findDrivingMaterials(activityid));
				a.setMedia(getMedia(activityid, "activity"));
				activity.add(a);
			}

			
			Activity[] activitytype = new Activity[activity.size()];
			for(int x = 0; x < activitytype.length; x++)
				activitytype[x] = activity.get(x);
			
			return activitytype;
		}
		catch(SQLException e)
		{
			throw new PlanException("An error occured while communicating with the database", e);
		}
	}
	
	protected CSIDivision[] getCSIDivisions(String querystring) throws PlanException
	{
		try
		{
			ResultSet results = conn.createStatement().executeQuery(querystring);
			
			ArrayList<CSIDivision> csi = new ArrayList<CSIDivision>();
			while(results.next())
			{
				int csidivisionid = results.getInt("csiid");
				String name = results.getString("name");
				String description;
				
				try
				{
					description = results.getString("description");
				}
				catch(SQLException e)
				{
					//column doesn't exist, so set description to null
					description = null;
				}
				
				csi.add(new CSIDivision(csidivisionid, name, description));
			}
			
			CSIDivision[] csidiv = new CSIDivision[csi.size()];
			for(int x = 0; x < csidiv.length; x++)
				csidiv[x] = csi.get(x);
			
			return csidiv;
		}
		catch(SQLException e)
		{
			throw new PlanException("An error occured while communicating with the database", e);
		}
	}
	
	protected Media[] getMedia(String querystring) throws PlanException
	{
		try
		{
			ResultSet results = conn.createStatement().executeQuery(querystring);
			
			ArrayList<Media> resp = new ArrayList<Media>();
			while(results.next())
			{
				int id = results.getInt("mediaid");
				String type = results.getString("objtype");
				Object obj = results.getBytes("object");

				
				resp.add(new Media(id, type, obj));
			}
			
			Media[] respons = new Media[resp.size()];
			for(int x = 0; x < respons.length; x++)
				respons[x] = resp.get(x);
			
			return respons;
		}
		catch(SQLException e)
		{
			throw new PlanException("An error occured while communicating with the database", e);
		}
	}
	
	protected Responsibility[] getResponsibility(String querystring) throws PlanException
	{
		try
		{
			ResultSet results = conn.createStatement().executeQuery(querystring);
			
			ArrayList<Responsibility> resp = new ArrayList<Responsibility>();
			while(results.next())
			{
				int id = results.getInt("respid");
				String name = results.getString("name");

				
				resp.add(new Responsibility(id, name));
			}
			
			Responsibility[] respons = new Responsibility[resp.size()];
			for(int x = 0; x < respons.length; x++)
				respons[x] = resp.get(x);
			
			return respons;
		}
		catch(SQLException e)
		{
			throw new PlanException("An error occured while communicating with the database", e);
		}
	}
	
	protected Constraint[] getConstraints(String querystring, Activity[] activities) throws PlanException
	{
		try
		{
			ResultSet results = conn.createStatement().executeQuery(querystring);
			
			ArrayList<Constraint> constraints = new ArrayList<Constraint>();
			while(results.next())
			{
				int id = results.getInt("constraintid");
				int from = results.getInt("from");
				int to = results.getInt("to");
				int duration = results.getInt("duration");
				boolean soft = results.getBoolean("soft");
				
				Activity s = findActivity(activities, from);
				Activity e = findActivity(activities, to);
								
				constraints.add(new Constraint(id, s, e, duration, soft));
			}
			
			Constraint[] constrainttype = new Constraint[constraints.size()];
			for(int x = 0; x < constrainttype.length; x++)
				constrainttype[x] = constraints.get(x);
			
			return constrainttype;
		}
		catch(SQLException e)
		{
			throw new PlanException("An error occured while communicating with the database", e);
		}
	}
	
	protected Rule[] getRules(Variable[] v, String querystring) throws PlanException
	{
		Random r = new Random();
		try
		{
			ResultSet results = conn.createStatement().executeQuery(querystring);
			
			ArrayList<Rule> rules = new ArrayList<Rule>();
			while(results.next())
			{
				//unused to this point
				int ruleid = results.getInt("ruleid");
				String description = results.getString("description");
				String message = results.getString("message");
				double probability = results.getDouble("probability");
				boolean global = results.getBoolean("global");
				
				Rule ru = new Rule(description, message,
						getPreconditions(ruleid, v),
						getPostconditions(ruleid, v),
						probability, global, ruleid);
				
				ru.setMedia(getMedia(ruleid, "rule"));
				
				rules.add(ru);
				
			}
			
			Rule[] rule = new Rule[rules.size()];
			for (int x = 0; x < rule.length; x++)
				rule[x]= rules.get(x);
			return rule;
		}
		catch(SQLException e)
		{
			throw new PlanException("An error occured while communicating with the database", e);
		}
	}
	
	protected Variable[] getVariables(String querystring, int projectid, MaterialType[] mat) throws PlanException
	{
		try
		{
			ResultSet results = conn.createStatement().executeQuery(querystring);
			
			boolean idfound = false;
			boolean productivityfound = false;
			boolean activitytimefound = false;
			
			ArrayList<Variable> vars = new ArrayList<Variable>();
			while(results.next())
			{
				
				//vars.add(new ContinV(label, global, new Double(initalstate));
				//vars.add(new DiscreteV(label, global, initalstate);
				
				//unused to this point
				int variableid = results.getInt("variableid");
				String label= results.getString("label");
				boolean global= results.getBoolean("global");
				String initialstate = results.getString("initialstate");
				boolean discreet = results.getBoolean("discreet");
				
				if(label.equals("ID"))
					idfound = true;
				if(label.equals("ActivityTime"))
					activitytimefound = true;
				if(label.equals("Productivity"))
					productivityfound = true;
				
				Variable var;
				if (discreet)
				{
					var = new DiscreteV(variableid, label, global, initialstate);
				} else
				{
					var = new ContinV(variableid, label, global, new Double(initialstate));
				}
				
				vars.add(var);
				
				for(MaterialType t : getMatTypesForVariable(var, projectid, mat))
				{
					var.setMaterial(t);
				}
			}
			
			if(!idfound)
				vars.add(new DiscreteV(-1, "ID", false, "0"));
			if(!activitytimefound)
				vars.add(new DiscreteV(-2, "ActivityTime", false, "0"));
			if(!productivityfound)
				vars.add(new ContinV(-3, "Productivity", false, 1));
			
			//Add new Variables here
			
			Variable[] variables = new Variable[vars.size()];
			for (int x = 0; x < variables.length; x++)
			{
				variables[x]= vars.get(x);
			}
			 return variables;
		}
		catch(SQLException e)
		{
			throw new PlanException("An error occured while communicating with the database", e);
		}
		
	}
	
	protected HashSet<MaterialType> getMatTypesForVariable(Variable v, int projectid, MaterialType[] mat) throws PlanException
	{
		try
		{
			String querystring = "select * from materialvariable where " +
			"projectid = " + projectid + " and " +
			"variableid = " + v.getID() + ";"; 
			//TODO: Move these into vcdb interpreter. no query specific stuff should be here
			ResultSet results = conn.createStatement().executeQuery(querystring);

			HashSet<MaterialType> mattype = new HashSet<MaterialType>();
			while(results.next())
			{
				//int preconditionid = results.getInt("preconditionid");
				int materialid = results.getInt("materialid");
				
				for(MaterialType t : mat)
				{
					if(t.getID() == materialid)
						mattype.add(t);
				}
			}
			
			 return mattype;
		}
		catch(SQLException e)
		{
			throw new PlanException("An error occured while communicating with the database", e);
		}
	}
	
	protected Condition[] getPreconditions(int ruleid, Variable[] variables) throws PlanException
	{
		try
		{
			//TODO: Move these into vcdb interpreter. no query specific stuff should be here
			ResultSet results = conn.createStatement().executeQuery(
					"select p.* from precondition p, rule r, ruleprecondition rp where " +
					"p.preconditionid = rp.preconditionid and " +
					"r.ruleid = rp.ruleid and r.ruleid = " + ruleid + ";");
			
			ArrayList<Condition> cond = new ArrayList<Condition>();
			while(results.next())
			{
				//int preconditionid = results.getInt("preconditionid");
				int variableid = results.getInt("variableid");
				String state = results.getString("state");
				String action = results.getString("action");
				
				Variable v = findVariable(variableid, variables);
				cond.add(new Condition(v.getLabel(), state, action));
			}
			
			Condition[] cond2 = new Condition[cond.size()];
			for (int x = 0; x < cond2.length; x++)
			{
				cond2[x]= cond.get(x);
			}
			 return cond2;
		}
		catch(SQLException e)
		{
			throw new PlanException("An error occured while communicating with the database", e);
		}
	}
	
	protected Condition[] getPostconditions(int ruleid, Variable[] variables) throws PlanException
	{
		try
		{
			//TODO: Move these into vcdb interpreter. no query specific stuff should be here
			ResultSet results = conn.createStatement().executeQuery(
					"select p.* from postcondition p, rule r, rulepostcondition rp where " +
					"p.postconditionid = rp.postconditionid and " +
					"r.ruleid = rp.ruleid and r.ruleid = " + ruleid + ";");
			
			ArrayList<Condition> cond = new ArrayList<Condition>();
			while(results.next())
			{
				//int postconditionid = results.getInt("postconditionid");
				int variableid = results.getInt("variableid");
				String state = results.getString("state");
				int time = results.getInt("time");
				String action = results.getString("action");

				Variable v = findVariable(variableid, variables);
				if(v instanceof ContinV)
					cond.add(new Condition(v.getLabel(), new Double(state), time, action));
				else
					cond.add(new Condition(v.getLabel(), state, time, action));
			}
			
			Condition[] cond2 = new Condition[cond.size()];
			for (int x = 0; x < cond2.length; x++)
			{
				cond2[x]= cond.get(x);
			}
			 return cond2;
		}
		catch(SQLException e)
		{
			throw new PlanException("An error occured while communicating with the database", e);
		}
	}
	
	protected LaborCrew[] getLaborCrews(LaborType[] labor, String querystring) throws PlanException
	{
		try
		{
			ResultSet results = conn.createStatement().executeQuery(querystring);
			
			ArrayList<LaborCrew> crews = new ArrayList<LaborCrew>();
			
			while(results.next())
			{
				int id = results.getInt("laborcrewid");
				String descr = results.getString("description");
				LaborCrew cr = new LaborCrew(id, descr);
				populateLaborCrew(labor, cr);
				
				crews.add(cr);
			}
			
			LaborCrew[] c = new LaborCrew[crews.size()];
			for(int x = 0; x < crews.size(); x++)
				c[x] = crews.get(x);
			
			return c;
		}
		catch(SQLException e)
		{
			throw new PlanException("An error occured while communicating with the database", e);
		}
	}
	
	protected void populateLaborCrew(LaborType[] labor, LaborCrew crew, String querystring) throws PlanException
	{
		try
		{
			ResultSet results = conn.createStatement().executeQuery(querystring);
			
			while(results.next())
			{
				int id = results.getInt("laborid");
				int amt = results.getInt("amount");
				
				crew.add(findLaborType(labor, id), amt);
			}
		}
		catch(SQLException e)
		{
			throw new PlanException("An error occured while communicating with the database", e);
		}
	}
	
	protected HashSet<LaborCrew> getLaborCrewUse(LaborCrew[] crews, String querystring) throws PlanException
	{
		try
		{
			ResultSet results = conn.createStatement().executeQuery(querystring);
			
			HashSet<LaborCrew> crewlist = new HashSet<LaborCrew>();
			
			while(results.next())
			{
				int id = results.getInt("laborcrewid");
				crewlist.add(findLaborCrew(crews, id));
			}
			
			return crewlist;
		}
		catch(SQLException e)
		{
			throw new PlanException("An error occured while communicating with the database", e);
		}
	}
	
	protected HashMap<MaterialType, Integer> getMaterialUse(MaterialType[] materials, String querystring) throws PlanException
	{
		try
		{
			ResultSet results = conn.createStatement().executeQuery(querystring);
			
			HashMap<MaterialType, Integer> matuse = new HashMap<MaterialType, Integer>();
			while(results.next())
			{
				MaterialType mat = findMaterialType(materials, results.getInt("materialid"));
				int quantity = results.getInt("quantity");
				
				matuse.put(mat, quantity);
			}
			
			return matuse;
		}
		catch(SQLException e)
		{
			throw new PlanException("An error occured while communicating with the database", e);
		}
	}
	
	private Variable findVariable(int vid, Variable[] list)
	{
		for(Variable v : list)
		{
			if(v.getID() == vid)
				return v;
		}
		
		return null;
	}
	
	private Activity findActivity(Activity[] activities, int id)
	{
		for(int x = 0; x < activities.length; x++)
		{
			if(activities[x].getID() == id)
				return activities[x];
		}
		
		return null;
	}
	
	private LaborCrew findLaborCrew(LaborCrew[] crews, int id)
	{
		for(int x = 0; x < crews.length; x++)
		{
			if(crews[x].getID() == id)
				return crews[x];
		}
		
		return null;
	}
	
	private MaterialType findMaterialType(MaterialType[] materials, int id)
	{
		for(int x = 0; x < materials.length; x++)
		{
			if(materials[x].getID() == id)
				return materials[x];
		}
		
		return null;
	}
	
	private LaborType findLaborType(LaborType[] labor, int id)
	{
		for(int x = 0; x < labor.length; x++)
		{
			if(labor[x].getID() == id)
				return labor[x];
		}
		
		return null;
	}
	
	private CSIDivision findCSIDiv(CSIDivision[] div, int id) throws PlanException
	{
		for(int x = 0; x < div.length; x++)
		{
			if(div[x].getCSIId() == id)
				return div[x];
		}
		
		throw new PlanException("Invalid CSI Division attached to activity!!!!", new Exception());
	}
	
	private Responsibility findResponsibility(Responsibility[] resp, int id) throws PlanException
	{
		for(int x = 0; x < resp.length; x++)
		{
			if(resp[x].getResponsibilityId() == id)
				return resp[x];
		}
		
		throw new PlanException("Invalid CSI Division attached to activity!!!!", new Exception());
	}
	
	private HashSet<Integer> findDrivingMaterials(int activityid){
		HashSet<Integer> criticalMaterials = new HashSet<Integer>();
		try
		{
			//TODO: Move the query into vcdbinterpreter sometime. --kekoa
			String query2 = "";
			query2 += "select materialid as material ";
			query2 += "from driving_material m ";
			query2 += "where activityid = " + activityid + ";";
			ResultSet crMatResults = conn.createStatement().executeQuery(query2);
			
			while(crMatResults.next())
			{
				criticalMaterials.add(crMatResults.getInt("material"));
			}
			
		}
		catch(SQLException e){
			criticalMaterials = null;
		}
		
		return criticalMaterials;
	}
	
	public void finalize() throws PlanException
	{
		//nothing to do here
	}
}
