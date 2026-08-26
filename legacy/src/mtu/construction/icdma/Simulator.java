package mtu.construction.icdma;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;

import mtu.construction.listener.G_SpaceViolationListener;
import mtu.construction.listener.LaborAlteredListener;
import mtu.construction.listener.LaborChangeListener;
import mtu.construction.listener.QueryResultListener;
import mtu.construction.listener.RuleListener;
import mtu.construction.listener.SpaceViolationListener;

import mtu.construction.gui.old.MainWindow;
import mtu.construction.gui.wrapper.G_Activity;
import mtu.construction.gui.wrapper.G_LaborCrew;
import mtu.construction.gui.wrapper.G_Material;
import mtu.construction.gui.wrapper.G_ResourceAlloc;
import mtu.construction.gui.wrapper.G_Variable;
import mtu.construction.gui.wrapper.GanttChartInfo;
import mtu.construction.interpreter.databaseconnector.DBConnection;
import mtu.construction.interpreter.databaseconnector.VCDBInterpreter;
//import project.Activity;
import mtu.construction.project.Activity;
import mtu.construction.project.LaborCrew;
import mtu.construction.project.LaborType;
import mtu.construction.project.MaterialType;
import mtu.construction.project.PlanException;
import mtu.construction.project.Project;
import mtu.construction.project.Stock;
import mtu.construction.project.TONAE;
import mtu.construction.project.TimeFrame;

import mtu.construction.tonae.*;
import mtu.construction.variable.Variable;

/**
 * The Simulator.
 * It is the backend. It is like all the ports on the back of a computer.
 * 
 * It should be some sort of state machine, accepting inputs/requests until "sim step"
 * Client/Server type thing...
 * 
 * Frontend Updating is done through listeners
 * 
 * @author kkaaikal
 *
 */
/* NOTES:
 * Getting a sorted list of activities depends on activity ID and not on constraints?...
 * Need to dynamically build lists every time the function is called?
 * NOTE: Currently only one spaceViolationListener allowed in TONAE
 * NOTE: The G_* classes should not create new items, rather use ones stored in Simulator
 */
public class Simulator implements SpaceViolationListener{
	
	/* Begin Global Variables */
	public static boolean querymode = false; //whether or not we are querying
	public static int query_timeout = 1000; //timeout threshold querying (ms)
	public static int queryThreads = 7; //Number of threads used for querying (multi-threaded queryFutures)
	private boolean guiEnabled = false; //whether or not we are using a gui
	public static int numFutures = 100;
	public static int QUERY_FUTURES_QUANTINIZATION = 30000;
	
	/* Begin Other Variables */
	private DBConnection conn;
	private TONAE tonae;
	MainWindow GUI;
	
	private HashSet<G_Activity> activities;
	private HashSet<G_LaborCrew> crews;
	private HashSet<G_Material> materials;
	private Vector<G_SpaceViolationListener> spaceViolationListeners;
	
	private HashMap<Integer, G_Activity> activities2;
	
	/* Variables for Updating */
	LaborCrew Hired = new LaborCrew(-1, "DefaultHired");//For laborAllocationDialog, specific to "Missing (sick) Worker" and "Labor Strike"
	LaborCrew Unmapped = new LaborCrew(-2, "DefaultUnmapped");// laborAllocationDialog -> LaborCrewPanel "labor not mapped to a crew"
	LaborCrew[] crewRequest = {Hired};
	Vector<ResourceAllocation> Requested = new Vector<ResourceAllocation>();
	HashMap<MaterialType, Integer> SoldMaterial = new HashMap<MaterialType, Integer>();
	
	/**
	 * This constructor prepares the simulation to run. This includes:
	 * 1. Reading the plan from the database
	 * 2. Creating the project and associated items
	 * 3. ...
	 */
	public Simulator(){
		
		//Attempt to connect to database and create project.
		if(!connect()){
			System.out.println("Could not connect to the database: closing.");
			System.exit(1);
		}
		
		activities = new HashSet<G_Activity>();
		activities2 = new HashMap<Integer, G_Activity>();
		crews = new HashSet<G_LaborCrew>();
		materials = new HashSet<G_Material>();
		spaceViolationListeners = new Vector<G_SpaceViolationListener>();
		
		setSpaceViolationListener(this);
		
		updateLists();
		
		System.out.println("Connected successfully!");
		
	}
	
