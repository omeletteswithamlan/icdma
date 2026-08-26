package mtu.construction.tonae;

import mtu.construction.icdma.Simulator;
import mtu.construction.interpreter.databaseconnector.DBConnection;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;

import mtu.construction.project.Activity;
import mtu.construction.project.LaborCrew;
import mtu.construction.project.LaborType;
import mtu.construction.project.MaterialType;
import mtu.construction.project.Project;
import mtu.construction.project.TONAE;
import mtu.construction.variable.Rule;
import mtu.construction.variable.Variable;
import mtu.construction.project.TriggeredRule;

public class DBRecorder implements Serializable
{
	protected long historyid;
	protected int lasthistorytimeid;

	//there is a bug in the JDBC driver for postgres where if you submit too many queries at once, the
	//function call for executing the query will enter an infinite loop. So this index variable is to
	//avoid this bug. Only 10 queries at a time will be submitted. It is very slow to submit them one
	//by one because each new signal sent to the database is slow, so doing 10 at a time should provide
	//at least a marginal speedup....
	private int counter;
	private static final int MAX_QUERY = 10;
	private String querystring = "";

	/********************************************************************************************
	 * These booleans are debug control variables that can be used to help understand the code!
	 ********************************************************************************************/
	private boolean dbdbug=false; //make this line true to print out database quarries to console
	private boolean dbrecord=true; // set this variable to false in order to suppress database writing

	public DBRecorder(Project project, DBConnection conn) throws SQLException
	{
		/*if (historyid<=0) 
		{
			dbrecord=false;
			System.out.println("historyID errorenous, supressing database out put");
		}*/
		historyid = insertColumn(conn, "history", "insert into history(projectid, skillid) values(" +
				project.getID() + ", " + project.getSkill().getID() + ")");

		//check for "bad" historyID
		if (historyid<=0) 
		{
			System.out.println("historyID erroneous, suppressing database output.");
			dbrecord=false;
		}
		System.out.println("\n*********************************");
		System.out.println("Your history id is: " + historyid);
		System.out.println("*********************************\n");

		lasthistorytimeid = -1;
	}

	private void query(DBConnection conn, String query)
	{
		counter++;
		querystring += query;
		if (dbdbug)System.out.println(query);
		if(counter % MAX_QUERY == 0)
		{
			insertBatch(conn, querystring);

			querystring = "";
		}
	}

