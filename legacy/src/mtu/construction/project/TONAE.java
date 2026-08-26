package mtu.construction.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
//import java.sql.SQLException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Map.Entry;

//Project:
import mtu.construction.project.Activity;
import mtu.construction.project.CSIDivision;
import mtu.construction.project.Constraint;
import mtu.construction.project.LaborCrew;
import mtu.construction.project.LaborType;
import mtu.construction.project.MaterialType;
import mtu.construction.project.Responsibility;

//Tonae:
import mtu.construction.tonae.CostSchedule;
import mtu.construction.tonae.DBRecorder;
import mtu.construction.tonae.PNodeCompare;
import mtu.construction.tonae.ANode;
import mtu.construction.tonae.AgentM;
import mtu.construction.tonae.Arc;
import mtu.construction.tonae.Node;
import mtu.construction.tonae.PNode;
import mtu.construction.tonae.QueryResult;
import mtu.construction.tonae.QueryResult2;
import mtu.construction.serialize.Serializer;
import mtu.construction.gui.old.MissingLaborContainer;
import mtu.construction.icdma.Simulator;
import mtu.construction.interpreter.databaseconnector.DBConnection;

//Listeners:
import mtu.construction.listener.LaborAlteredListener;
import mtu.construction.listener.LaborChangeListener;
import mtu.construction.listener.QueryResultListener;
import mtu.construction.listener.RuleListener;
import mtu.construction.listener.SpaceViolationListener;

//Variable:
import mtu.construction.variable.Condition;
import mtu.construction.variable.ContinV;
import mtu.construction.variable.DiscreteV;
import mtu.construction.variable.Environment;
import mtu.construction.variable.Rule;
import mtu.construction.variable.Variable;

//To Work On:
import mtu.construction.tonae.ResourceAllocation;


//Unused Currently:
//import mtu.construction.tonae.record.DBRecorder;

/**
 * @author  Ryan Anderson
 * @author  Matt Watkins
 * @author  Corey Tebo
 *
 */
public class TONAE implements Serializable, RuleListener
{
	private static final long serialVersionUID = 6172885308929162454L;

	// Management construct for the state of the project
	private TONAEState currentState;

	// Set storage construct for rules
	private final int MAXIMUM_DURATION = 10000;

	//this is a temp variable and can be removed later
	protected long time;

	private Project asPlanned;	//As Planned
	private Project asBuilt;	//As Built
	private Project baseline;	//Baseline

	//event listeners
	private ArrayList<RuleListener> rulelisteners = new ArrayList<RuleListener>();
	private ArrayList<SpaceViolationListener> spaceViolationListener = new ArrayList<SpaceViolationListener>();
	private ArrayList<LaborChangeListener> laborChangeListener = new ArrayList<LaborChangeListener>();
	private ArrayList<LaborAlteredListener> laborAlteredListener = new ArrayList<LaborAlteredListener>();
	private ArrayList<QueryResultListener> querylisteners = new ArrayList<QueryResultListener>();

	private Vector<Rule> rules;
	private Vector<TriggeredRule> tRules = new Vector<TriggeredRule>();

	private double total_space;

	//when true the simulator suppresses run recording and simplifies most computations
	//to enable querying futures for statistically likely outcomes
	public static boolean querymode;	//whether or not we are querying
	public static boolean dbrecord=true;		//whether or not to record to the database
	public static boolean paperGant=true;
	public static boolean fileGant=false;
	public static boolean debugText=false;
	public static boolean stockOutput=false;
	public static boolean queryOutput=false;
	public static boolean costOutput=false;
	public static boolean resourceOutput=false;


	private long timeStart = 0; //Used for timing turn length (how long decisions take...)
	//NOTE: moved into the GUI, not used...

	private DBRecorder db;
	//private FileOutputStream fout;
	//private PrintStream print;

	private HashMap<Activity, HashMap<MaterialType, Integer>> usedMaterials = new HashMap<Activity, HashMap<MaterialType, Integer>>();

	private QueryResult2 result; //The query results for a turn

	//Constructor
	public TONAE(DBConnection conn, Project p)
	{
		/*try
		{
			fout = new FileOutputStream("simulationRecord.txt");
		}catch (IOException e){
			System.out.println("error making simulation transript h: "+e);
		}

		print = new PrintStream(fout);*/
		asPlanned = p;
		asBuilt = (Project)Serializer.copy(p);	//These objects need to be the same yet different
		baseline= (Project)Serializer.copy(p);	//start the baseline schedule off as a copy

		querymode = false;
		total_space = p.getSpace();

		// Set up the initial state of the TONAE
		currentState = new TONAEState(p.getActivities(), total_space);

		currentState.currentDate=(GregorianCalendar)p.getDate().clone();

		// Create the global P-node of the project
		PNode stateGlobal = new PNode();

		// Indicate that the P-Node is the global one
		stateGlobal.setGlobal(true);

		// Assigning the current project the global P-Node
		currentState.setGlobal(stateGlobal);

		// TONAE set up here
		// Set up the steel example
		constructActivities(p);

		//time is an one indexed system
		currentState.setTime(1);

		// Iterator for moving through the list of A-Nodes
		Iterator<ANode> i_aNodeSet;
		// Placeholder for A-Nodes
		ANode aNode;
		// Go set up all of the present Nodes
		// All present nodes are set up at the beginning of the simulation
		// When a present node is added, it removes the link between beginning and end		
		i_aNodeSet = currentState.getANodeSet().iterator();
		while(i_aNodeSet.hasNext())
		{
			aNode = i_aNodeSet.next();

			if (aNode.getOutPrimaryArc() != null)
			{	    
				initPresentNode(aNode);
			}
		}

		currentState.mathAgent = new AgentM(this);
		currentState.mathAgent.setOverstockLoss(p.getOverstockPenalty());
		initVariables();

		startActivities(currentState);

		currentState.environment.update(this.getProject(), this.getCurrentTimeStep(), this.getANodeSet(), null);

		currentState.fsched = getLateSchedule();

		addRuleListener(this);

		try
		{
			if (dbrecord) db = new DBRecorder(getProject(), conn);
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			throw new Error("Critical Failure Creating DBRecorder: "+e);
		}
	}

	/**
	 * Determine if an activity is Critical (i.e. it is imperative that it is given priority otherwise
	 * the entire project end date will be pushed back)
	 * 
	 * @param a - The activity to check
	 * @return - true if it is a critical activity, false otherwise
	 */
	public boolean isCritical(Activity a)
	{
		return(getEarlyStart(a)==getLateStart(a) && getEarlyFinish(a)==getLateFinish(a));
	}

	public Stock getStock()
	{
		return currentState.stock;
	}

	public HashMap<MaterialType, Integer> getPurchaceOrder()
	{
		return currentState.purchasedmaterial;
	}

	public HashMap<MaterialType, Integer> getRejectedMaterial()
	{
		return currentState.rejectedmaterial;
	}

	/**
	 * Purchase some amount of a material
	 * 
	 * @param t - the (type of) material to purchase
	 * @param amt - the amount to purchase
	 */
	public void purchaceMaterial(MaterialType t, int amt)
	{
		if(currentState.purchasedmaterial.containsKey(t))
			currentState.purchasedmaterial.put(t, currentState.purchasedmaterial.get(t) + amt);
		else
			currentState.purchasedmaterial.put(t, amt);
	}

	/**
	 * Remove all (pending?) material purchases from the list
	 * Note: Somebody check this
	 */
	public void clearPurchaceOrder()
	{
		currentState.purchasedmaterial.clear();
	}

	public ArrayList<Stock> getStockTrack()
	{
		return currentState.stock_track;
	}

	public TreeMap<Integer, Double> getAsPlannedProgress(Activity a)
	{
		return currentState.mathAgent.getAsPlannedProgress(a);
	}

	public TreeMap<Integer, Double> getAsBuiltProgress(Activity a)
	{
		return currentState.mathAgent.getAsBuiltProgress(a);
	}

	/*
	public TreeMap<Integer, Double> getBaselineProgress(Activity a)
	{
		return currentState.mathAgent.getBaselineProgress(Activity a);
	}
	 */

	public AgentM getMathAgent()
	{
		return currentState.mathAgent;
	}

	//Get the asBuilt
	public Project getProject()
	{
		return asBuilt;
	}

	//Get as-planned project
	public Project getAsPlannedSched()
	{
		return asPlanned;
	}

	//Get Baseline
	public Project getBaselineSched()
	{
		return baseline;
	}

	public GregorianCalendar getCalendar()
	{
		return currentState.currentDate;
	}

	/********************************************************
	 * BASELINE MANIPULATION FUNCTIONS
	 * The baseline schedule should be manipulated only through these functions. Using getPlan().set.... will cause the changes to not get
	 * propogated into the as-built schedule, and will cause calculation errors.
	 */

	/********************************************************
	 * This function will cause a delay in the baseline schedule. This will re-adjust the daily material so that the material for the remaining days is
	 * stretched out over one day. If the delay also causes the material list to change, use the alterBaselineMaterial function.
	 */
	public void delayBaseline(Activity act, int amt)
	{
		if (!querymode)
		{int a=2;}
		for(Activity a : getProject().getActivities())
		{
			if(a.getID() == act.getID())
			{
				for(ANode node : getANodeSet())
				{
					if(node.getOutPrimaryArc() != null)
					{
						PNode pnode = (PNode)node.getOutPrimaryArc().getHeadNode();
						if(pnode.getParentAct().getID() == act.getID())
						{
							//this is the activity that needs to be altered
							a.setDuration(a.getDuration() + amt, pnode.getTotalWorkLeft());

							this.delayActivity(pnode, amt);
							return;
						}
					}
				}
			}
		}
	}
	/*	
	private LaborType getLaborType(int i)
	{
		for(LaborType t : getPlan().getLaborTypes())
		{
			if(t.getID() == i)
				return t;
		}
		throw new Error("Labor Type not found!");
	}*/

	/* private void testFunc() //example of how to use the update base line schedule requirements
	{
		LaborCrew c = new LaborCrew(3, "testing");
		LaborType t1 = getLaborType(12);		//welder
		LaborType t2 = getLaborType(8);			//structural steel worker
		LaborType t3 = getLaborType(9);			//crane operator


		//this should force welder crew 1 to need 10 welders
		updateBaselineLaborRequirements(c, t1, 10);

		//should remove all structural steel workers from the welder crew 1
		updateBaselineLaborRequirements(c, t2, 0);

		//should add a crane operator to welder crew 1
		updateBaselineLaborRequirements(c, t3, 1);
	}*/

	public LaborCrew getBaseline(LaborCrew c)
	{
		Project p = getProject();
		for(LaborCrew crew : p.getLaborCrews())
		{
			if(crew.getID() == c.getID())
				return crew;
		}
		return null;
	}

	public void updateBaselineLaborRequirements(LaborCrew c, LaborType l, int amount)
	{
		//update a 
		//	Plan p=getPlan();
		LaborCrew crew = getBaseline(c);
		crew.set(l, amount);
		System.out.println("I set the stuff!!!!!");
		//Let crew = laborcrew that came from p.getLaborCrews() with same id as c
		//crew.set(l, amount)
		//test here very thorougly!!!
	}

	public void updateBaselineActivityLaborRequirements(Activity a, LaborCrew c, boolean remove)
	{
		//	Plan p = getPlan();
		Activity act = getBaseline(a);

		if(!remove)
			act.addLaborUse(c);
		else
			act.removeLaborUse(c);
		//look up

	}

	/*********************************************************
	 * This function adds in material into the schedule for a
	 */
	public void updateBaselineDailyMaterialUse(Activity act, MaterialType m, int amt)
	{
		for(Activity a : getProject().getActivities())
		{
			if(a.getID() == act.getID())
			{
				for(ANode node : getANodeSet())
				{
					if(node.getOutPrimaryArc() != null)
					{
						PNode pnode = (PNode)node.getOutPrimaryArc().getHeadNode();
						if(pnode.getParentAct().getID() == act.getID())
						{
							int pamt;
							if(a.getMaterialUse().containsKey(m))
								pamt = a.getMaterialUse().get(m);
							else
								pamt = 0;

							double remainingdays = pnode.getTotalWorkLeft() / a.computeDailyMaterialCost(); 

							//set the new material use.
							a.setMaterialUse(amt, m);

							//add in the difference in material amounts
							pnode.setTotalWorkLeft(pnode.getTotalWorkLeft() + m.getCost() * remainingdays * (amt - pamt));
							return;
						}
					}
				}
			}
		}
	}

	/********************************************************
	 * This function will alter the String descriptor of the baseline.
	 */
	public void setBaselineDescription(Activity act, String description)
	{
		for(Activity a : getProject().getActivities())
		{
			if(a.getID() == act.getID())
			{
				a.setDescription(description);
				return;
			}
		}
	}

	/********************************************************
	 * This function will change the activity code string.
	 */
	public void setActivityCode(Activity act, String ActivityCode)
	{
		for(Activity a : getProject().getActivities())
		{
			if(a.getID() == act.getID())
			{
				a.setCode(ActivityCode);
				return;
			}
		}
	}

	/********************************************************
	 * This function will chance which CSIDivion an Activity is associated with
	 */
	public void setCSIDivision(Activity act, CSIDivision div)
	{
		for(Activity a : getProject().getActivities())
		{
			if(a.getID() == act.getID())
			{
				a.setDivision(div);
				return;
			}
		}
	}

	/********************************************************
	 * This function will change the Responsibility associated with an Activity
	 */
	public void setResponsibility(Activity act, Responsibility responsibility)
	{
		for(Activity a : getProject().getActivities())
		{
			if(a.getID() == act.getID())
			{
				a.setResponsibility(responsibility);
				return;
			}
		}
	}

	/********************************************
	 * Gets the first day of the baseline schedule
	 */
	public int getBaselineStart()
	{
		int min = getProject().getActivities()[0].getStart();
		for(Activity a : getProject().getActivities())
			min = Math.min(min, a.getStart());

		return min;
	}