	public Simulator(String host, int port, String database, int projectNo ,String username, String password){
		if(!connect(host, port, database, projectNo, username, password)){
			System.out.println("Could not connect to the database: closing.");
			System.exit(1);
		}
		
		activities = new HashSet<G_Activity>();
		activities2 = new HashMap<Integer, G_Activity>();
		crews = new HashSet<G_LaborCrew>();
		materials = new HashSet<G_Material>();
		spaceViolationListeners = new Vector<G_SpaceViolationListener>();
		
		setSpaceViolationListener(this);
		
		updateLists();
		
		System.out.println("Connected successfully!");
	}
	
	/**
	 * Begins the simulation...
	 */
	public void run(){
		//GUI = new MainWindow(this);
		//GUI.setVisible(true);
	}
	
	private boolean connect(){
		return connect("localhost", 5433, "vcdb", 7, "postgres", "");
	}
	
	/**
	 * Attempts to connect to the database. On success, it creates a project
	 * 
	 * @return true if connection succeeded, false otherwise.
	 */
	private boolean connect(String host, int port, String database, int projectNo, String username, String password){
		Project proj;
		
		try
		{
			conn = new DBConnection("postgresql", host, port, database, username, password);
			VCDBInterpreter interpreter = new VCDBInterpreter(conn, projectNo);
			proj = interpreter.buildProject();//plan = new Plan(interpreter);
			
		}
		catch(PlanException e)
		{
			e.printStackTrace();
			System.out.println("Problem loading plan.");
			return false;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			System.out.println("Problem connecting to database.");
			//throw new Error("A critical failure occurred while connecting to the database");
			return false;
		}
		
		if(proj == null || conn == null){
			return false;
		}
		
		//Create TONAE
		tonae = new TONAE(conn, proj);

		//Return success
		System.out.println("Plan was loaded successfully.");
		return true;
	}

	/*
		What follows are the various methods used to manipulate the
		simulation.
	
	*/
	 
	/*======== Grabbing Information ========*/

	
	//============= Information obtainable from TONAE or AgentM
	//Get Material Types (requested amount, ...)
	//Get Stock Track
	//Get weather state (listener?) (need to send Environment to GUI?)
	//Get delivery materials
	
	/**
	 * Get Last timestep... (the last day/week)
	 * 
	 * @return - the last step of the simulation
	 */
	public int getLastTimeStep(){
		return tonae.getLastTimeStep();
	}
	/**
	 * Get Current TimeStep
	 * 
	 * @return - get the current step of the simulation
	 */
	public int getCurrentTimeStep(){
		return tonae.getCurrentTimeStep();
	}
	
	//******A Copy of stock to get Space, etc...
	//Get Space --Any
	public Stock getStock(){
		return tonae.getStock().clone();
	}
	
	//Get Resource-Loaded Commodity curve --Any
	public TreeMap<Integer, Double> getResourceLoadedCommodityCurve(){
		return tonae.getResourceLoadedCommodityCurve();
	}
	
	//Get Resource-Loaded Labor curve --Any
	public HashMap<G_LaborCrew, TreeMap<Integer, Integer>> getResourceLoadedLaborCurve(){
		HashMap<G_LaborCrew, TreeMap<Integer, Integer>> rllc = new HashMap<G_LaborCrew, TreeMap<Integer, Integer>>();
		for(Entry<LaborCrew, TreeMap<Integer, Integer>> e : tonae.getResourceLoadedLaborCurve().entrySet()){
			rllc.put(new G_LaborCrew(e.getKey()), e.getValue());
		}
		return rllc;
	}
	