	/**
	 * temporarily suppressed database writing wile debugging the this function
	 * 
	 * @param conn database connection
	 * @param tonae the simulator state to be used for reference when building the queries
	 * @param alloc a vector of resource allocations
	 * @param crews an array of labor crews conveying 
	 * @param spaceused how much space is used in the simulator this turn
	 * @param laborcost
	 * @param materialcost
	 * @param indirectcost 
	 * @param queryresults
	 */
	public void recordturn(DBConnection conn, TONAE tonae, Vector<ResourceAllocation> alloc, HashMap<Activity, HashMap<MaterialType, Integer>> usedMaterials, LaborCrew[] crews, double spaceused, double laborcost, double materialcost, double indirectcost, Vector<TriggeredRule> rules, QueryResult2 queryresults)
	{

		int id;

		String htquery = "insert into historytime(historyid, time, spaceused, laborcost, materialcost, indirectcost";
		if(lasthistorytimeid != -1)
			htquery += ", lasthistorytimeid";
		htquery += ") values(" + historyid + ", " + tonae.getCurrentTimeStep() + ", " + spaceused + ", " + laborcost + ", " + materialcost + ", " + indirectcost;
		if(lasthistorytimeid != -1)
			htquery += ", " + lasthistorytimeid;
		htquery += ")";

		id = insertColumn(conn, "historytime", htquery);
		
		System.out.println("\n*************************");
		System.out.println("HistoryTimeID is: "+ id +".");
		System.out.println("*************************\n");

		//record activities for this turn
		for(PNode p : tonae.getReadyList()){
			//for(ANode node : tonae.getANodeSet())
			//{
			//if(node.getOutPrimaryArc() != null)
			//{
			//the first arc links to a PNode which sits on the ANode. The PNode links to the end of the activity
			//Node endnode = node.getOutPrimaryArc().getHeadNode().getOutPrimaryArc().getHeadNode();
			//int end = endnode.getEarlyStart();
			//int start = node.getEarlyStart();
			PNode node = p;
			int start = node.getInPrimaryArc().getTailNode().getEarlyStart();
			int end = node.getOutPrimaryArc().getHeadNode().getEarlyStart();

			//if (tonae.getCurrentTimeStep()>=node.getParentAct().getStart() && tonae.getCurrentTimeStep()<node.getParentAct().getEnd())
			//{
			query(conn, "insert into historyactivity(historytimeid, activityid, starttime, endtime) values(" + 
					id + ", " + node.getParentAct().getRealID() + ", " + start + ", " + end + ");");
			//}
			//}
			//}
		}


		//record the global variables for this turn
		for(Variable v : tonae.getEnvironment().getGlobalVariables())
		{
			//unused required variables (ID, ActivityTime, Productivity) have a negative ID,
			//so don't record them (they aren't used!)
			if(v.getID() >= 0)
			{
				query(conn, "insert into historyvariable(historytimeid, variableid, state) values(" + 
						id + ", " + v.getID() + ", '" + v.getStringState() + "');");
			}
		}

		//record the local variables for this turn
		for(Activity a : tonae.getProject().getActivities())
		{
			if(tonae.getCurrentTimeStep()< a.getEnd()&&tonae.getCurrentTimeStep()>=a.getStart())
			{
				for(Variable v : tonae.getEnvironment().getVariables(a))
				{
					query(conn, "insert into historyvariable(historytimeid, activityid, variableid, state) values(" + 
							id + ", " + a.getRealID() + ", " + v.getID() + ", '" + v.getStringState() + "');");
				}
			}
		}

		//record the material allocation
		for(ResourceAllocation r : alloc)
		{
			HashMap<MaterialType, Integer> materialuse = null;
			if(r.getActivity() != null){
				System.out.println(r.getActivity().getDescription());
				materialuse = usedMaterials.get(r.getActivity());
			}
			if(materialuse != null)
			for(MaterialType m : tonae.getProject().getMaterialTypes())
			{
				if(r.getRequested(m) != 0)
				{
					if(r.getActivity() == null)// if the activity associated is null the material belongs to the stock and was either user ordered or extra from a previous turn
						//value of stock materials
						query(conn, "insert into historymaterialallocation(historytimeid, materialid, quantity) values(" +
								id + ", " + m.getID() + ", " + r.getRequested(m) + ");");
					else //if(tonae.getCurrentTimeStep()<r.getActivity().getEnd()&&tonae.getCurrentTimeStep()>=r.getActivity().getStart())//don't write entries after there activity
					{
						//needs context of time -put time logic here
						int used = 0;
						if(materialuse.containsKey(m))
							used = materialuse.get(m);
						query(conn, "insert into historymaterialallocation(historytimeid, materialid, activityid, quantity) values(" +
								id + ", " + m.getID() + ", " + r.getActivity().getRealID() + ", " + used/*r.getRequested(m)*/ + ");");
					}
				}
			}
		}
		
		//record stock materials
		for(Entry<MaterialType, Integer> e : tonae.getStock().entrySet()){
			if(e.getValue() > 0) //Only output to database if the item exists in stock. Otherwise it wastes DB space
			query(conn, "insert into historymaterialallocation(historytimeid, materialid, quantity) values(" +
					id + ", " + e.getKey().getID() + ", " + e.getValue() + ");");
		}

		//record the labor allocation, note that because ResourceAllocations or only made for pnodes in the ready list no check is needed to verify that the activity is still running
		for(ResourceAllocation r : alloc)
		{
			//if(r.getActivity() != null&& tonae.getCurrentTimeStep()<r.getActivity().getEnd()&&tonae.getCurrentTimeStep()>=r.getActivity().getStart())
			//{
			//	for(LaborCrew c : crews)
			//	{
			//		for(LaborCrew c2 : r.getActivity().getLaborUse())
			//		{
			//			if (c==null)System.out.println("c abort");
			//			if (c2==null)System.out.println("c2 abort");
			//			if( c2!=null&&c!=null&&c2.getID() == c.getID())// why should either c or c2 be null? to my knowledge should never happens but does when database writing is suppressed
			//			{
			/*
			for(LaborCrew cr : r.getActivity().getLaborUse()){

				int histlabcrew = insertColumn(conn, "historylaborcrewallocation", "insert into historylaborcrewallocation(historytimeid, laborcrewid, hours, days, wage, activityid)"
						+ " values(" + id + ", " + cr.getID() + ", " + r.getWorkHours() + ", " + r.getWorkDays() + ", " + r.getWageIncentive() + ", " + r.getActivity().getRealID() + ");");

				for(LaborType t : cr.getTypes()){
					int asBuilt = 0;
					for(LaborCrew c : r.getLaborCrews()){
						if(cr.getID() == c.getID()){	
							for(LaborType l : c.getTypes())
							{
								if(t.getID() == c.getID()){
									asBuilt = c.getAmt(l);
									break;
								}
							}
						}
					}
					query(conn, "insert into historylaborallocation(historylaborcrewallocationid, laborid, quantity) values(" +
							histlabcrew + ", " + t.getID() + ", " + asBuilt + ");");
				}
			}
			 */

			//Only output the crews that are in the resourceallocation.
			for(LaborCrew c : r.getLaborCrews()){
				int histlabcrew = insertColumn(conn, "historylaborcrewallocation", "insert into historylaborcrewallocation(historytimeid, laborcrewid, hours, days, wage, activityid)"
						+ " values(" + id + ", " + c.getID() + ", " + r.getWorkHours() + ", " + r.getWorkDays() + ", " + r.getWageIncentive() + ", " + r.getActivity().getRealID() + ");");
					for(LaborType l : c.getTypes())
					{
						query(conn, "insert into historylaborallocation(historylaborcrewallocationid, laborid, quantity) values(" +
								histlabcrew + ", " + l.getID() + ", " + c.getAmt(l) + ");");
					}
			}

			//			} 
			//		}
			//	}
			//}
		}

		//record the rules
		for(TriggeredRule r : rules){
			query(conn, "insert into historyevent(historytimeid, ruleid, description) values(" + id +", "+ r.getRule().getRuleID() +", '"+ r.getMessage()+"');");
		}

		if(queryresults != null)
		{
			//record the last query results
			TreeMap<Double, Integer> data = queryresults.getDistribution(Simulator.QUERY_FUTURES_QUANTINIZATION);

			//System.out.println("e: " + data.entrySet().size());
			for(Entry<Double, Integer> e : data.entrySet())
			{
				query(conn, "insert into historyqueryresults(historytimeid, cost, quantity) values("
						+ id + ", " + e.getKey() + ", " + e.getValue() + ");");
			}
			
			//Record day distribution
			TreeMap<Integer, Integer> data2 = queryresults.getDayDistribution();
			for(Entry<Integer, Integer> e :data2.entrySet()){
				query(conn, "insert into historyquerydays(historytimeid, length, quantity) values("
						+id+", "+e.getKey()+", "+e.getValue()+");");
			}
			
			//Write the data to a file...
			Simulator.writeQueryResults(""+id, queryresults);
		}

		lasthistorytimeid = id;
	}

	public void endrecord(DBConnection conn)
	{
		if(querystring != ""&&dbrecord)
			insertBatch(conn, querystring);
		counter = 0;
		querystring = "";
	}

	protected void insertBatch(DBConnection conn, String query)
	{
		try
		{
			if (dbrecord)
			{
				Statement sql = conn.createStatement();
				sql.executeUpdate(query);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	//this is to get around the fact that postgres jdbc does not support returning auto-generated columns
	protected int insertColumn(DBConnection conn, String table, String query)
	{
		try
		{
			if(dbrecord)
			{
				PreparedStatement st = conn.getPreparedStatement("SELECT nextval('public." + table + "_" + table + "id_seq') as id;");

				ResultSet rs = st.executeQuery();
				if(rs.next())
				{
					int i = rs.getInt("id"); 
					st.close();

					int l = query.indexOf('(');
					query = query.substring(0, l + 1) + table + "id, " + query.substring(l + 1);
					l = query.indexOf('(', l + 1);
					query = query.substring(0, l + 1) + i + ", " + query.substring(l + 1) + ";";

					st = conn.getPreparedStatement(query);

					st.executeUpdate(); 
					return i;
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		return -1;
	}

	public long getHistoryID(){
		return historyid;
	}
}