	/********************************************
	 * Gets the last day of the baseline schedule
	 */
	public int getBaselineEnd()
	{
		int max = getProject().getActivities()[0].getEnd();
		for(Activity a : getProject().getActivities())
			max = Math.max(max, a.getEnd());

		return max;
	}

	/********************************************************
	 * END BASELINE MANIPULATION FUNCTIONS
	 */

	/********************************************************
	 * START EARNED VALUE CALCULATIONS
	 */

	/**
	 * Get the as-planned version of this activity
	 * 
	 */
	private Activity getAsPlanned(Activity a)
	{
		for(Activity act : this.getAsPlannedSched().getActivities())
		{
			if(a.getID() == act.getID())
				return act;
		}

		return null;
	}

	private Activity getBaseline(Activity a)
	{
		for(Activity act : getProject().getActivities())
		{
			if(a.getID() == act.getID())
				return act;
		}

		return null;
	}

	/*
	private Activity getBaseline(Activity a)
	{
		for(Activity act : this.getBaselineSched().getActivities())
		{
			if(a.getID() == act.getID())
				return act;
		}

		return null;
	}
	 */

	/**
	 * COST VARIANCE INDEX? // Comment needs review
	 * @param a
	 * @return
	 */
	public double CVI(Activity a)
	{
		double perc, mat, lab, ind;
		if(getCurrentTimeStep() == 1)
		{
			perc = 0;
			mat = 0;
			lab = 0;
			ind = 0;
		}
		else
		{
			perc = currentState.mathAgent.getAsBuiltProgress(getBaseline(a)).get(getCurrentTimeStep() - 1);
			mat = currentState.mathAgent.getAsBuilt().getMaterial(a, getCurrentTimeStep() - 1);
			lab = currentState.mathAgent.getAsBuilt().getLabor(a, getCurrentTimeStep() - 1);
			ind = currentState.mathAgent.getAsBuilt().getIndirect(a, getCurrentTimeStep() - 1);
		}

		if(perc == 0)
			return 0;
		return (mat + lab + ind) / (BAC(a) * perc) - 1.0;
	}

	/**
	 * ACTUAL COST OF WORK PERFORMED ()
	 * @param a Activity
	 * @return double cost of work alreay performend on the given activity
	 */
	public double AC(Activity a)
	{
		return currentState.mathAgent.getAsBuilt().getTotal(getBaseline(a), this);
	}

	/**
	 * BUDGET AT COMPLETION
	 * @param a Activity
	 * @return
	 */
	public double BAC(Activity a)
	{
		return getBaseline(a).getTotal();
	}

	/**
	 * COST PERFORMANCE INDEX
	 * @param a Activity
	 * @return The cost performance index of an activity
	 */
	public double CPI(Activity a)
	{
		double ac = AC(a);
		if(ac == 0)
			return 0;

		return EV(a) / ac;
	}

	/**
	 * COST VARIANCE
	 * @param a Activity
	 * @return The cost variance of an activity
	 */
	public double CV(Activity a)
	{
		return EV(a) - AC(a);
	}

	/**
	 * ESTIMATED COST AT COMPLETION
	 * @param a Activity
	 * @return the estimated cost of an activity at it's completion
	 */
	public double EAC(Activity a)
	{
		return BAC(a) / CPI(a);
	}

	/**
	 * ESTIMATE TO COMPLETE
	 * @param a Activity
	 * @return the estimated cost of completing an activity
	 */
	public double ETC(Activity a)
	{
		return EAC(a) - AC(a);
	}

	/**
	 * BUDGETED COST OF WORK PERFORMED (EARNED VALUE)
	 * @param a Activity
	 * @return the earned value of an activity in terms of the baseline schedule
	 */
	public double EV(Activity a)
	{
		a = getBaseline(a);		//WARNING! I was not sure if EV is based on the baseline or as-planned schedule
		//so I guessed the baseline schedule, since that makes the most sense to me
		//If this assumption is wrong, change this to a = getAsPlanned(a)
		TreeMap<Integer, Double> progress = currentState.mathAgent.getAsBuiltProgress(getBaseline(a));

		if(progress.containsKey(getCurrentTimeStep() - 1))
			return progress.get(getCurrentTimeStep() - 1) * a.getTotal();
		else
			return 0;
	}

	/**
	 * BUDGETED COST OF WORK SCHEDULED (PLANNED VALUE)
	 * @param a Activity
	 * @return the planed value of work scheduled, as in the budget
	 */
	public double PV(Activity a)
	{
		return getAsPlanned(a).getTotal();
	}

	/**
	 * SCHEDULE PERFORMANCE INDEX
	 * 
	 * Behaver undefined for activities with a planned value of 0
	 * possible division by 0
	 * 
	 * @param a Activity
	 * @return the Schedule performance index of an activity
	 */
	public double SPI(Activity a)
	{
		//no check for divide by 0 here because it doesn't make sense to have an activity with 0 planned cost....
		return EV(a) / PV(a);
	}

	/**
	 *  SCHEDULE VARIANCE INDEX? // programmer unsure this documentation needs review
	 * @param a Activity
	 * @return 
	 */
	public double SVI(Activity a)
	{
		if(a==null)
			return -1;
		double abperc = currentState.mathAgent.getAsBuiltProgress(getBaseline(a)).get(getCurrentTimeStep());
		TreeMap<Integer,Double> asPlanned =currentState.mathAgent.getAsPlannedProgress(getBaseline(a));
		double apperc;
		if (getCurrentTimeStep()>=getAsPlanned(a).getEnd())
			apperc=1;
		else if (asPlanned.containsKey(getCurrentTimeStep()))
			apperc = asPlanned.get(getCurrentTimeStep());
		else
		{
			System.out.println("Malfunction: as-planned labor use does not exist at time step "+getCurrentTimeStep()+" for activity "+a.getID()+". asumming the activity was already compleeted.");
			apperc=1.0;
		}

		if(apperc == 0)
			return 0;
		return abperc / apperc - 1.0;
	}

	/**
	 * SCHEDULE VARIANCE
	 * @param a Activity
	 * @return the Schedule Variance of an activity
	 */
	public double SV(Activity a)
	{
		return EV(a) - PV(a);
	}

	/**
	 * VARIANCE AT COMPLETION
	 * @param a Activity
	 * @return The variance at completion
	 */
	public double VAC(Activity a)
	{
		return BAC(a) - EAC(a);
	}

	/********************************************************
	 * END EARNED VALUE CALCULATIONS
	 */

	protected Set<String> constructStateSpace(String values[])
	{
		Set<String> statespace = new TreeSet<String>();

		for(int x = 0; x < values.length; x++)
			statespace.add(values[x]);

		return statespace;
	}

	private void initVariables()
	{
		Activity[] a = getProject().getActivities();
		rules = new Vector<Rule>();

		for(Variable v : getProject().getVariables())
			currentState.environment.addVariable(v, a);

		for(Rule r : getProject().getRules())
			rules.add(r);
	}

	//construct activities is called only once to build the activities
	//in the TONAE. Future fundamental changes to TONAE should alter
	//this function.
	private void constructActivities(Project plan)
	{
		Set<ANode> aNodeSet = currentState.getANodeSet();

		Activity[] activities = plan.getActivities();
		for(int x = 0; x < activities.length; x++)
		{
			//make a new anode for the start and end of each activitiy
			ANode start = new ANode();
			ANode end = new ANode();
			//if(debug)
			//System.out.println("Activity "+activities[x].getDescription()+" Starts "+activities[x].getStart());

			start.setLabel(activities[x].getLabel() + ": Start");
			start.setEarlyStart(activities[x].getStart());
			start.setParentAct(activities[x]);
			end.setLabel(activities[x].getLabel() + ": End");
			end.setEarlyStart(activities[x].getStart() + activities[x].getDuration());
			end.setParentAct(activities[x]);

			Arc arc = new Arc();
			arc.setTailNode(start);
			arc.setHeadNode(end);
			arc.setLabel(activities[x].getLabel() + ": Arc");
			arc.setLower(activities[x].getDuration());
			arc.setUpper(activities[x].getDuration() + MAXIMUM_DURATION);

			start.setOutPrimaryArc(arc);
			end.setInPrimaryArc(arc);

			//these penalty values need to come from the database eventually.
			arc.setPenaltyRate(10);
			arc.setPenaltyBase(0);

			aNodeSet.add(start);
			aNodeSet.add(end);
		}

		Constraint[] constraints = plan.getConstraints();
		for(int x = 0; x < constraints.length; x++)
		{
			ANode start = null;
			ANode end = null;

			Iterator<ANode> i = aNodeSet.iterator();
			while(i.hasNext())
			{
				ANode a = i.next();
				if(a.getParentAct() == constraints[x].getFrom())
				{
					if(start == null || start.getEarlyStart() < a.getEarlyStart())
						start = a;
				}
				if(a.getParentAct() == constraints[x].getTo())
				{
					if(end == null || end.getEarlyStart() > a.getEarlyStart())
						end = a;
				}
			}

			if(start == null || end == null)
			{
				System.out.println("This is a crisis scenario you are not trained to deal with");
				System.exit(1);
			}

			Arc carc = new Arc();
			carc.setTailNode(start);
			carc.setHeadNode(end);
			carc.setLabel(end.getLabel() + ": Constraint");
			carc.setLower(constraints[x].getDuration());
			if(constraints[x].isHardConstraint())
			{
				//upper and threshold are the same, thus no sop, thus hard constraint
				carc.setUpper(constraints[x].getDuration() + MAXIMUM_DURATION);
				carc.setThreshold(constraints[x].getDuration() + MAXIMUM_DURATION);
			}
			else
			{
				//upper and threshold are different, thus sop exists, thus soft constraint
				carc.setUpper(constraints[x].getDuration() + MAXIMUM_DURATION);
				carc.setThreshold(MAXIMUM_DURATION);
			}

			//this information needs to come from the database
			carc.setPenaltyRate(20);
			carc.setPenaltyBase(1);

			start.addOutConstraint(carc);
			end.addInConstraint(carc);
		}
	}

	// 'node' is a beginning A-Node
	private void initPresentNode(ANode node)
	{
		// Create the present node for this act
		PNode pNode = new PNode();			    
		Activity aTemp = node.getParentAct();

		// Indicate the parent 'Activity' construct of this P-Node
		pNode.setParentAct(aTemp);

		// Get references to the starting and ending A_Nodes of this activity
		ANode start = node;
		Arc currentArc = start.getOutPrimaryArc();

		// Construct two new arcs for the new present node
		// These will replace the original primary arc for the A-Nodes
		Arc newBefore = new Arc();
		Arc newAfter = new Arc();

		// Set the penalty information
		newBefore.setPenaltyRate(currentArc.getPenaltyRate());
		newBefore.setPenaltyBase(currentArc.getPenaltyBase());
		newAfter.setPenaltyRate(currentArc.getPenaltyRate());
		newAfter.setPenaltyBase(currentArc.getPenaltyBase());

		// The new before arcs from start -> pNode
		newBefore.setTailNode(start);
		newBefore.setHeadNode(pNode);

		// The new after arcs from pNode -> end
		newAfter.setTailNode(pNode);
		newAfter.setHeadNode(currentArc.getHeadNode());

		start.setOutPrimaryArc(newBefore);
		pNode.setInPrimaryArc(newBefore);
		pNode.setOutPrimaryArc(newAfter);
		currentArc.getHeadNode().setInPrimaryArc(newAfter);

		// The 'before' arc is going to represent the past/present
		newBefore.setLower(0);
		newBefore.setUpper(0);

		// The 'after' arc is going to represent the future
		newAfter.setLower(currentArc.getLower());
		newAfter.setUpper(currentArc.getUpper());

		// Indicate that this present node is resolved and active
		pNode.setEarlyStart(node.getEarlyStart());

		// Automatically set the label of the two arcs
		pNode.setLabel("Y-" + start.getLabel()); // For example, P-node for 'A1' is 'Y-A1'
		newBefore.setLabel();
		newAfter.setLabel();

	}

	/**
	 * Check all activities and find the ones that can run on this turn.
	 * Set them as active, and add them to the readyList.
	 * 
	 * @param currentProj - the current state.
	 */
	private void startActivities(TONAEState currentProj)
	{
		Iterator<ANode> i_aNodeSet = currentProj.getANodeSet().iterator();
		ANode currentNode;

		// Loop through all of the elements in the A-Nodes set
		while (i_aNodeSet.hasNext())
		{
			currentNode = i_aNodeSet.next();

			// Check to see if the node's early time is now, and check to 
			// ensure that the node is not already resolved. This block
			// only resolves starting A-Nodes. Ending A-Nodes are resolved
			// in the CalcRD block
			//			System.out.println(currentNode.getParentAct().getLabel()+" Start: "+currentNode.getEarlyStart()+" Time: "+currentProj.getTime()+" Resolution: "+currentNode.getTimeOfResolution());
			if((currentNode.getEarlyStart() == currentProj.getTime()) && (currentNode.getTimeOfResolution() == -1) && (currentNode.getOutPrimaryArc() != null))
			{
				// Resolve the A-Node to the current time
				currentNode.setTimeOfResolution(currentProj.getTime());

				// Activate the present node
				PNode pNode = (PNode)currentNode.getOutPrimaryArc().getHeadNode();
				pNode.setEarlyStart(currentProj.getTime());
				pNode.setTimeOfResolution(currentProj.getTime());
				pNode.setActive(true);

				currentProj.getReadyList().add(pNode);
			}
		}
	}