	/**
	 * Get the Calendar with current date
	 * 
	 * @return - copy of the current calendar
	 */
	public GregorianCalendar getCalendar(){
		return (GregorianCalendar) tonae.getCalendar().clone();
	}
	/**
	 * Get the timeframe for this project
	 * 
	 * @return - the timeframe
	 */
	public TimeFrame getTimeFrame(){
		return tonae.getProject().getTimeFrame();
	}
	
	/**
	 * Get Global Variables
	 * 
	 * @return - a HashSet of all global variables for the project
	 */
	public HashSet<G_Variable> getGlobalVariables(){
		HashSet<Variable> vars = tonae.getEnvironment().getGlobalVariables();
		HashSet<G_Variable> newVars = new HashSet<G_Variable>();
		for(Variable v : vars){
			newVars.add(new G_Variable(v));
		}
		return newVars;
	}
	
	/**
	 * Get a specific global variable
	 * 
	 * @param s - the name of the variable to get
	 * @return - the variable, if it exists or null otherwise
	 */
	public G_Variable getGlobalVariable(String s){
		Variable v = tonae.getEnvironment().getGlobalVariable(s);
		if(v != null) return new G_Variable(v);
		return null;
	}
	
	/**
	 * Get a sorted list of activities
	 * They are sorted in the order of execution
	 * 
	 * @return - sorted list of activities
	 */
	public Vector<G_Activity> getSortedActivityList()
	{
		Vector<G_Activity> retval = getActivityList();
		for(int y = 0; y < retval.size(); y++)
		{
			for(int x = 0; x < retval.size(); x++)
			{
				if(retval.get(x).getID() > retval.get(y).getID())
				{
					G_Activity tmp = retval.get(x);
					retval.set(x, retval.get(y));
					retval.set(y, tmp);
				}
			}
		}
		
		return retval;
	}
	
	/**
	 * Get a list of activities
	 * @return
	 */
	public Vector<G_Activity> getActivityList()
	{
		Vector<G_Activity> acts = new Vector<G_Activity>();
		for(ANode node : tonae.getANodeSet())
		{
			if(node.getOutPrimaryArc() != null){
				G_Activity act = activities2.get(node.getParentAct().getID());
				if(act == null){
					act = new G_Activity((PNode)node.getOutPrimaryArc().getHeadNode(), tonae, this);
					acts.add(act);
					activities2.put(act.getID(), act);
				}
				else acts.add(act);
			}
		}
		
		return acts;
	}
	
	/**
	 * Get array containing all activities from project
	 * 
	 * @return - array of all activities
	 */
	public G_Activity[] getActivities() {
		
		//Activity[] acts = tonae.getProject().getActivities();
		G_Activity[] newActs = new G_Activity[activities.size()];
		/*for(int i=0; i<acts.length; i++){
			newActs[i] = new G_Activity(acts[i], tonae, this);
		}
		return newActs;
		*/
		int i=0;
		for(G_Activity a : activities){
			newActs[i] = a;
			i++;
		}
		return newActs;
		
	}
	
	/**
	 * Get the total space size from project
	 * 
	 * @return - the total amount of space for material storage
	 */
	public int getSpace(){
		return tonae.getProject().getSpace();
	}
	
	/**
	 * Get list of activities that are "ready"
	 * These are activities that can work this timestep
	 * 
	 * @return
	 */
	public Vector<G_Activity> getReadyActivities(){
		Vector<G_Activity> activities = new Vector<G_Activity>();
		
		for(PNode p : tonae.getReadyList()){
			//activities.add(new G_Activity(p, tonae, this));
			G_Activity x = activities2.get(p.getParentAct().getID());
			
			activities.add(x);
		}
		return activities;
	}
	
	//Get AsPlanned Distributions
	public TreeMap<Integer, Double> getAsPlannedDirect(){
		return tonae.getMathAgent().getAsPlanned().getDirect(tonae);
	}
	public TreeMap<Integer, Double> getAsPlannedIndirect(){
		return tonae.getMathAgent().getAsPlanned().getIndirect(tonae);
	}
	public TreeMap<Integer, Double> getAsPlannedTotal(){
		return tonae.getMathAgent().getAsPlanned().getTotal(tonae);
	}
	
	//Get AsBuilt Distributions
	public TreeMap<Integer, Double> getAsBuiltDirect(){
		return tonae.getMathAgent().getAsBuilt().getDirect(tonae);
	}
	public TreeMap<Integer, Double> getAsBuiltIndirect(){
		return tonae.getMathAgent().getAsBuilt().getIndirect(tonae);
	}
	public TreeMap<Integer, Double> getAsBuiltTotal(){
		return tonae.getMathAgent().getAsBuilt().getTotal(tonae);
	}
	
	/** Get a list of all laborcrews from project
	 * 
	 * @return - array containing all laborcrews
	 */
	public G_LaborCrew[] getLaborCrews(){
		
		//LaborCrew[] crews = tonae.getProject().getLaborCrews();
		G_LaborCrew[] newCrews = new G_LaborCrew[crews.size()];
		
		//for(int i=0; i<crews.length; i++){
			//newCrews[i] = new G_LaborCrew(crews[i]);
		int i=0;
		for(G_LaborCrew c : crews){
			newCrews[i] = c; i++;
		}
		return newCrews;
		
	}
	
	/**
	 * Get a list of all labortypes from the project
	 * @return
	 */
	public LaborType[] getLaborTypes(){
		return tonae.getProject().getLaborTypes();
	}
	
	/**
	 * Get a list of all materialtypes from the project
	 * @return
	 */
	public G_Material[] getMaterialTypes(){
		
		//MaterialType[] mat = tonae.getProject().getMaterialTypes();
		G_Material[] material = new G_Material[materials.size()];
		//for(int i=0; i<mat.length; i++){
			//materials[i] = new G_Material(mat[i]);
		int i=0;
		for(G_Material m : materials){
			material[i] = m; i++;
		}
		
		return material;
	}
	
	/**
	 * Get the default resource allocations per activity
	 * 
	 * TODO: we create a new resourceAllocation each time the method is called. It would be good to change this.
	 */
	public Vector<G_ResourceAlloc> getDefaultResourceAllocations(){
		Vector<G_ResourceAlloc> alloc = new Vector<G_ResourceAlloc>();
		for(ResourceAllocation r : tonae.getDefaultResourceAllocation()){
			alloc.add(new G_ResourceAlloc(r, tonae, this));
		}
		return alloc;
	}
	
	/**
	 * Signal the start of the timer
	 */
	public void signalTimerStart(){
		tonae.signalTimerStart();
	}
	
	/**
	 * Signal the end of the timer
	 */
	public void signalTimerEnd(){
		tonae.signalTimerEnd();
	}
	
	/**
	 * Query some amount of futures, for whatever reason
	 * 
	 * @param amt - the number of futures to query
	 * @param bucketsize - the bucketsize of the results
	 * @return - Cost/probability distribution from the future!
	 */
	public QueryResult2 queryFutures(int amt, int bucketsize){
		System.out.println("Querying "+amt+" futures.");
		return tonae.queryFutures(amt);//, bucketsize);
	}
	
	/**
	 * Convenient method to get gantt chart information
	 * 
	 * @return Gantt chart information per activity
	 */
	public HashMap<G_Activity, GanttChartInfo> getGanttChart(){
		HashMap<G_Activity, GanttChartInfo> info = new HashMap<G_Activity, GanttChartInfo>();
		for(ANode anode : tonae.getANodeSet())
		{
			if(anode.getOutPrimaryArc() == null)
				continue;
			
			Activity a = anode.getParentAct();
			
			G_Activity act = activities2.get(a.getID());
			
			if(act == null){ act = new G_Activity(a, tonae, this); activities.add(act); activities2.put(act.getID(), act); System.out.println("NewAct");}
			info.put(act, new GanttChartInfo(act, anode.getEarlyStart(), anode.getOutPrimaryArc().getHeadNode().getOutPrimaryArc().getHeadNode().getEarlyStart(), act.isCritical()));
		}
		
		return info;
	}
	