	/**
	 * Check all activities in the readylist, and if they are finished,
	 * set them as inactive and remove them from the readylist.
	 * 
	 * @param currentProj
	 */
	private void endActivities(TONAEState currentProj)
	{
		Iterator<PNode> i_readyList = currentProj.getReadyList().iterator();
		PNode currentNode;
		HashSet<PNode> victimSet = new HashSet<PNode>();

		while (i_readyList.hasNext())
		{
			currentNode = i_readyList.next();
			Arc out = currentNode.getOutPrimaryArc();

			// Check to see if we've reached the end of the arc
			//if (out.getLower() == 0)
			//plus 1 becase we are removing before we update
			if(out.getHeadNode().getEarlyStart() <= getCurrentTimeStep() + 1)
			{
				// Set the head end A-Node to a resolved state
				currentNode.setTimeOfResolution(currentProj.getTime());

				// Add this present node to our victim set
				victimSet.add(currentNode);

				// Deactivate this present node
				currentNode.setActive(false);
			}
		}

		// Now loop through the victim set and remove the inactive P-Nodes
		i_readyList = victimSet.iterator();
		while (i_readyList.hasNext())
		{
			currentNode = i_readyList.next();
			currentProj.getReadyList().remove(currentNode);
		}
	}

	/**
	 * For all active nodes (activities in the readylist)
	 * increment the present node.
	 * 
	 * Then increment the global pnode
	 * 
	 * @param currentProj
	 */
	private void incrementPresentNodes(TONAEState currentProj)
	{
		Iterator<PNode> i_readyList = currentProj.getReadyList().iterator();
		PNode currentNode;

		// Loop through and increment all the P-Nodes on the ready list
		while (i_readyList.hasNext())
		{
			currentNode = i_readyList.next();
			incrementPresentNode(currentNode);
		}

		// Now increment the global node
		incrementPresentNode(currentProj.getGlobal());
	}

	/**
	 * Helper method for incrementPresentNodes().
	 * Increments a pNode.
	 * 
	 * @param current
	 */
	private void incrementPresentNode(PNode current)
	{
		current.advance();
		// Increment the incoming arc upper and lower bounds
		if (current.getInPrimaryArc() != null)
		{
			current.getInPrimaryArc().setLower(current.getInPrimaryArc().getLower() + 1);
			current.getInPrimaryArc().setUpper(current.getInPrimaryArc().getUpper() + 1);
		}

		// Decrement the outgoing arc upper and lower bounds
		if (current.getOutPrimaryArc() != null)
		{
			current.getOutPrimaryArc().setLower(current.getOutPrimaryArc().getLower() - 1);
			current.getOutPrimaryArc().setUpper(current.getOutPrimaryArc().getUpper() - 1);
		}

		// Loop through and increment all the event arcs
		Iterator<Arc> i_eventArcs = current.getEvents().iterator();
		while (i_eventArcs.hasNext())
		{
			Arc arc = i_eventArcs.next();
			arc.setLower(arc.getLower() + 1);
			arc.setUpper(arc.getUpper() + 1);
		}
	}
	/*	
	private int getWeekendDays(int start, int end)
	{
		GregorianCalendar cal = (GregorianCalendar)getPlan().getProject().getDate().clone();
		start -= 1;
		end -= 1;
		cal.add(Calendar.HOUR, 24 * start);

		int weekendcount = 0;
		for(int x = 0; x < end - start; x++)
		{
			if(cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
				weekendcount++;
			cal.add(Calendar.HOUR, 24);
		}

		return weekendcount;
	}*/

	/**
	 * Delay an activity by a certain amount.
	 */
	public void delayActivity(Activity a, int delay)
	{
		for (ANode node:getANodeSet())
		{
			if (node.getParentAct().getID()==a.getID())
			{
				if(node.getOutPrimaryArc() != null)
				{
					PNode pnode = (PNode)node.getOutPrimaryArc().getHeadNode();
					delayActivity(pnode, delay);
				}
			}
		}
	}

	/**
	 * Helper method for delayActivity(Activity, int)
	 * Delays an activity by a certain amount. This handles negative delays properly. but not when before current date
	 * 
	 */
	public void delayActivity(PNode node, int delay)
	{	
		if(!querymode)
		{int a=2;}
		ANode start = (ANode)node.getInPrimaryArc().getTailNode();
		ANode end = (ANode)node.getOutPrimaryArc().getHeadNode();

		end.setEarlyStart(end.getEarlyStart() + delay);
		if(end.getEarlyStart() < getCurrentTimeStep()+1)
			end.setEarlyStart(getCurrentTimeStep()+1);
		if(end.getEarlyStart() < start.getEarlyStart())
			end.setEarlyStart(start.getEarlyStart());

		boolean finished = false;
		while(!finished)
		{
			finished = true;
			Iterator<ANode> iter = getANodeSet().iterator();
			while(iter.hasNext())
			{
				end = iter.next();
				//make sure this actually is an end node
				if(end.getOutPrimaryArc() == null)
				{
					//traverse back from the end anode to the pnode to the start anode
					start = (ANode)end.getInPrimaryArc().getTailNode().getInPrimaryArc().getTailNode();
					//make sure the activity hasn't started yet before moving it around
					if(start.getEarlyStart() > getCurrentTimeStep())
					{
						Set<Arc> constraints = start.getInConstraints();
						Iterator<Arc> citer = constraints.iterator();
						//make sure no activities get set to start before this latest day
						int latest = getCurrentTimeStep()+1;
						while(citer.hasNext())
						{
							Arc carc = citer.next();
							ANode lastEnd = (ANode)carc.getTailNode();
							boolean soft = carc.getUpper() != carc.getThreshold();

							//this is the case if the constraint is hard, or if the constraint is soft and the actual
							//activity is farther in the future than the constraint suggests
							int suggested = lastEnd.getEarlyStart() + carc.getLower();

							//this is the case if this activity starts before the previous ends on a soft constraint
							if(soft && end.getEarlyStart() < lastEnd.getEarlyStart())
								suggested = lastEnd.getEarlyStart();

							//this is the case if the delay is within the bounds of softness
							else if(soft && end.getEarlyStart() >= lastEnd.getEarlyStart() && end.getEarlyStart() < suggested)
								suggested = end.getEarlyStart();

							if(suggested > latest)
								latest = suggested;
						}

						//if something is to be changed
						if(start.getEarlyStart() != latest)
						{
							int duration = end.getEarlyStart() - start.getEarlyStart();
							start.setEarlyStart(latest);
							end.setEarlyStart(latest + duration);

							//something changed, must take another pass
							finished = false;
						}
					}
				}
			}
		}
	}