	public double getOverstockPenalty(){
		return tonae.getProject().getOverstockPenalty();
	}
	
	/*======== Altering Simulation ========*/
	
	//Do one step of the simulation
	public void update(){
		tonae.update(conn, tonae.getQueryResults(), Requested, SoldMaterial, crewRequest, Unmapped, Hired);
		//updateLists();
	}
	
	//Load from file
/*	public boolean load(String path){
		File f = new File(path);
		tonae.load(f);
		return false;
	}
*/	public boolean load(File file){
		try {
			tonae = TONAE.load(file);
			setSpaceViolationListener(this);
			updateLists();
			long history = getHistoryID();
			if(history > 0){
				System.out.println("\n*************************");
				System.out.println("Continuing History "+history+" at time +"+tonae.getCurrentTimeStep()+"+.");
				System.out.println("*************************\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	//Save to file
/*	public boolean save(String path){
		File f = new File(path);
		tonae.save(f);
		return false;
	}
*/	public void save(File file){
		unsetSpaceViolationListener(this);
		tonae.save(file);
	}
	
	//Quit the simulation
	public void quit(){
		System.exit(0);
	}
	
	/**
	 * Update internal data structures
	 * This forces the GUI to work with brand new sets every timestep...
	 */
	private void updateLists(){
		
		//Generate G_ Items
		
		//empty lists
		activities.clear();
		activities2.clear();
		crews.clear();
		materials.clear();
		
		//Update lists
		for(Activity a : tonae.getProject().getActivities()){
			G_Activity x = new G_Activity(a, tonae, this);
//			if(x.isActive()) System.out.println(x.getLabel()+" is Active!");
			activities.add(x);
			activities2.put(x.getID(), x);
		}
		
		for(LaborCrew c : tonae.getProject().getLaborCrews()){
			crews.add(new G_LaborCrew(c));
		}
		
		for(MaterialType m : tonae.getProject().getMaterialTypes()){
			materials.add(new G_Material(m));
		}
	}
	

	/*======== Set Simulation Parameters ========*/
	
	//Register Listeners
	public void registerQueryResultListener(QueryResultListener r){
		tonae.addQueryResultListener(r);
	}
	public void registerRuleListener(RuleListener r){
		tonae.addRuleListener(r);
	}
	private void setSpaceViolationListener(SpaceViolationListener r){
		tonae.addSpaceViolationListener(r);
	}
	public void registerSpaceViolationListener(G_SpaceViolationListener r){
		spaceViolationListeners.add(r);
	}
	public void registerLaborChangeListener(LaborChangeListener l){
		tonae.addLaborChangeListener(l);
	}
	public void registerLaborAlteredListener(LaborAlteredListener l){
		tonae.addLaborAlteredListener(l);
	}

	//Un-Register Listeners
	public void unregisterQueryResultListener(QueryResultListener r){
		tonae.removeQueryResultListener(r);
	}
	public void unregisterRuleListener(RuleListener r){
		tonae.removeRuleListener(r);
	}
	public void unregisterSpaceViolationListener(G_SpaceViolationListener r){
		spaceViolationListeners.remove(r);
	}
	private void unsetSpaceViolationListener(SpaceViolationListener r){
		tonae.removeSpaceViolationListener(r);
	}
	public void unregisterLaborChangeListener(LaborChangeListener l){
		tonae.removeLaborChangeListener(l);
	}
	public void unregisterLaborAlteredListener(LaborAlteredListener l){
		tonae.removeLaborAlteredListener(l);
	}

	
	//Moved to G_ResourceAlloc
	//public double computeWorkQuantityMultiplier(G_ResourceAlloc r){
	//	tonae.computeWorkQuantityMultiplier(r.getActivity().getAsPlannedLaborUse(), null, r.getWorkHours(), r.getWorkDays(), r.getWageIncentive());
	//}
	
	/* Resource Allocation Stuff */
	/*
	public int getAvailable(G_Material, G_Activity){
		
	}
	public int getRequested(G_Material, G_Activity){
		
	}
	public int getUsed(G_Material, G_Activity){
		
	}
	public HashSet<G_LaborCrew> getCrews(G_Activity){
		
	}
	*/
	
	//Get Hired labor
	//This is where the GUI tells the simulator what laborcrew is hired
	public void setHired(LaborCrew hire){
		Hired = hire;
	}
	public void setHired(G_LaborCrew hire){
		Hired = hire.unwrap();
	}
	
	//Get Unmapped Labor
	public void setUnmapped(LaborCrew umap){
		Unmapped = umap;
	}
	public void setUnmapped(G_LaborCrew umap){
		Unmapped = umap.unwrap();
	}
	
	//Get requested crews
	//This is where the GUI tells the simulator what laborcrews are being requested
	public void setCrewRequest(LaborCrew[] request){
		crewRequest = request;
	}
	public void setCrewRequest(G_LaborCrew[] request){
		LaborCrew[] newRequest = new LaborCrew[request.length];
		int i=0;
		for(G_LaborCrew c : request){
			newRequest[i] = c.unwrap();
			i++;
		}
		crewRequest = newRequest;
	}
	
	//Get requested resources
	//This is where the GUI tells the simulator what resources the activities are requesting
	public void setResourceRequest(Vector<ResourceAllocation> request){
		Requested = request;
	}
	public void setgResourceRequest(Vector<G_ResourceAlloc> grequest){
		Vector<ResourceAllocation> allocation = new Vector<ResourceAllocation>();
		for(G_ResourceAlloc r : grequest){
			ResourceAllocation alloc = new ResourceAllocation(r.getActivity().unwrap());
			for(G_LaborCrew c : r.getRequestedLabor()){
				alloc.request(c.unwrap());
			}
			for(G_Material m : getMaterialTypes()){
				if(r.getRequested(m) > 0){
					alloc.request(m.unwrap(), r.getRequested(m));
				}
			}
			alloc.setOrder(r.getOrder());
			alloc.setWorkDays(r.getWorkDays());
			alloc.setWorkHours(r.getWorkHours());
			alloc.setWageIncentive(r.getWageIncentive());
			allocation.add(alloc);
		}
		Requested = allocation;
	}
	
	//Get sold material
	//This is where the GUI tells the simulator what materials are being sold
	public void setSoldMaterial(HashMap<MaterialType, Integer> sold){
		SoldMaterial = sold;
	}
	public void setgSoldMaterial(HashMap<G_Material, Integer> sold){
		HashMap<MaterialType, Integer> newSold = new HashMap<MaterialType, Integer>();
		for(Entry<G_Material, Integer> e : sold.entrySet()){
			newSold.put(e.getKey().unwrap(), e.getValue());
		}
	}

	/**
	 * Determine if the simulation has finished or not.
	 * 
	 * @return true if the simulation has finished, false if not.
	 */
	public boolean isFinished() {
		return tonae.isFinished();
	}
	
	public long getHistoryID(){
		return tonae.getHistoryID();
	}

	@Override
	public void spaceViolation(double spaceOccupied, double spaceAllowed,
			double deliveryspace, HashMap<MaterialType, Integer> delivery,
			Vector<ResourceAllocation> request) {
		Vector<G_ResourceAlloc> gRequest = new Vector<G_ResourceAlloc>();
		for(ResourceAllocation a : request){
			gRequest.add(new G_ResourceAlloc(a, tonae, this));
		}
		for(G_SpaceViolationListener l : spaceViolationListeners){
			l.spaceViolation(spaceOccupied, spaceAllowed, deliveryspace, delivery, gRequest);
		}
		
	}
	
	public static void writeQueryResults(String name, QueryResult2 results){
		FileOutputStream fout;
		PrintStream print = null;
		try {
			fout = new FileOutputStream(name+".txt");
			print = new PrintStream(fout);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		if(print != null){
			print.println("Days\tCost");
			for(Object[] o : results.getEndData()){
				print.println((Integer)o[0]+"\t"+(Double)o[1]);
			}
			print.close();
		}
		else
			System.out.println("!!!File Writing Error.");
	}
}