	/************************************
	 * Gets a distribution of future results
	 * 
	 * @param num : number of futures to query
	 * @param bucketsize : bucket size for returning the results
	 * @return a distribution
	 */
	public QueryResult2 queryFutures(int num)//, double bucketsize)
	{
		//System.out.println("Current thing.");
		//for(Entry<Integer, Double> e : currentState.mathAgent.getAsBuilt().getQueryFuturesTotal().entrySet()){
		//	System.out.println(e.getKey()+", "+e.getValue());
		//}
		TONAE.querymode = true;
		int numThreads = Simulator.queryThreads;
		//result = new QueryResult(bucketsize);
		result = new QueryResult2();
		QueryThread[] threads = new QueryThread[numThreads];
		//5 threads, 20 futures = 20/5 = 4 futures per thread
		//5 threads, 21 futures = 21/5 = 4 futures per thread
		int extra = (num % numThreads);
		for(int i=0; i<numThreads; i++){
			threads[i] = new QueryThread(this, this.currentState, asPlanned, asBuilt, rules, (num/numThreads));//, bucketsize);
			threads[i].start();
		}
		if(extra > 0){
			QueryThread x = new QueryThread(this, this.currentState, asPlanned, asBuilt, rules, extra);//, bucketsize);
			x.start();
			try {
				x.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		//Wait for threads
		for(QueryThread q : threads){
			try {
				q.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		//Make sure we have full futures
		int tries = 0;
		//System.out.println((num - result.numResults())+" results left");
		while(result.numResults() < num && tries < 100){
			tries++;
			int difference = (num - result.numResults());
			System.out.println(difference+" results left");
			QueryThread x = new QueryThread(this, this.currentState, asPlanned, asBuilt, rules, difference);//, bucketsize);
			x.start();
			try {
				x.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if(tries == 100) System.out.println("Tries timed out.");

		TONAE.querymode = false;

		//Notify Listeners...
		for(QueryResultListener r : querylisteners){
			r.onQueryFinished(result);
		}

		return result;
	}

	/**
	 * Indicates whether or not the project is finished
	 * 
	 * @return true if there are no more activities in this project, false otherwise
	 */
	public boolean isFinished()
	{
		return getCurrentTimeStep() >= getLastTimeStep();
	}

	/** 
	 * Returns the current date of the simulation, where the first day of the simulation is 1
	 * 
	 * @return the current date of the simulation
	 */
	public int getCurrentTimeStep()
	{
		return currentState.getTime();
	}

	/**
	 * Returns the last day of the schedule currently. If the simulation is at day 1, then this is the
	 * as planned last day. If the simulation is in the middle, then this is the last day as it is planned
	 * from the current as built time point
	 * @return
	 */
	public int getLastTimeStep()
	{
		//the ANode set contains the current set of all ANodes, which includes ending ANodes, which means
		//the latest ANode in aNodeSet will return the current last day
		Set<ANode> aNodeSet = currentState.getANodeSet();

		if(aNodeSet.size() == 0)
			return 0;

		int max = 0;
		Iterator<ANode> iter = aNodeSet.iterator();
		while(iter.hasNext())
		{
			ANode a = iter.next();
			if(a.getEarlyStart() > max)
				max = a.getEarlyStart();
		}

		return max;
	}

	public Environment getEnvironment()
	{
		return currentState.environment;
	}

	public Set<PNode> getReadyList()
	{
		return currentState.getReadyList();
	}

	public HashSet<ANode> getANodeSet()
	{
		return currentState.aNodeSet;
	}

	public void addRuleListener(RuleListener l)
	{
		rulelisteners.add(l);
	}

	public void addSpaceViolationListener(SpaceViolationListener w)
	{
		spaceViolationListener.add(w);
	}

	public void addLaborChangeListener(LaborChangeListener l)
	{
		laborChangeListener.add(l);
	}

	public void issueSpaceViolation(double spaceOccupied, double spaceAllowed, double deliveryspace, HashMap<MaterialType, Integer> delivery, Vector<ResourceAllocation> request)
	{
		for(SpaceViolationListener s : spaceViolationListener)
			s.spaceViolation(spaceOccupied, spaceAllowed, deliveryspace, delivery, request);
	}

	/**
	 * Find rules with the specified name and value in the list
	 * 
	 * @param varname - name of the rule to find
	 * @param value - value of the rule to find
	 * @param list - list to check for the rules
	 * @return - a hashmap of rules matching the search criteria
	 */
	private HashMap<Rule, HashSet<Activity>> findRule(String varname, String value, HashMap<Rule, HashSet<Activity>> list)
	{
		HashMap<Rule, HashSet<Activity>> map = new HashMap<Rule, HashSet<Activity>>();

		for(Entry<Rule, HashSet<Activity>> e : list.entrySet())
		{
			boolean exists = false;

			for(Condition co : e.getKey().getPostConditions())
			{
				if(co.varname.equals(varname) && co.dstate.equals(value))
					exists = true;
			}

			if(exists)
				map.put(e.getKey(), e.getValue());
		}

		return map;
	}

	/**
	 * Trigger all entries for a rule (once for each activity in the list)
	 * 
	 * @param rulename - name of the rule to look up
	 * @param value - value we're looking up...
	 * @param o - an object to pass along to the rulelistener (Missing Labor Container)
	 * @param list - the list of rules to check
	 */
	private void trigger(String rulename, String value, Object o, HashMap<Rule, HashSet<Activity>> list)
	{
		for(Entry<Rule, HashSet<Activity>> e : findRule(rulename, value, list).entrySet())
		{
			//Trigger the rule for all activities in e.getValue(), the hashset of activities
			e.getKey().trigger(rulelisteners, e.getValue(), o);
			list.remove(e.getKey());
		}
	}

	/**
	 * Trigger only for the specified activity (for all entries of the rule).
	 * 
	 * @param rulename - name of the rule to look up
	 * @param value - value to look up
	 * @param o - object to pass to ruleListener
	 * @param list - the list rules & activities
	 * @param act - the specific activity to trigger for
	 */
	private void trigger(String rulename, String value, Object o, HashMap<Rule, HashSet<Activity>> list, Activity act){
		for(Entry<Rule, HashSet<Activity>> e : findRule(rulename, value, list).entrySet())
		{
			HashSet<Activity> acts = new HashSet<Activity>();
			acts.add(act);
			e.getKey().trigger(rulelisteners, acts, o);
			list.get(e.getKey()).remove(act);
			if(e.getValue().size() == 0) list.remove(e.getKey());
		}
	}

	//TODO: This will be going away once we have a database infrastructure for mapping hired labor onto simulation variables. MW (8-25-08)
	private void checkLabor(Vector<ResourceAllocation> resourcerequest, LaborCrew[] crews, LaborCrew unmapped, LaborCrew hired, HashMap<Rule, HashSet<Activity>> rulelist)
	{
		boolean laborchanged = false;

		for(ResourceAllocation alloc : resourcerequest)
		{
			if(alloc.getActivity() != null)
			{
				//Each of these variables are local variables
				DiscreteV lab_avail = (DiscreteV)currentState.environment.getVariable(alloc.getActivity(), "Labor Available");
				DiscreteV lab_low = (DiscreteV)currentState.environment.getVariable(alloc.getActivity(), "Low Labor");

				if(lab_avail == null || lab_low == null) // restoration patch: scenario defines no labor-event variables for this activity
					continue;

				//Check if a global labor strike has occurred
				if(lab_avail.getState().equals("False"))
				{
					for(LaborCrew c : alloc.getActivity().getLaborUse())
					{
						for(LaborCrew o : crews)
						{
							if(c.getID() == o.getID())
							{
								o.clear();
								laborchanged = true;
							}
						}
					}

					trigger("Global Labor Strike", "True", null, rulelist);
				}

				//Check if Missing worker event has occurred
				else if(lab_low.getState().equals("True"))
				{
					//Create a list of all labortypes allocated to this activity
					ArrayList<LaborType> types = new ArrayList<LaborType>();
					for(LaborCrew c : alloc.getActivity().getLaborUse())
					{
						for(LaborCrew o : crews)
						{
							if(c.getID() == o.getID())
							{
								for(LaborType t : o.getTypes())
								{
									for(int x = 0; x < o.getAmt(t); x++)
										types.add(t);
								}
							}
						}
					}

					//Choose a laborer at random
					Random r = new Random();
					if(types.size() > 0){ //It is possible that the entire crew was fired
											//And thus no one can call in sick from that crew.
						LaborType t = types.get(r.nextInt(types.size()));
						boolean removed = false;

						//Remove that laborer.
						for(LaborCrew c : alloc.getActivity().getLaborUse())
						{
							for(LaborCrew o : crews)
							{
								if(c.getID() == o.getID() && !removed && o.remove(t) == 1)//removes the laborer
								{
									//Tell rulelistener which laborer was removed, from which crew
									trigger("Low Labor", "True", new MissingLaborContainer(o, t), rulelist, alloc.getActivity());
									//HashMap<LaborCrew, LaborType> map = new HashMap<LaborCrew, LaborType>();
									//map.put(o, t);
									//trigger("Low Labor", "True", map, rulelist);

									removed = true;
									laborchanged = true;
								}
							}
						}
					}
					else{//If no workers assigned to a crew (all were fired...) remove from the rulelist
						
						//System.out.println("No workers, so no one can call in sick!");
						lab_low.setState("False", 1);

						//Remove the activity from the list.
						for(Rule ru : rulelist.keySet()){
							if(ru.getName().equals("Missing Worker")){
								HashSet<Activity> list = rulelist.get(ru);
								//System.out.println("Activity "+alloc.getActivity().getDescription()+" removed from the RuleList");
								list.remove(alloc.getActivity());
								if(list.isEmpty())//Remove the rule if no activities left
									rulelist.remove(ru);
								break;
							}
						}
					}
				}
			}
		}

		if(laborchanged && this.laborChangeListener != null && !querymode)
		{
			for(LaborChangeListener l : laborChangeListener)
				l.laborChanged(crews, unmapped, hired);
		}
	}

	/**
	 * True Labor Checking
	 * 1. Compare old labor crews to the new labor crews
	 * 2. If any change has occurred, then laborAltered=true
	 * 3. Trigger the listener if any change was detected (laborAltered=true)
	 * 
	 * We are checking laborAltered for all activities, not specific activities
	 * 
	 * We must also update the variables associated with the LaborType for each Activity
	 * 
	 * NOTE: This method was separated from checkLabor() in order to have rules based on
	 * 		labor compliment take effect on the same day
	 */
	private void checkLaborCompliment(Vector<ResourceAllocation> resourcerequest, LaborCrew[] crews){

		boolean laborAltered = false;

		for(ResourceAllocation alloc : resourcerequest){
			if(alloc.getActivity() != null){
				Vector<LaborCrew> newCrews = new Vector<LaborCrew>();
				Vector<LaborCrew> oldCrews = new Vector<LaborCrew>();
				for(LaborCrew o : alloc.getActivity().getLaborUse()){ //The old labor crews
					for(LaborCrew n : crews){ //The new labor crews
						if(n.getID() == o.getID()){ //if "the same labor crew"
							if(n.getTypes().size() != o.getTypes().size()) laborAltered=true;
							else {
								for(LaborType t : o.getTypes()){
									if(n.getAmt(t) != o.getAmt(t)){
										laborAltered=true;
										if(!querymode){
											if(!newCrews.contains(n)){newCrews.add(n); oldCrews.add(o);}
											//											System.out.println(t.getDescription()+" changed from "+o.getAmt(t)+" to "+n.getAmt(t)+" for Activity ("+alloc.getActivity().getDescription()+").");
											//Update variable here
											//this.getEnvironment().getContinuousVariable(alloc.getActivity(), t.getDescription()).setState(n.getAmt(t), 1);
											for(DiscreteV var : this.getEnvironment().getDiscreteVariables(alloc.getActivity())){
												if(var.getLabel().equals(t.getDescription())){
													var.setState(n.getAmt(t)+"", 1);
												}
											}
										}
									}
								}
							}
						}
					}
				}
				if(!querymode && oldCrews.size() > 0)
					calculateCompliments(alloc.getActivity(), oldCrews, newCrews);
			}
		}
		if(!querymode && laborAltered){
			//if(laborAlteredListener != null)
			for(LaborAlteredListener l : laborAlteredListener)
				l.laborAltered();
		}
	}

	private void calculateCompliments(Activity act, Vector<LaborCrew> oldOnes, Vector<LaborCrew> newOnes){
		if(oldOnes.size() != newOnes.size()){ /*BAD DATAS*/ return;}


		LaborType theImportantOne = null;
		Iterator<LaborType> types = oldOnes.firstElement().getTypes().iterator();

		//Get the most important type of worker.
		//We assume that that worker is the one that occurs the most in the base crew
		int largest = 0;
		while(types.hasNext()){
			LaborType t = types.next();
			if(oldOnes.firstElement().getAmt(t) > largest){
				theImportantOne = t;
				largest = oldOnes.firstElement().getAmt(t);
			}
		}
		if(largest == 0 || theImportantOne == null){/*NO IMPORTANT WORKERS?*/ return;}

		/* Get the total amount */
		int base = oldOnes.firstElement().getAmt(theImportantOne);
		int total = 0;
		for(LaborCrew old : oldOnes){
			total = total + old.getAmt(theImportantOne);
		}

		/* Calculate the percentage that each crew provides */
		double[] portions = new double[oldOnes.size()];
		int j=0;
		for(LaborCrew n : newOnes){
			portions[j] = (double)n.getAmt(theImportantOne)/total;
			//			System.out.println("\tCrew "+n.getName()+" has "+portions[j]+" ("+(portions[j]*100)+"%)influence.");
			j++;
		}

		/* Calculate the partial compliment for each crew */
		double fullCompliment = (double)total/base;
		//		System.out.println("Total Labor Compliment for Activity "+act.getDescription()+": "+fullCompliment+" ("+total+"/"+base+").");
		for(j=0; j<portions.length; j++){
			portions[j] = fullCompliment * portions[j];
			//			System.out.println("\tCrew "+oldOnes.elementAt(j).getName()+" has compliment of "+portions[j]+".");
		}

		/* Next Step?? */
		/* Update the compliments for all the laborcrews */
		for(j=0; j<portions.length; j++){
			//this.getEnvironment().getContinuousVariable(act, newOnes.elementAt(j).getName()).setState(portions[j], 2);
			for(DiscreteV var : this.getEnvironment().getDiscreteVariables(act)){
				//				if(!querymode)System.out.println(var.getLabel() + " = " +newOnes.elementAt(j).getName());
				if(var.getLabel().equals(newOnes.elementAt(j).getName())){
					//We limit the discrete range to 0 - 4
					int amount=(int)portions[j];
					if(amount < 0) amount=0;
					else if(amount > 4) amount=4;
					var.setState(((int)portions[j])+"", 1); //Labor compliment remains for two days so that the rule can detect it.
					//					System.out.println("State of variable "+var.getLabel()+" set to "+var.getState());
				}
			}
		}

		return;
	}

	/**
	 * Updates a variable that tracks the current month
	 * The variable must be named Month
	 * The variable is discrete and possible values are the long names of the months
	 * 		January February March April May June July August September October November December
	 * Created as a feature request
	 */
	private void updateMonthVar(){
		DiscreteV var = (DiscreteV)this.getEnvironment().getGlobalVariable("Month");
		if(var != null)
			var.setState(this.currentState.currentDate.getDisplayName(GregorianCalendar.MONTH, GregorianCalendar.LONG, Locale.US), 1);
	}

	/**
	 * Updates a variable that tracks the current day of the month (integer value)
	 * The variable must be named Day
	 * The variable is discrete and global, and possible values range from
	 * 		1 (Calendar.SUNDAY) to 7 (Calendar.SATURDAY)
	 * Created as a feature request
	 * TODO: Test this method
	 */
	private void updateDayVar(){
		DiscreteV var = (DiscreteV)this.getEnvironment().getGlobalVariable("Day");
		if(var != null)
			var.setState(""+this.currentState.currentDate.get(Calendar.DAY_OF_WEEK), 1);
	}

	/**
	 * Checks to see whether or not the Material Available variable was altered,
	 * Likely due to a "No Material Delivery" event,
	 * and acts accordingly.
	 * 
	 * @param rulelist - List of triggered rules.
	 */
	private void checkMaterial(HashMap<Rule, HashSet<Activity>> rulelist, Vector<ResourceAllocation> request)
	{
		// restoration patch: DB variable is spelled "Material Available"; original code
		// looked up the misspelled "Material Available" and NPE'd on the second use.
		DiscreteV mat_avail = (DiscreteV)currentState.environment.getGlobalVariable("Material Available");
		if(mat_avail == null)
			return;
		if(mat_avail.getState().equals("False"))
			currentState.purchasedmaterial.clear();
		//Reset the PNode "ordered" variable
		if(mat_avail.getState().equals("False"))
		for(PNode p : this.getReadyList()){
			for(ResourceAllocation r : request){
				Activity act = r.getActivity();
				if(act != null && act.getID() == p.getParentAct().getID()){
					int duration = act.getDuration();
					double newOrder = p.getPercentOrdered() - r.getOrder()/duration;
					System.out.println("+++Setting order from "+p.getPercentOrdered()+" back to "+newOrder+" for "+act.getDescription());
					p.setOrdered(newOrder);
					break;
				}
			}
		}
	}

	/**
	 * Determines which crews should be assigned to which activity.
	 * Useful when two activities both need the same crew.
	 * 
	 * @param resourcerequest - resourceAllocations for the activities
	 * @param soldmaterial
	 * @param crews - list of available crews
	 * @param rulelist - list of triggered rules.
	 */
	private void buildRequestList(Vector<ResourceAllocation> resourcerequest, HashMap<MaterialType, Integer> soldmaterial, LaborCrew[] crews, HashMap<Rule, HashSet<Activity>> rulelist)
	{
		//sell material here by adding sold material to rejected material list
		for(Entry<MaterialType, Integer> e : soldmaterial.entrySet())
		{
			int amt = currentState.stock.remove(e.getKey(), e.getValue());
			currentState.rejectedmaterial.put(e.getKey(), amt);
		}

		for(ResourceAllocation alloc : resourcerequest)
		{
			if(alloc.getActivity() != null)
			{
				alloc.clearLaborRequest();
				for(LaborCrew c : alloc.getActivity().getLaborUse())
				{
					for(int x = 0; x < crews.length; x++)
					{
						if(crews[x] != null && crews[x].getID() == c.getID())
						{
							alloc.request(crews[x]);
							crews[x] = null;
						}
					}
				}
			}
		}
	}

	/**
	 * Determine the amount of materials we need. This is:
	 * 
	 * Amount_Requested - Amount_Already_In_Stock = Amount_To_Purchase
	 * 
	 * @param resourcerequest - requested resources per activity.
	 */
	private void buildPurchaceList(Vector<ResourceAllocation> resourcerequest)
	{
		Stock cs = currentState.stock.clone(); //clone the stock so we can remove from it
		for(ResourceAllocation alloc : resourcerequest)
		{
			if(alloc!=null&&alloc.getActivity() != null)
				for(MaterialType m : getProject().getMaterialTypes())
				{
					int amount = 0;

					if(alloc.getActivity().getMaterialUse().get(m) != null)
						amount = (int)Math.ceil(alloc.getActivity().getMaterialUse().get(m) * alloc.getOrder() / 100);
					//System.out.println("Ordering "+alloc.getOrder()+"% of "+m.getDescription());

					//version 1
					//int amt = cs.remove(m, alloc.getRequested(m));		//remove what we can from stock,
					//purchaceMaterial(m, /*alloc.getRequested(m)*/amount - amt);		//purchace the rest

					//version 2
					purchaceMaterial(m, amount);
				}
		}
	}

	/*	//This method was merged into the manageResources() method
    private void manageStock(Vector<ResourceAllocation> resourcerequest)
	{
		double spaceneeded = 0.0;
		for(Entry<MaterialType, Integer> e : currentState.purchacedmaterial.entrySet())
			spaceneeded += e.getValue() * e.getKey().getSize();

		double allowed = currentState.stock.getAvailableSpace();
		if(!querymode && allowed < spaceneeded)
			issueSpaceViolation(currentState.stock.getUsedSpace(), allowed, spaceneeded, currentState.purchacedmaterial);

		for(Entry<MaterialType, Integer> e : currentState.purchacedmaterial.entrySet())
		{
			int amt = e.getValue() - currentState.stock.add(e.getKey(), e.getValue());
			int camt = 0;
			if(currentState.rejectedmaterial.containsKey(e.getKey()))
				camt = currentState.rejectedmaterial.get(e.getKey());
			currentState.rejectedmaterial.put(e.getKey(), amt + camt);
		}

		for(ResourceAllocation alloc : resourcerequest)
		{
			for(MaterialType m : getProject().getMaterialTypes())
			{
				//don't grab materials out for the stock allocation!
				alloc.set(m, currentState.stock.remove(m, alloc.getRequested(m)));
			}
		}
	}
	 */
	//Create and return a default ResourceAllocation for the running activities (and stock)
	public Vector<ResourceAllocation> getDefaultResourceAllocation()
	{
		Vector<ResourceAllocation> alloc = new Vector<ResourceAllocation>();

		alloc.add(new ResourceAllocation(null));	//add an empty resource allocation for the stock

		//for each activity in the ready list
		for(PNode node : getReadyList())
		{
			Activity a = node.getParentAct();
			ResourceAllocation ralloc = new ResourceAllocation(a);
			for(Entry<MaterialType, Integer> e : a.getMaterialUse().entrySet())
				ralloc.request(e.getKey(), e.getValue());

			ralloc.setWorkDays(5);
			ralloc.setWorkHours(8);
			alloc.add(ralloc);
		}

		return alloc;
	}

	/**
	 * Get the "default" labor crews (those from the original plan)
	 * 
	 * @return - array of the default labor crews
	 */
	public LaborCrew[] getDefaultLaborCrews()
	{
		LaborCrew[] oldcrew = getProject().getLaborCrews();
		LaborCrew[] c;
		c = new LaborCrew[oldcrew.length];

		for(int x = 0; x < oldcrew.length; x++)
			c[x] = oldcrew[x].clone();

		return c;
	}

	//Queries a single future
	public TreeMap<Integer, Double> queryFuture()
	{
		TONAEState p = currentState;

		querymode = true;
		currentState = (TONAEState)Serializer.copy(currentState);

		while(!isFinished() && currentState.getTime() < 1000)
		{
			update(null, null, getDefaultResourceAllocation(), new HashMap<MaterialType, Integer>(), getDefaultLaborCrews(), new LaborCrew(0, "Unmapped"), new LaborCrew(0, "Hired"));
		}


		//currentState is now at the end of the project
		TONAEState completed = currentState;
		currentState = p;
		querymode = false;

		return completed.mathAgent.getAsBuilt().getQueryFuturesTotal();
	}

	/**
	 * Increments the project by one day. Updates the as-built schedule with new cost info
	 */
	public void update(DBConnection conn, QueryResult2 queryresults, Vector<ResourceAllocation> resourcerequest, HashMap<MaterialType, Integer> soldmaterial, LaborCrew[] crews, LaborCrew unmapped, LaborCrew hired)
	{
		//		int cd = getCurrentTimeStep() - 1;
		buildPurchaceList(resourcerequest);
		//currentState.environment.update(this, this.currentState.purchasedmaterial);
		currentState.environment.update(this.getProject(), this.getCurrentTimeStep(), this.getANodeSet(), this.currentState.purchasedmaterial);

		//build a hash map of rules that triggered, and the activities that they triggered on so that
		//later the GUI can be notified that an event occured.
		//allows for many events to accumulate
		/**
		 * calculate labor complement here or above to have rules that pertain to them take affect the same day
		 * also material variables
		 */

		//make a copy of crews, because crews gets destroyed
		LaborCrew[] crewlist = new LaborCrew[crews.length];
		for(int x = 0; x < crews.length; x++)
			crewlist[x] = crews[x];

		//update the DrivingMaterias
		checkDrivingMaterials(resourcerequest);

		//Calculate the labor compliment
		checkLaborCompliment(resourcerequest, crews);

		//Update the month variable before rule checking
		updateMonthVar();

		//Also Update the day variable
		updateDayVar();

		//Apply rules
		HashMap<Rule, HashSet<Activity>> rulemap = new HashMap<Rule, HashSet<Activity>>();
		for(Rule rule : rules)
		{
			HashSet<Activity> act = rule.apply(currentState.environment, this.getReadyList());
			if(act != null)
				rulemap.put(rule, act);
		}

		//Display Triggered Rules
		//System.out.println("Triggered Rules:");
		//for(Entry<Rule, HashSet<Activity>> e : rulemap.entrySet()){
		//	System.out.println("\t"+e.getKey().getName()+" ("+e.getValue().size()+")");
		//}

		//currentState.environment.updateValues(this, currentState.purchasedmaterial);
		currentState.environment.updateValues(currentState.purchasedmaterial);

		//Clear old triggered rules
		//The TONAE.ruleTriggered should update the tRules vector
		tRules.clear();

		//allow these functions to handle special rules
		checkLabor(resourcerequest, crews, unmapped, hired, rulemap); //This removes the special rules "Low Labor" and "Labor Available" from the rulemap
		buildRequestList(resourcerequest, soldmaterial, crews, rulemap);
		checkMaterial(rulemap, resourcerequest);

		/*************************************************************************
		 * 
		 * this is the end of updating, and the beginning of the "player's turn"
		 * to respond to events and errors. if you want to do any thing that involves
		 * the turn's totals, for example reading how much space is used by the
		 * Inventory, now is the time to do it.
		 * any effects, actions, debug out put, test events, data recording 
		 * ai replacement of the user, etc. should go between this 
		 * and the next gigantic comment block
		 * 
		 * -Corey Tebo 10/3/2009
		 * 
		 ************************************************************************/

		//notify GUI of any rules that triggered.
		for(Entry<Rule, HashSet<Activity>> e : rulemap.entrySet())
			e.getKey().trigger(rulelisteners, e.getValue(), null);

		//print useful information
		if (paperGant||fileGant) 
		{
			//printJunk();//Old function to print stuff
		}

		//if(!querymode && rec != null)
		//	rec.intermediatedecision(currentState.rejectedmaterial, crewlist, hired);

		/**************************************************************************
		 * 
		 * this is the end of the "player's turn" 
		 * everything after this modifies or cleans up the data structures
		 * based on the events above
		 * 
		 **************************************************************************/

		if(debugText){
			System.out.println("Purchased Materials:");
			for(Entry<MaterialType, Integer> e : currentState.purchasedmaterial.entrySet()){
				if(e.getValue() > 0)
					System.out.println("\t"+e.getKey().getDescription()+" : \t\t"+e.getValue());
			}
		}

		//Copy resoucerequest before managing resouces becuase recordturn() needs the original copy...
		//Vector<ResourceAllocation> resourceCopy = (Vector<ResourceAllocation>)Serializer.copy(resourcerequest);

		//manageResourcesOriginal(resourcerequest, hired); //Original stuff
		manageResources(resourcerequest, querymode); //New stuff to rid items from resourceAllocation

		if(debugText){
			System.out.println("Stock Materials:");
			for(Entry<MaterialType, Integer> e : currentState.stock.entrySet()){
				if(e.getValue() > 0)
					System.out.println("\t"+e.getKey().getDescription()+" : \t\t"+e.getValue());
			}
		}

		//record decisions to the database
		if(!querymode&&dbrecord){
			int cd = getCurrentTimeStep();// - 1;
			db.recordturn(conn, this, resourcerequest, usedMaterials, crewlist, currentState.stock.getUsedSpace(),
					currentState.mathAgent.getAsBuilt().getLaborTotal(cd, this),
					currentState.mathAgent.getAsBuilt().getMaterialTotal(cd, this),
					currentState.mathAgent.getAsBuilt().getIndirectTotal(cd, this), tRules,
					queryresults);//records results for the "previous turn"
			//Records today's resourcerequest, today's materials, today's rules, today's AsBuilt totals...
			//Records YESTERDAY's queryresults.
		}

		//To go in the below if statement
		printDailyTotals();
		printResourceUsage2(resourcerequest);

		//Print info on the old day!
		if(!querymode && paperGant){

		}

		//clear used materials
		usedMaterials.clear();

		// Now go through and remove finished activities
		endActivities(currentState);

		//increment the time
		currentState.setTime(currentState.getTime() + 1);

		//populate the ready list with the new day's activities
		startActivities(currentState);

		//perishable materials go away!
		currentState.stock.clearPerishable();
		currentState.rejectedmaterial.clear();
		currentState.purchasedmaterial.clear();
		currentState.currentDate.add(Calendar.HOUR, 24*getProject().getTimeFrame().getInterval());

		//no need to build the late schedule while querying
		if(!querymode&&dbrecord)
		{
			currentState.fsched = getLateSchedule();
			db.endrecord(conn);
		}

		/*if(!querymode && getCurrentDay() == 3)
			testFunc();
		 */

		//TODO: Should this be automatic, or should we force the GUI to update futures...
		//      I am leaning toward the GUI doing it.
		//Query Futures.
		queryFutures(Simulator.numFutures);//, Simulator.QUERY_FUTURES_QUANTINIZATION);
	}

	//Function to print Labor and Material use for running Activities
	//Old. Replaced with printResourceUsage2
	/*	private void printResourceUsage(){
		for(Activity a : this.getProject().getActivities()){
			if(getCurrentTimeStep()>=a.getStart()&&getCurrentTimeStep()<a.getEnd()){
				//HashSet<LaborCrew> newCrews = a.getLaborUse();
				//HashMap<MaterialType, Integer> newMaterials = a.getMaterialUse();

				System.out.println("\nActivity "+a.getID()+" - "+ a.getDescription()+": Day " + getCurrentTimeStep());

				//Print out labor for the activity
				System.out.println("\tLabor:");
				for(LaborCrew c : a.getLaborUse()){
					System.out.println("\t\t"+c.getName()+":");
					for(LaborType t : c.getTypes()){
						//System.out.println("\t\t\t"+t.getDescription()+": "+c.getAmt(t));
						System.out.printf("\t\t\t"+t.getDescription()+": %1d\n",c.getAmt(t));
					}
				}

				//Print out labor for the activity
				System.out.println("\n\tMaterials:");
				for(MaterialType t : a.getMaterialUse().keySet()){
					//System.out.println("\t\t"+t.getDescription()+": "+a.getMaterialUse().get(t));
					System.out.printf("\t\t"+t.getDescription()+": %9.2f\n", a.getMaterialUse().get(t));
				}
			}
		}
	}
	 */

	/**
	 * Print out the differences between As-Planned and As-Built Resource Usage
	 * -If we use more than as-planned, it will be positive, if we use less it will be negative
	 * -If we don't change the amount of resource used, 0 is output
	 * 
	 * NOTES: No way to get true as-planned sched info
	 * -Currently As-Planned is actually Baseline.
	 * 
	 * @param resourcerequest - Requested resources
	 */
	private void printResourceUsage2(Vector<ResourceAllocation> resourcerequest){

		System.out.println("\nResource Usage -----------------------------------------------");
		for(ANode anode : getANodeSet())
		{
			if(anode.getOutPrimaryArc() == null)
				continue;

			Activity a = anode.getParentAct();
			int abstart = anode.getEarlyStart();
			int abend = anode.getOutPrimaryArc().getHeadNode().getOutPrimaryArc().getHeadNode().getEarlyStart();
			int basestart = a.getStart();
			int baseend = a.getEnd();
			//			System.out.println(a.getDescription()+": ABS."+abstart+" ABE."+abend+" BS."+basestart+" BE."+baseend);
			//		for(Activity a : this.getProject().getActivities()){
			//			if(getCurrentTimeStep()>=a.getStart()&&getCurrentTimeStep()<=a.getEnd()){
			if(
					(abstart > getCurrentTimeStep() && basestart > getCurrentTimeStep()) || //activity not started
					(abend <= getCurrentTimeStep() && baseend <= getCurrentTimeStep())//activity already ended
			)
				continue;

			System.out.println("\nActivity "+a.getID()+" - "+ a.getDescription()+": Week " + getCurrentTimeStep()+" As-Planned Start: "+basestart+" As-Planned End: "+baseend);
			double productivity = getEnvironment().getContinuousVariable(a, "Productivity").getState();

			//Print out labor for the activity
			System.out.println("\tLabor:");
			for(LaborCrew c : a.getLaborUse()){
				System.out.println("\t\t"+c.getName()+":");
				System.out.println("\t\tLaborType\tAs-Built\tAs-Planned\tDifference");
				for(LaborType t : c.getTypes()){

					//int asPlanned = c.getAmt(t); //asPlanned is stored in Activity
					int asPlanned = a.getEnd()<=getCurrentTimeStep()? 0 : c.getAmt(t); //asPlanned is stored in Activity
					int asBuilt = 0;

					//asBuilt is stored in ResourceAllocation
					for(ResourceAllocation r : resourcerequest){
						if(r.getActivity() != null && r.getActivity().getID() == a.getID()){
							for(LaborCrew rc : r.getLaborCrews()){
								if(rc.getID() == c.getID())
									for(LaborType rt : rc.getTypes()){
										if(rt.getID() == t.getID()){
											asBuilt = (int) (rc.getAmt(rt)*productivity); //Kinda a cheap way to do it...
										}
									}
							}
						}
					}
					System.out.println("\t\t"+t.getDescription()+":\t"+asBuilt+"\t"+asPlanned+"\t"+(asBuilt - asPlanned));
				}
			}

			//Print out materials for the activity
			System.out.println("\n\tMaterials:");
			System.out.println("\t\tMaterial\tAs-Built\tAs-Planned\tDifference");
			for(MaterialType t : a.getMaterialUse().keySet()){
				//int asPlanned = a.getMaterialUse().get(t);
				int asPlanned = a.getEnd()<=getCurrentTimeStep()? 0 : a.getMaterialUse().get(t);
				int asBuilt = 0;

				for(ResourceAllocation r : resourcerequest){
					if(r.getActivity() != null && r.getActivity().getID() == a.getID()){
						if(usedMaterials.get(r.getActivity()).containsKey(t))	
							asBuilt = usedMaterials.get(r.getActivity()).get(t); //used materials (as-built)
						else
							asBuilt = 0;//System.out.println("This Material Was Removed....");

					}
				}
				System.out.println("\t\t"+t.getDescription()+":\t"+asBuilt+"\t"+asPlanned+"\t"+(asBuilt - asPlanned));
			}
			//			}
		}
		System.out.println("--------------------------------------------------------------\n");
	}

	/**
	 * 
	 * @param resourcerequest
	 */
	private void checkDrivingMaterials(Vector<ResourceAllocation> resourcerequest) 
	{

		for (Activity a: asBuilt.getActivities())//TODO: should access only current activities
		{
			int min=50000;

			if(a.getDrivingMaterials().isEmpty())
				min=100;
			for(int dm: a.getDrivingMaterials())
			{
				for (ResourceAllocation r:resourcerequest)
				{

					//Make sure the resourceAllocation is for this activity
					if(a.equals(r.getActivity())){

						//get materialType for dm
						for(MaterialType matType:asBuilt.getMaterialTypes())
						{
							//if(!querymode)
							//System.out.println("Material "+matType.getID()+" != "+dm);
							if (a.getMaterialUse().get(matType) != null && matType.getID()==dm) //Make sure the activity uses the material
							{

								//get purchased amount for dm
								int available = r.getRequested(matType);

								//get baseline amount for dm
								int base = (a.getMaterialUse().get(matType));

								int temp = base==0? 0 : (available/base)*100;

								//								if(!querymode)
								//									System.out.println("Setting temp to "+available+"/"+base+" * 100 = "+temp);
								if (temp < min)
									min = temp;
							}
						}
						break;
					}
				}
			}
			//update variable
			//			if(!querymode)
			//				System.out.println("Setting driving material to "+(min/10)+" for activity "+a.getLabel());
			DiscreteV var = (DiscreteV) getEnvironment().getVariable(a, "Driving Material Available");
			if (var == null) // restoration patch: projects without driving materials never define this variable
				continue;
			var.setState(""+(min/10), 1);
		}

	}

	/*****************************************
	 * Save will save the state of the TONAE to a file, specified by "path"
	 * This save method uses Java Serialization, which means the following must be true:
	 * 
	 * The saved file can only be loaded into a JRE with the same version under which it was saved! (i.e. no saving under 1.4.2 and loading under 1.5)
	 * The saved file can only be loaded into the same OS under which it was saved (no saving in Windows and loading in Linux)
	 * The saved file will break if any state variables are added to the program. So, if
	 * 		the file was saved under TONAE v. 1.0 and then loaded under TONAE v. 1.1, then this will
	 * 		only work if the only difference between the versions are code differences, no new state variables.
	 * 
	 * @param path		path to save at
	 */
	public void save(File path)
	{
		ArrayList<RuleListener> rl = rulelisteners;
		ArrayList<SpaceViolationListener> sl = spaceViolationListener;
		ArrayList<LaborChangeListener> ll = laborChangeListener;

		rulelisteners = new ArrayList<RuleListener>();
		spaceViolationListener = new ArrayList<SpaceViolationListener>();
		laborChangeListener = new ArrayList<LaborChangeListener>();

		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try
		{
			fos = new FileOutputStream(path);
			out = new ObjectOutputStream(fos);
			out.writeObject(this);
			out.close();
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}

		rulelisteners = rl;
		spaceViolationListener = sl;
		laborChangeListener = ll;
	}

	/*****************************************
	 * Load will load the state of the TONAE from a file, specified by "path"
	 * This load method uses Java Serialization, which means the following must be true:
	 * 
	 * The saved file can only be loaded into a JRE with the same version under which it was saved! (i.e. no saving under 1.4.2 and loading under 1.5)
	 * The saved file can only be loaded into the same OS under which it was saved (no saving in Windows and loading in Linux)
	 * The saved file will break if any state variables are added to the program. So, if
	 * 		the file was saved under TONAE v. 1.0 and then loaded under TONAE v. 1.1, then this will
	 * 		only work if the only difference between the versions are code differences, no new state variables.
	 * 
	 * @param path		path to load from
	 * @throws IOException 
	 */
	public static TONAE load(File path) throws IOException
	{
		TONAE tonae;
		FileInputStream fis = null;
		ObjectInputStream in = null;
		fis = new FileInputStream(path);
		in = new ObjectInputStream(fis);

		try
		{
			tonae = (TONAE)in.readObject();
			tonae.addRuleListener(tonae);
		}
		catch(ClassNotFoundException e)
		{
			throw new IOException("The specified file was not a valid save file!");
		}

		in.close();

		return tonae;
	}

	/*****************************************************************
	 * This computes the resource loaded commodity curve. This curve is a
	 * plot of time (x axis) vs. space needed (y axis)
	 * 
	 * @author Matt Watkins
	 */

	public TreeMap<Integer, Double> getResourceLoadedCommodityCurve()
	{
		TreeMap<Integer, Double> curve = new TreeMap<Integer, Double>();

		for(int x = 1; x <= getLastTimeStep(); x++)
		{
			double amt = 0;
			for(ANode node : getANodeSet())
			{
				if(node.getOutPrimaryArc() != null)
				{
					Activity parent = node.getParentAct();
					Node endnode = node.getOutPrimaryArc().getHeadNode().getOutPrimaryArc().getHeadNode();
					int end = endnode.getEarlyStart();
					int start = node.getEarlyStart();
					if(x >= start && x < end)
					{
						for(Entry<MaterialType, Integer> e : parent.getMaterialUse().entrySet())
							amt += e.getValue() * e.getKey().getSize();
					}
				}
			}
			curve.put(x, amt);
		}

		return curve;
	}

	/*****************************************************************
	 * This computes the resource loaded labor curve. This curve is a
	 * plot of time (x axis) vs. labor crew use (y axis)
	 * 
	 * @author Matt Watkins
	 */

	public HashMap<LaborCrew, TreeMap<Integer, Integer>> getResourceLoadedLaborCurve()
	{
		HashMap<LaborCrew, TreeMap<Integer, Integer>> tcurve = new HashMap<LaborCrew, TreeMap<Integer, Integer>>();
		for(LaborCrew c : getProject().getLaborCrews())
		{
			TreeMap<Integer, Integer> map = new TreeMap<Integer, Integer>();

			for(int x = 1; x <= getLastTimeStep(); x++)
			{
				int amt = 0;
				for(ANode node : getANodeSet())
				{
					if(node.getOutPrimaryArc() != null)
					{
						Activity parent = node.getParentAct();
						Node endnode = node.getOutPrimaryArc().getHeadNode().getOutPrimaryArc().getHeadNode();
						int end = endnode.getEarlyStart();
						int start = node.getEarlyStart();
						if(x >= start && x < end)
						{
							for(LaborCrew nc : parent.getLaborUse())
							{
								if(nc.getID() == c.getID())
									amt++;
							}
						}
					}
				}
				map.put(x, amt);
			}

			tcurve.put(c, map);
		}

		return tcurve;
	}

	/************************************************
	 * This computes the early start for each activity
	 * 
	 */
	public HashMap<Activity, Integer> getEarlyStart()
	{
		//schedule is stored in terms of early start and late start, so this is just returning the schedule
		HashMap<Activity, Integer> time = new HashMap<Activity, Integer>();

		for(ANode node : getANodeSet())
		{
			if(node.getOutPrimaryArc() != null)
				time.put(node.getParentAct(), node.getEarlyStart());
		}

		return time;
	}

	public int getEarlyStart (Activity a)
	{
		for(ANode node : getANodeSet())
		{
			if(node.getOutPrimaryArc() != null && node.getParentAct().getID()== a.getID())
				return node.getEarlyStart();
		}
		throw new Error("internal error :56566");
	}

	/************************************************
	 * This computes the early finish for each activity
	 * 
	 */
	public HashMap<Activity, Integer> getEarlyFinish()
	{
		//schedule is stored in terms of early start and late start, so this is just returning the schedule
		HashMap<Activity, Integer> time = new HashMap<Activity, Integer>();

		for(ANode node : getANodeSet())
		{
			if(node.getOutPrimaryArc() == null)
				time.put(node.getParentAct(), node.getEarlyStart());
		}

		return time;
	}

	public int getEarlyFinish (Activity a)
	{
		for(ANode node : getANodeSet())
		{
			if(node.getOutPrimaryArc() == null && node.getParentAct().getID()== a.getID())
				return node.getEarlyStart();
		}
		throw new Error("internal error :56567");
	}
	/************************************************
	 * This computes the late start for each activity
	 * 
	 */
	public HashMap<Activity, Integer> getLateStart()
	{
		HashMap<Activity, Integer> time = new HashMap<Activity, Integer>();

		for(Entry<Activity, Entry<Integer, Integer>> e : currentState.fsched.entrySet())
			time.put(e.getKey(), e.getValue().getKey());

		return time;
	}

	public int getLateStart(Activity a)
	{
		return currentState.fsched.get(a).getKey();
	}

	/************************************************
	 * This computes the late finish for each activity
	 */
	public HashMap<Activity, Integer> getLateFinish()
	{
		HashMap<Activity, Integer> time = new HashMap<Activity, Integer>();

		for(Entry<Activity, Entry<Integer, Integer>> e : currentState.fsched.entrySet())
			time.put(e.getKey(), e.getValue().getValue());

		return time;
	}

	public int getLateFinish(Activity a)
	{
		return currentState.fsched.get(a).getValue();
	}

	/***********************************************
	 * Computes the late schedule. Automatically called by update
	 * 
	 * @return the late schedule
	 */
	public HashMap<Activity, Entry<Integer, Integer>> getLateSchedule()
	{
		int lastday = this.getLastTimeStep();

		HashMap<Activity, Entry<Integer, Integer>> sched = new HashMap<Activity, Entry<Integer, Integer>>();

		//keep track of the minimum start date
		int minstart = 0;
		while(sched.size() != getProject().getActivities().length)
		{
			boolean changed = false;

			for(ANode anode : getANodeSet())
			{
				if(anode.getOutPrimaryArc() != null && !sched.containsKey(anode.getParentAct()))
				{
					ANode start = anode;
					ANode end = (ANode)anode.getOutPrimaryArc().getHeadNode().getOutPrimaryArc().getHeadNode();

					int min = lastday;
					boolean noadd = false;
					for(Constraint c : start.getParentAct().getConstraints())
					{
						if(sched.containsKey(c.getTo()))
						{
							Entry<Integer, Integer> e = sched.get(c.getTo());
							min = Math.min(min, e.getKey() - c.getDuration());
						}
						else
							noadd = true;
					}

					if(!noadd)
					{
						int stime = min - (end.getEarlyStart() - start.getEarlyStart());
						minstart = Math.min(minstart, stime);
						if(start.getEarlyStart() <= currentState.t_now)
							stime = start.getEarlyStart();
						if(end.getEarlyStart() <= currentState.t_now)
							min = end.getEarlyStart();
						sched.put(start.getParentAct(), new TONAEEntry(stime, min));

						changed = true;
					}
				}
			}

			if(!changed)
				throw new Error("FATAL INTERNAL ERROR WHILE COMPUTING LATE SCHEDULE");
		}

		return sched;
	}

	/*
	METHODS TAKEN FROM SCHEDULECALCULATOR
	###################################################################################################################### */

	//gets the resource allocation from the work performed list
	public static ResourceAllocation getResourceAllocation(Activity act, Vector<ResourceAllocation> workperformed)
	{
		//		System.out.println(workperformed.size()+" allocs in the list.");
		for(ResourceAllocation alloc : workperformed)
		{
			if(alloc.getActivity() != null)
			{
				if(alloc.getActivity().getID() == act.getID())
					return alloc;
			}
		}

		return null;
	}

	//computes the percentage of work on an activity which can be completed, based on the available labor
	//requires work hours/days/wageincentive, laboruse, and crew allocation
	public double computeWorkQuantityMultiplier(HashSet<LaborCrew> laboruse, HashSet<LaborCrew> alloc, int hours, int days, double incentive)
	{
		double work = -1;
		double constent=2;

		if (alloc==null || laboruse == null)
		{
			return 0;
		}

		double hourfactor = hours * days/ 40.0;
		double wagefactor = constent-(constent-1.0)/incentive;

		if(hourfactor > 1)
		{
			//overtime work is only half as productive
			hourfactor -= 1;
			hourfactor *= .5;
			hourfactor += 1;
		}
		for(LaborCrew c : laboruse)
		{
			for(LaborCrew o : alloc)
			{
				if(c.getID() == o.getID())
				{
					double perc = c.compareProductivity(o);
					if(perc > 1)
					{
						//congestion causes excess work to be 80% as efficient.
						perc -= 1;
						perc *= .8;
						perc += 1;
					}

					perc *= hourfactor;

					if(work == -1 || perc < work)
						work = perc;
				}
			}
		}

		int interval = getProject().getTimeFrame().getInterval();
		int dayofweek = getCalendar().get(Calendar.DAY_OF_WEEK);

		if(interval == 1)
		{
			if((dayofweek == Calendar.SATURDAY && days <= 5) || (dayofweek == Calendar.SUNDAY && days <= 6))
				return 0;
		}

		if(work == -1)
			return 0;
		else
			return work*wagefactor;
	}

	//computes the percentage of work on an activity which can be completed, based on available materials
	//requires materialuse, available material allocation
	/*	private double computeMaterialQuantityMultipler(ResourceAllocation alloc)
	{
		double mat = -1;

		if(alloc.getActivity() == null) return 0;

		for(Entry<MaterialType, Integer> e : alloc.getActivity().getMaterialUse().entrySet())
		{
			double perc = (double)alloc.getAvailable(e.getKey()) / (double)e.getValue();
			if(mat == -1 || perc < mat)
				mat = perc;
		}

		//work is -1 if the activity doesn't require labor. Shouldn't ever happen, but just in case it does...
		if(mat == -1)
			return 0;
		else
			return mat;
	}
	 */	
	/**
	 * Function computes work done per activity per day quantized to units of 1% of complete.
	 *
	 */
	/*	public void computeWorkPerformed(Vector<ResourceAllocation> workperformed, boolean querymode)
	{
		for(PNode act : getReadyList())
		{
			ResourceAllocation alloc = getResourceAllocation(act.getParentAct(), workperformed);
			double baseworkrate = computeWorkQuantityMultiplier(act.getParentAct().getLaborUse(), alloc.getLaborCrews(), alloc.getWorkHours(), alloc.getWorkDays(), alloc.getWageIncentive());
			double materialrate = computeMaterialQuantityMultipler(alloc);

			ContinV productivity = (ContinV)getEnvironment().getContinuousVariable(act.getParentAct(), "Productivity");

			double workrate = baseworkrate * productivity.getState();

			double rate = workrate;
			if(materialrate < rate)
				rate = materialrate;

			//Compute the amount of work done... Done in percentages
			for(Entry<MaterialType, Integer> e : act.getParentAct().getMaterialUse().entrySet())
			{
				int baseuse = e.getValue();
				int amtused = alloc.removeAvailable(e.getKey(), (int)(baseuse * rate));
				act.setTotalWorkLeft(act.getTotalWorkLeft() - e.getKey().getCost() * (double)amtused);
			}

			//Compute remaining duration... In days not percentages.

			//base work is the expected amount done by the as-planned compliment of laborers
			double base_work = act.getParentAct().computeDailyMaterialCost();

			double unaltednewdir = (act.getTotalWorkLeft() / base_work) - .01;//subtract 1 percent to quantize productivity, and condition the floating point numbers to round properly

			//new duration is the duration after all the work has been done.
			int new_duration = (int)Math.ceil(unaltednewdir);

			//current duration is the number of days left as represented in the TONAE
			int current_duration = act.getOutPrimaryArc().getHeadNode().getEarlyStart() - act.getEarlyStart();

			//under ordinary circumstances, new_duration = current_duration - 1 (because one day
			//worth of work was done). So add 1 to get what the delay should be.
			int delay = new_duration - current_duration + 1;

			//No longer a factor because we add 1 to delay (above)
			//if day is negative something is seriously wrong
			//if(delay<0)
				//throw(new Error("negitive delay error"));

			//no need to call delay if it is 0
			if(delay != 0)
				delayActivity(act, delay);
		}
	}
	 */	
	/* ######################################################################################################################
	END OF METHODS TAKEN FROM SCHEDULECALCULATOR
	 */

	/**
	 * enables or disable a a transcript of the cost of activities per time period to the console 
	 * @param bool 
	 */
	public void setPaperGant(boolean bool)
	{
		paperGant=bool;
	}
	/**
	 * enables or disable a a transcript of the cost of activities per time period to the file 
	 * @param bool 
	 */
	public void setFileGant(boolean bool)
	{
		fileGant=bool;
	}

	/**
	 * Set weather or not to record to the database
	 * @param bool
	 */
	/*	public void setDBRecord(boolean bool)
	{
		dbrecord=bool;
	}
	 */

	//Tell the turn timer to start...
	public void signalTimerStart(){
		timeStart = System.currentTimeMillis();
	}

	//Tell the turn timer to end, and print out the time the turn took
	public void signalTimerEnd(){
		System.out.println("Turn took " + ((System.currentTimeMillis() - timeStart)/1000) + " seconds.");
	}

	public void addLaborAlteredListener(LaborAlteredListener l) {
		laborAlteredListener.add(l);
	}

	public void addQueryResultListener(QueryResultListener r){
		querylisteners.add(r);
	}

	/*	private void manageResourcesOriginal(Vector<ResourceAllocation> resourcerequest, LaborCrew hired){
		manageStock(resourcerequest);

		//add stock tracking before calcRDing, because calcRD removes material from Stock
		if(!querymode)
			currentState.stock_track.add(currentState.stock.clone());

		//Do all calculations
		computeWorkPerformed(resourcerequest, querymode);

		incrementPresentNodes(currentState);

		//here the simulation computes the cost of the schedule + changes from the user.
		currentState.mathAgent.computeCost(resourcerequest, hired, this);

		for(ResourceAllocation alloc : resourcerequest)
		{
			for(MaterialType m : getProject().getMaterialTypes())
				currentState.stock.add(m, alloc.getAvailable(m));
		}
	}
	 */	
	//Big Test Method
	private void manageResources(Vector<ResourceAllocation> resourcerequest, boolean querymode)
	{
		/* PER PROJECT STUFF */
		int interval = getProject().getTimeFrame().getInterval();
		int dayofweek = getCalendar().get(Calendar.DAY_OF_WEEK);
		double cost = 0;
		CostSchedule sched = getMathAgent().getAsBuilt();
		int day = getCurrentTimeStep();

		//Stock Tracking
		if(!querymode)
			currentState.stock_track.add(currentState.stock.clone());

		//Determine if we have enough space
		double spaceneeded = 0.0;
		for(Entry<MaterialType, Integer> e : currentState.purchasedmaterial.entrySet())
			spaceneeded += e.getValue() * e.getKey().getSize();

		//if not enough space, issue space violation
		double allowed = currentState.stock.getAvailableSpace();
		if(!querymode && allowed < spaceneeded)
			issueSpaceViolation(currentState.stock.getUsedSpace(), allowed, spaceneeded, currentState.purchasedmaterial, resourcerequest);

		//Reject extra materials
		for(Entry<MaterialType, Integer> e : currentState.purchasedmaterial.entrySet())
		{
			//put purchased materials into stock.
			int amt = e.getValue() - currentState.stock.add(e.getKey(), e.getValue());//amt is the amount that couldn't be added to the stock.
			int camt = 0;
			if(currentState.rejectedmaterial.containsKey(e.getKey()))
				camt = currentState.rejectedmaterial.get(e.getKey());// camt is the current amount in the rejected list
			currentState.rejectedmaterial.put(e.getKey(), amt + camt);//reject what doesn't "fit" in our current space
		}

		/* ALL ACTIVITY STUFF */
		//set today's cost here
		if(!querymode){
			for(Activity a : getProject().getActivities())
			{
				sched.setMaterial(a, day, sched.getMaterial(a, day - 1));
				sched.setLabor(a, day, sched.getLabor(a, day - 1));
				sched.setIndirect(a, day, sched.getIndirect(a, day - 1));
			}
		}

		/* PER ACTIVITY/RA STUFF */
		for(PNode act : getReadyList()){

			//1. Compute work rate
			ResourceAllocation alloc = getResourceAllocation(act.getParentAct(), resourcerequest);//Get RA for this activity
			if(alloc == null){ System.out.println("Null Alloc for "+act.getParentAct().getLabel()); continue;}
			double baseworkrate = alloc.computeWorkQuantityMultiplier(interval, dayofweek);
			//double materialrate = alloc.computeMaterialQuantityMultipler(); //Relies on getAvailable amount **********
			double materialrate = -1;
			double materialrate2 = 0;//new, experimental materialrate calc
			double dailyCost = 0;
			
			ContinV productivity = (ContinV)getEnvironment().getContinuousVariable(act.getParentAct(), "Productivity");
			double workrate = baseworkrate * productivity.getState();

			HashMap<MaterialType, Integer> avail = new HashMap<MaterialType, Integer>();

			//Debug Text to print out the current state of Stock (before allocating material to the activity)
			if(debugText){
				System.out.println("Stock Materials:");
				for(Entry<MaterialType, Integer> e : currentState.stock.entrySet()){
					if(e.getValue() > 0)
						System.out.println("\t"+e.getKey().getDescription()+" : \t\t"+e.getValue());
				}
			}

			//2. Compute Material Rate
			if(debugText) System.out.println("Available Materials:");
			for(Entry<MaterialType, Integer> e : act.getParentAct().getMaterialUse().entrySet())
			{
				//int amount = (int)Math.ceil(act.getParentAct().getMaterialUse().get(e.getKey()) * alloc.getOrder() / 100);//amount we want to order
				//2.1 Remove requested materials from stock
				int available = currentState.stock.remove(e.getKey(), alloc.getRequested(e.getKey())); //put Amount in Available
				avail.put(e.getKey(), available);
				if(debugText) System.out.println(e.getKey().getDescription()+" : \t\t"+available+" Requested: "+alloc.getRequested(e.getKey()));

				//2.2 Compute material rate
				double perc = (double)available / (double)e.getValue();
				if(materialrate == -1 || perc < materialrate)
					materialrate = perc;
				
				materialrate2 += available * e.getKey().getCost();
				dailyCost += e.getValue() * e.getKey().getCost();
			}
			materialrate2 /= dailyCost;
			
			double newRate = (materialrate2 < workrate)? materialrate2 : workrate;
			double materialcost = newRate * dailyCost;
			
			if(debugText){
				System.out.println("New MaterialRate: "+materialrate2);
				System.out.println("Must consume $"+materialcost+" of materials.");
			}
			
			if(materialrate == -1) materialrate = 0;

			//Debug Text to print the material rate for the activity
			if(debugText)System.out.println("MaterialRate is "+ materialrate);
			double rate = workrate;
			if(materialrate < rate)
				rate = materialrate;
			if(debugText) ; System.out.println("The rate for "+act.getParentAct().getDescription()+" is "+rate);
			System.out.println("New Rate is: "+newRate);

			//3. Material Computations
			HashMap<MaterialType, Integer> map = new HashMap<MaterialType, Integer>();
			usedMaterials.put(act.getParentAct(), map);
			for(Entry<MaterialType, Integer> e : act.getParentAct().getMaterialUse().entrySet())
			{
				int available = avail.get(e.getKey());

				//Try removing RATE percent of each material
				MaterialInfo info1 = act.getParentAct().getMaterialInfo(e.getKey());
				int newAmt = (int)Math.ceil(e.getValue() * newRate);
				//Don't remove more than we need
				if( (newAmt - (info1.getTotalNeed()-info1.getTotalUsed())) > 0 )
					newAmt = info1.getTotalNeed() - info1.getTotalUsed();
				//Don't remove more than available
				newAmt = (newAmt > available)? available : newAmt;
				//Don't remove more than the activity has allocated to itself
				int test = act.getOrderedAmount(e.getKey()) - info1.getTotalUsed();
				if(newAmt > test) newAmt = test;
				
				if(debugText){
					System.out.println(act.getParentAct().getDescription()+" to use "+newAmt+" of "+e.getKey().getDescription());
					System.out.println("\tAvailable(Alloc): "+available+" Ordered: "+test+" Used: "+info1.getTotalUsed()+" Req: "+info1.getTotalNeed()+" Need: "+(info1.getTotalNeed()-info1.getTotalUsed()));
				}
					
				if(newAmt > 0){
					//Here we would:
					// 1.remove from stock
					available = available - newAmt;
					// 2.set total work left
					act.setTotalWorkLeft(act.getTotalWorkLeft() - e.getKey().getCost() * (double)newAmt);
					// 3.add to the usedmaterials list
					map.put(e.getKey(), newAmt);
					// 4.store used materials in materialinfo
					info1.addUse(newAmt);
					// 5.do cost computation
					sched.addMaterial(act.getParentAct(), day, newAmt * e.getKey().getCost()); //here as well
					cost += newAmt * e.getKey().getCost();
					// 7.remove the cost from materialcost
					materialcost -= newAmt * e.getKey().getCost();
				}
				if(available > 0){
					if(available > test) available = test;
					if(debugText) System.out.println("-----> Putting "+available+" back into stock.");
					// 6.put extra back into stock
					currentState.stock.add(e.getKey(), available);
				}
				// 8.put available into avail
				avail.put(e.getKey(), available);
			}
			if(materialcost > 1){ //Do a second pass (if we need to spend more than a dollar on materials still)
				if(debugText) System.out.println("Second Pass.");
				for(Entry<MaterialType, Integer> e : act.getParentAct().getMaterialUse().entrySet())
				{
					int available = avail.get(e.getKey());
					MaterialInfo info1 = act.getParentAct().getMaterialInfo(e.getKey());
					
					//The amount we need
					int need = info1.getTotalNeed() - info1.getTotalUsed();
					
					//Don't remove more than the activity has allocated to itself
					int test = act.getOrderedAmount(e.getKey()) - info1.getTotalUsed();
					if(need > test) need = test;
					
					//Make sure there is enough allocated
					int newAmt = (need > available)? available : need;
					
					//Don't spend more than we want.... (more than we have allocated for)
					int newMax = (int)(materialcost/e.getValue()); ///THINK 10/3 = 3, 1 remains. 9/3 = 3, 0 remain.
					if(newMax < newAmt) newAmt = (newMax + 1);
					//Max should be equal or *one* greater than materialcost can handle
					//Max should also be less than or equal to the MaxNeed number
					
					if(debugText){
						System.out.println(newMax+" to fill the Cost($"+newMax*e.getValue()+"/$"+materialcost+").");
						System.out.println(act.getParentAct().getDescription()+" to use "+newAmt+" of "+e.getKey().getDescription());
						System.out.println("\tAvailable: "+available+" Ordered: "+test+" Used: "+info1.getTotalUsed()+" Need: "+(info1.getTotalNeed()-info1.getTotalUsed()));
					}
					
					if(newAmt > 0){
						//Here we would:
						// 1.remove from stock
						available = available - newAmt;
						//NOTE: Since the first pass adds the item to stock, we have to remove again.
						currentState.stock.remove(e.getKey(), newAmt);
						// 2.set total work left
						act.setTotalWorkLeft(act.getTotalWorkLeft() - e.getKey().getCost() * (double)newAmt);
						// 3.add to the usedmaterials list
						//HashMap<MaterialType, Integer> actMaterials = usedMaterials.get(act.getParentAct());
						int oldAmt = 0;
						if(map.containsKey(e.getKey())){
							oldAmt = map.get(e.getKey());	
						}
						map.put(e.getKey(), newAmt + oldAmt);
						// 4.store used materials in materialinfo
						info1.addUse(newAmt);
						// 5.do cost computation
						sched.addMaterial(act.getParentAct(), day, newAmt * e.getKey().getCost());
						cost += newAmt * e.getKey().getCost();
						// 7.remove the cost from materialcost
						materialcost -= newAmt * e.getKey().getCost();
					}
					if(available > 0){
						if(available > test) available = test;
						if(debugText) System.out.println("-----> Putting "+available+" back into stock.");
						// 6.put extra back into stock
						currentState.stock.add(e.getKey(), available);
					}
					// 8.put available into avail
					avail.put(e.getKey(), available);
					// 8.we are done if materialcost is "emptied"
					if(materialcost <= 0) break;
				}
			}
			

			//4. Compute remaining duration... In days not percentages.
			//base work is the expected amount done by the as-planned compliment of laborers
			double base_work = act.getParentAct().computeDailyMaterialCost();
			double unaltednewdir = (act.getTotalWorkLeft() / base_work) - .01;//subtract 1 percent to quantize productivity, and condition the floating point numbers to round properly //useTotalWorkLeft
			int new_duration = (int)Math.ceil(unaltednewdir);
			int current_duration = act.getOutPrimaryArc().getHeadNode().getEarlyStart() - act.getEarlyStart();
			int delay = new_duration - current_duration + 1;

			//5. Delay activity
			//no need to call delay if it is 0
			if(delay != 0)
				delayActivity(act, delay);

			//6. Compute costs
			Activity a = act.getParentAct();
			for(LaborCrew c : alloc.getLaborCrews())
			{

				double time=0;
				double todayCost = 0; //the cost for the labor crew

				//Calculate time
				switch (interval)
				{
				case 1:
					time=alloc.getWorkHours();//default
					int cday = getCalendar().get(Calendar.DAY_OF_WEEK);

					if ((alloc.getWorkDays()>5 && cday == Calendar.SATURDAY) || (alloc.getWorkDays()>6 && cday == Calendar.SUNDAY))//overtime
						time*=AgentM.OVER_TIME_RATE;
					else if(cday == Calendar.SATURDAY || cday == Calendar.SUNDAY)//day off
						time=0;
					else if (time > AgentM.OVER_TIME_HOURS)//regularday with over time
						time=((time - AgentM.OVER_TIME_HOURS)*AgentM.OVER_TIME_RATE)+AgentM.OVER_TIME_HOURS;

					time /= AgentM.OVER_TIME_HOURS;

					//the only case remaining is regularday no overtime which needs no special adjustment
					break;

				case 7:
					time = alloc.getWorkHours()*alloc.getWorkDays();
					if (time>AgentM.OVER_TIME_HOURS*AgentM.OVER_TIME_DAYS)//overtime
						time = ((time -(AgentM.OVER_TIME_HOURS*AgentM.OVER_TIME_DAYS))*AgentM.OVER_TIME_RATE) + (AgentM.OVER_TIME_HOURS*AgentM.OVER_TIME_DAYS);
					time /= AgentM.OVER_TIME_HOURS*AgentM.OVER_TIME_DAYS;
					break;

				case 28:
					time = alloc.getWorkHours()*alloc.getWorkDays()*4;
					if (time>AgentM.OVER_TIME_HOURS*AgentM.OVER_TIME_DAYS*4)//overtime
						time = ((time -(AgentM.OVER_TIME_HOURS*AgentM.OVER_TIME_DAYS*4))*AgentM.OVER_TIME_RATE) + (AgentM.OVER_TIME_HOURS*AgentM.OVER_TIME_DAYS*4);
					time /= AgentM.OVER_TIME_HOURS*AgentM.OVER_TIME_DAYS*4;
					break;
				default:
					throw new Error("Unspecified time frame");
				}

				//Get the correct cost for the labor crew, in case we hire extra or fire people, etc.
				for(LaborType b : c.getTypes()){
					todayCost += c.getAmt(b)*b.getCost();
				}

				//adujsted work hours times by wage and wage incentive, double pay rate for overtime
				sched.addLabor(a, day, time * c.getDailyCost() * alloc.getWageIncentive()); // should day be c?
				cost += time * c.getDailyCost() * alloc.getWageIncentive();
			}

			sched.addIndirect(a, day, a.computeDailyMaterialCost() * AgentM.overhead);// is this incorect? day is used to spesify crew. FROM MATT: What? this comment makes no sense to me. Day is used to specify day for the schedule
			cost += a.computeDailyMaterialCost() * AgentM.overhead;
		}

		//increment Present Nodes before we exit
		incrementPresentNodes(currentState);

		sched.setStockValue(day, getStock().getValue());

		//doing this circumvents how this was intended to work, but the intended way has a bug and I don't have
		//the time to fix it at the moment. Normally, fast_day_total gets updated by the addIndirect, addMaterial,
		//and addLabor calls to sched. But, there is a bug with that. So, I just compute the cost change
		//OLD: changed from (day-1) to (day). Need to verify correctness: INCORRECT!
		//NOTE: That change caused the Projected best/worst case distributions to be incorrect
		// It has been changed back to fix this error.
		if(sched.getQueryFuturesTotal().containsKey(day-1))
			cost += sched.getQueryFuturesTotal().get(day-1);
		sched.setqueryFuturesTotal(cost);

		sched.updateDayTotal(getCurrentTimeStep());

		//do not do any further calculations if we are in query mode, since that is useless
		if(TONAE.querymode)
			return;

		//calculate a projection for the remainder of the project  
		currentState.mathAgent.computeCost(sched, getCurrentTimeStep() + 1, getLastTimeStep(), this);//asBuilt == sched

		Set<ANode> anodes = getANodeSet();
		for(ANode node : anodes)
		{
			TreeMap<Integer, Double> map = currentState.mathAgent.getAsBuiltProgress(node.getParentAct());//asbuiltprogress.get(node.getParentAct());
			//This is a starting a-node
			if(node.getOutPrimaryArc() != null)
			{
				PNode p = (PNode)node.getOutPrimaryArc().getHeadNode();
				double start = p.getStart();
				double end = p.getEnd();
				double now = p.getEarlyStart();

				if(now > end)
					map.put(getCurrentTimeStep() + 1, 1.0);
				else if(now < start)
					map.put(getCurrentTimeStep() + 1, 0.0);
				else
					map.put(getCurrentTimeStep() + 1, 1.0 - ((end - now) / p.getParentAct().getDuration()));
				//I believe that wage incentive must beaded in this code block to function correctly.
			}

		}
	}

	public void addQueryResult(QueryResult2 r){ //Used to combine threaded query results into the "main" query result.
		result.add(r);
	}

	public QueryResult2 getQueryResults(){
		return result;
	}

	/**
	 * Print daily cost information.
	 */
	private void printDailyTotals(){
		//
		//          asBuilt   asPlanned   difference
		//material
		//labor
		//indirect
		//

		CostSchedule asBuilt = currentState.mathAgent.getAsBuilt(); // The as-built schedule
		CostSchedule asPlanned = currentState.mathAgent.getAsPlanned(); //The as-planned schedule
		int today = getCurrentTimeStep();
		DecimalFormat format = new DecimalFormat("$#0.00");

		//		for(PNode p : currentState.getReadyList()){
		//			Activity act = p.getParentAct();
		for(ANode anode : getANodeSet())
		{
			if(anode.getOutPrimaryArc() == null)
				continue;

			Activity act = anode.getParentAct();
			int abstart = anode.getEarlyStart();
			int abend = anode.getOutPrimaryArc().getHeadNode().getOutPrimaryArc().getHeadNode().getEarlyStart();
			int basestart = act.getStart();
			int baseend = act.getEnd();

			if(
					(abstart > getCurrentTimeStep() && basestart > getCurrentTimeStep()) || //activity not started
					(abend <= getCurrentTimeStep() && baseend <= getCurrentTimeStep())//activity already ended
			)
				continue;
			System.out.println("\nActivity "+act.getLabel()+": Week " + getCurrentTimeStep()+" As-Planned Start: "+basestart+" As-Planned End: "+baseend);

			//Material
			double mb = asBuilt.getMaterial(act, today) - asBuilt.getMaterial(act, today - 1 );
			double mp = asPlanned.getMaterial(act, today) - asPlanned.getMaterial(act, today - 1);

			//Labor
			double lb = asBuilt.getLabor(act, today) - asBuilt.getLabor(act, today - 1);
			double lp = asPlanned.getLabor(act, today) - asPlanned.getLabor(act, today - 1);

			//Indirect
			double ib = asBuilt.getIndirect(act, today) - asBuilt.getIndirect(act, today - 1);
			double ip = asPlanned.getIndirect(act, today) - asPlanned.getIndirect(act, today - 1);

			System.out.println("\tAs-Built\tAs-Planned\tDifference");
			//System.out.println("Material:\t$"+mb+"\t$"+mp+"\t$"+(mb-mp));
			//System.out.println("Labor:\t$"+lb+"\t$"+lp+"\t$"+(lb-lp));
			//System.out.println("Indirect:\t$"+ib+"\t$"+ip+"\t$"+(ib-ip)+"\n");
			System.out.println("Material:\t"+format.format(mb)+"\t"+format.format(mp)+"\t"+format.format((mb-mp)));
			System.out.println("Labor:\t"+format.format(lb)+"\t"+format.format(lp)+"\t"+format.format((lb-lp)));
			System.out.println("Indirect:\t"+format.format(ib)+"\t"+format.format(ip)+"\t"+format.format((ib-ip))+"\n");
		}

		//Print stock value
		System.out.println("Stock Value:\n\t"+format.format(asBuilt.getStockValue(today))+"\t"+format.format(asPlanned.getStockValue(today)));

		//Print totals
		System.out.println("\nTotals");

		//Material
		double mb = asBuilt.getMaterialTotal(today, this) - asBuilt.getStockValue(today); //Remove stock value from the material total
		double mp = asPlanned.getMaterialTotal(today, this);

		//Labor
		double lb = asBuilt.getLaborTotal(today, this);
		double lp = asPlanned.getLaborTotal(today, this);

		//Indirect
		double ib = asBuilt.getIndirectTotal(today, this);
		double ip = asPlanned.getIndirectTotal(today, this);

		System.out.println("\tAs-Built\tAs-Planned\tDifference");
		//System.out.println("Material:\t$"+mb+"\t$"+mp+"\t$"+(mb-mp));
		//System.out.println("Labor:\t$"+lb+"\t$"+lp+"\t$"+(lb-lp));
		//System.out.println("Indirect:\t$"+ib+"\t$"+ip+"\t$"+(ib-ip)+"\n");
		System.out.println("Material:\t"+format.format(mb)+"\t"+format.format(mp)+"\t"+format.format((mb-mp)));
		System.out.println("Labor:\t"+format.format(lb)+"\t"+format.format(lp)+"\t"+format.format((lb-lp)));
		System.out.println("Indirect:\t"+format.format(ib)+"\t"+format.format(ip)+"\t"+format.format((ib-ip))+"\n");
	}

	@Override
	public void ruleTriggered(Rule r, Activity a, Object o) {
		if(r.getWarning() == null) return; //We only want to print warnings.
		//Print out the info on the rule
		System.out.println("\n=======================================================================>");
		System.out.println(r.getWarning());
		String s;
		if(o != null && o instanceof MissingLaborContainer)
			s = ((MissingLaborContainer) o).getType().getDescription()+" missing from crew "+((MissingLaborContainer) o).getCrew().getName();
		else
			s = r.getWarning();
		System.out.println(s);
		System.out.println("(Rule "+r.getName()+")");
		System.out.println("=======================================================================>\n");

		//Put the rule in the rule list
		tRules.add(new TriggeredRule(r, s));
	}

	public void removeQueryResultListener(QueryResultListener r){
		querylisteners.remove(r);
	}
	public void removeRuleListener(RuleListener r){
		rulelisteners.remove(r);
	}
	public void removeSpaceViolationListener(SpaceViolationListener r){
		spaceViolationListener.remove(r);
	}
	public void removeLaborChangeListener(LaborChangeListener l){
		laborChangeListener.remove(l);
	}
	public void removeLaborAlteredListener(LaborAlteredListener l){
		laborAlteredListener.remove(l);
	}

	public long getHistoryID(){
		if(db != null)
			return db.getHistoryID();
		return 0;
	}

}

class TONAEState implements Serializable
{
	protected static final long serialVersionUID = 1L;

	// Counter to keep track of what time *now* is
	protected int t_now;

	// Silly comparator required for TreeSet sorting for ready list
	protected PNodeCompare compare;

	protected Stock stock;
	protected ArrayList<Stock> stock_track = new ArrayList<Stock>();

	protected HashMap<MaterialType, Integer> purchasedmaterial = new HashMap<MaterialType, Integer>();
	protected HashMap<MaterialType, Integer> rejectedmaterial = new HashMap<MaterialType, Integer>();

	protected AgentM mathAgent;

	// Management constructs for the A-Node graph
	protected HashSet<ANode> aNodeSet;
	protected HashMap<String, ANode> aNodeMap;


	/**
	 * we think this is the node that represents the current time period in the simulation
	 */
	protected PNode global;

	// Construct for maintaining the ready list
	protected TreeSet<PNode> readyList;

	protected HashMap<Activity, Entry<Integer, Integer>> fsched;

	protected Environment environment;

	protected GregorianCalendar currentDate;


	////////////////////////////////////////////////////////////

	// Regular constructor
	public TONAEState(Activity[] activities, double total_space)
	{
		stock = new Stock(total_space);

		// Instantiate the constructs for managing the graph
		aNodeSet = new HashSet<ANode>();
		aNodeMap = new HashMap<String, ANode>();

		// Instantiate the ready list
		compare = new PNodeCompare();
		readyList = new TreeSet<PNode>(compare);

		// Set the time to be zero, for now
		t_now = 0;

		environment = new Environment(activities);

		// Create the global present node
		global = new PNode();
		global.setTimeOfResolution(0);
		global.setEarlyStart(0);
		global.setLabel("Global");
	}

	// Accessor methods for the A-Node graph
	public HashSet<ANode> getANodeSet() { return aNodeSet; }
	public HashMap<String, ANode> getANodeMap() { return aNodeMap; }

	// Accessor method for the comparator
	public PNodeCompare getComparator() { return compare; }

	// Accessor method for the ready list
	public TreeSet<PNode> getReadyList() { return readyList; }

	// Accessor methods for the project time
	public int getTime() { return t_now; }
	public void setTime(int t) { t_now = t; global.setEarlyStart(t_now); }

	public PNode getGlobal() { return global; }
	public void setGlobal(PNode globe) { global = globe; }
}

class TONAEEntry implements Entry<Integer, Integer>, Serializable
{
	private static final long serialVersionUID = 1L;
	public int key, value;

	public TONAEEntry(int k, int v)
	{
		key = k;
		value = v;
	}

	public Integer getKey()
	{
		return key;
	}

	public Integer getValue() {
		return value;
	}

	public Integer setValue(Integer arg0) {
		return value = arg0;
	}
}

class QueryThread extends Thread {

	private Updater update;
	private TONAE tonae;
	private int num;
	private double bucketsize;

	public QueryThread(TONAE tonae, TONAEState state, Project asPlanned, Project baseline, Vector<Rule> rules, int num)//, double bucketsize){
	{	
		this.tonae = tonae;
		this.num = num;
		//this.bucketsize = bucketsize;
		update = new Updater(state, asPlanned, baseline, rules);

		//return completed.mathAgent.getAsBuilt().getQueryFuturesTotal()
	}

	public void run(){
		tonae.addQueryResult(update.queryFutures(num));//, bucketsize));
	}
}