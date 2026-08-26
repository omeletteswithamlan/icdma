package mtu.construction.project;

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
import java.util.Vector;
import java.util.Map.Entry;

import mtu.construction.serialize.Serializer;
import mtu.construction.tonae.ANode;
import mtu.construction.tonae.AgentM;
import mtu.construction.tonae.Arc;
import mtu.construction.tonae.CostSchedule;
import mtu.construction.tonae.PNode;
import mtu.construction.tonae.QueryResult;
import mtu.construction.tonae.QueryResult2;
import mtu.construction.tonae.ResourceAllocation;
import mtu.construction.variable.Condition;
import mtu.construction.variable.ContinV;
import mtu.construction.variable.DiscreteV;
import mtu.construction.variable.Rule;

/*
 * Things we need:
 * 1. Interval
 * 2. TONAE for: materialTypes, ANode set, currentTimeStep (= currentState.getTime()), baseline activities, baseline materialTypes, rules, readyList (= currentState.getReadyList()), getAsBuiltSchedule
 * 3. CheckLaborCompliment does nothing for querymode?
 * 4. Reference to the project?...
 */
/**
 * This class is used to do a threaded query run.
 * It contains everything needed to run an update, which mirror many methods in tonae.
 * That means that any change to a similar method in TONAE should probably be done here as well.
 */
public class Updater {
	private TONAEState currentState;
	private Project built;
	private Project plan;
	private Vector<Rule> rules;
	
	/**
	 * Construct an Updater
	 * 
	 * @param state - A clone of Tonae.currentState
	 * @param asBuilt - reference to the as-planned project
	 * @param asPlanned - reference to the as-Planned project
	 * @param rules - a list of rules(conditions and effects) to be applyed to the simulation each turn.
	 */
	public Updater(TONAEState state, Project asBuilt, Project asPlanned, Vector<Rule> rules){
		currentState = state;
		built = asBuilt;
		plan = asPlanned;
		this.rules = rules;
	}
	
	public Project getProject(){
		return built;
	}
	
	/**
	 * Increments the project by one day. Updates the as-built schedule with new cost info
	 *
	 */
	public void update(Vector<ResourceAllocation> resourcerequest, HashMap<MaterialType, Integer> soldmaterial, LaborCrew[] crews, LaborCrew unmapped, LaborCrew hired)
	{
		buildPurchaseList(resourcerequest);
		currentState.environment.update(built, currentState.getTime(), currentState.aNodeSet, currentState.purchasedmaterial);

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
			HashSet<Activity> act = rule.apply(currentState.environment, currentState.readyList); //requires currentState.readyList, not "this"
			if(act != null)
				rulemap.put(rule, act);
		}

		currentState.environment.updateValues(currentState.purchasedmaterial);

		//allow these functions to handle special rules
		checkLabor(resourcerequest, crews, unmapped, hired, rulemap);
		buildRequestList(resourcerequest, soldmaterial, crews, rulemap);
		checkMaterial(rulemap);

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

		/* If all this does is notify GUI, then it is not needed for querying */
		//notify GUI of any rules that triggered.
//		for(Entry<Rule, HashSet<Activity>> e : rulemap.entrySet())
			//e.getKey().trigger(rulelisteners, e.getValue(), null);
		//TODO: RULE LISTENER

		/**************************************************************************
		 * 
		 * this is the end of the "player's turn" 
		 * everything after this modifies or cleans up the data structures
		 * based on the events above
		 * 
		 **************************************************************************/

		//manageResourcesOriginal(resourcerequest, hired); //Original stuff
		manageResources(resourcerequest, true); //New stuff to rid items from resourceAllocation

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
	}
	
	private void buildPurchaseList(Vector<ResourceAllocation> resourcerequest)
	{
		Stock cs = currentState.stock.clone(); //clone the stock so we can remove from it
		for(ResourceAllocation alloc : resourcerequest)
		{
			for(MaterialType m : getProject().getMaterialTypes())
			{
				int amt = cs.remove(m, alloc.getRequested(m));		//remove what we can from stock,
				purchaceMaterial(m, alloc.getRequested(m) - amt);		//purchace the rest
			}
		}
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
	
	private void checkDrivingMaterials(Vector<ResourceAllocation> resourcerequest) 
	{

		for (Activity a: plan.getActivities())//TODO: should access only current activities
		{
			int min=50000;
			// TODO dance as if no one is watching
			if(a.getDrivingMaterials().isEmpty())
				min=100;
			for(int dm: a.getDrivingMaterials())
			{
				for (ResourceAllocation r:resourcerequest)
				{

					//Make sure the resourceAllocation if for this activity
					if(a.equals(r.getActivity())){

						//get materialType for dm
						for(MaterialType matType:plan.getMaterialTypes())
						{
							if (a.getMaterialUse().get(matType) != null && matType.getID()==dm) //Make sure the activity uses the material
							{

								//get purchased amount for dm
								int available = r.getRequested(matType);

								//get baseline amount for dm
								int base = (a.getMaterialUse().get(matType));

								int temp = base==0? 0 : (available/base)*100;

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
			DiscreteV var = (DiscreteV) currentState.environment.getVariable(a, "Driving Material Available");
			if (var == null) // restoration patch: mirrors TONAE.checkDrivingMaterials guard
				continue;
			var.setState(""+(min/10), 1);
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
									}
								}
							}
						}
					}
				}
			}
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
			for(DiscreteV var : currentState.environment.getDiscreteVariables(act)){
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
		DiscreteV var = (DiscreteV)currentState.environment.getGlobalVariable("Month");
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
		DiscreteV var = (DiscreteV)currentState.environment.getGlobalVariable("Day");
		if(var != null)
			var.setState(""+this.currentState.currentDate.get(Calendar.DAY_OF_WEEK), 1);
	}
	
	//TODO: This will be going away once we have a database infrastructure for mapping hired labor onto simulation variables. MW (8-25-08)
	private void checkLabor(Vector<ResourceAllocation> resourcerequest, LaborCrew[] crews, LaborCrew unmapped, LaborCrew hired, HashMap<Rule, HashSet<Activity>> rulelist)
	{
		boolean laborchanged = false;

		for(ResourceAllocation alloc : resourcerequest)
		{
			if(alloc.getActivity() != null)
			{
				DiscreteV lab_avail = (DiscreteV)currentState.environment.getVariable(alloc.getActivity(), "Labor Available");
				DiscreteV lab_low = (DiscreteV)currentState.environment.getVariable(alloc.getActivity(), "Low Labor");

				if(lab_avail == null || lab_low == null) // restoration patch: mirrors TONAE.checkLabor guard
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

					//Remove a random worker from the laborcrew
					Random r = new Random();
					LaborType t = types.get(r.nextInt(types.size()));
					boolean removed = false;

					for(LaborCrew c : alloc.getActivity().getLaborUse())
					{
						for(LaborCrew o : crews)
						{
							if(c.getID() == o.getID() && !removed && o.remove(t) == 1)
							{
								//Tell rulelistener which laborer was removed, from which crew
								//trigger("Low Labor", "True", new MissingLaborContainer(o, t), rulelist);
								HashMap<LaborCrew, LaborType> map = new HashMap<LaborCrew, LaborType>();
								map.put(o, t);
								trigger("Low Labor", "True", map, rulelist);

								removed = true;
								laborchanged = true;
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Trigger and remove all entries for a rule
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
			//TODO: RULE LISTENER
			//e.getKey().trigger(rulelisteners, e.getValue(), o);
			list.remove(e.getKey());
		}
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
	 * Update Labor Request and Material request based on available crews and sold materials...
	 * 
	 * @param resourcerequest
	 * @param soldmaterial
	 * @param crews
	 * @param rulelist
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
	 * Special Variable check
	 * Check the state of the "Material Available" variable for the "No Material Delivery" rule.
	 * 
	 * @param rulelist
	 */
	private void checkMaterial(HashMap<Rule, HashSet<Activity>> rulelist)
	{
		// restoration patch: match DB spelling "Material Available" (see TONAE.checkMaterial)
		DiscreteV mat_avail = (DiscreteV)currentState.environment.getGlobalVariable("Material Available");
		if(mat_avail != null && mat_avail.getState().equals("False"))
			currentState.purchasedmaterial.clear();
	}
	
	//Big Test Method
	/**
	 * Manage all the resource useage calculations
	 */
	private void manageResources(Vector<ResourceAllocation> resourcerequest, boolean querymode)
	{
		/* PER PROJECT STUFF */
		int interval = getProject().getTimeFrame().getInterval();
		int dayofweek = currentState.currentDate.get(Calendar.DAY_OF_WEEK);
		double cost = 0;
		CostSchedule sched = currentState.mathAgent.getAsBuilt();
		int day = currentState.getTime();
		
		//Determine if we have enough space
		double spaceneeded = 0.0;
		for(Entry<MaterialType, Integer> e : currentState.purchasedmaterial.entrySet())
			spaceneeded += e.getValue() * e.getKey().getSize();

		double allowed = currentState.stock.getAvailableSpace();

		//Reject extra materials
		for(Entry<MaterialType, Integer> e : currentState.purchasedmaterial.entrySet())
		{
			int amt = e.getValue() - currentState.stock.add(e.getKey(), e.getValue());
			int camt = 0;
			if(currentState.rejectedmaterial.containsKey(e.getKey()))
				camt = currentState.rejectedmaterial.get(e.getKey());
			currentState.rejectedmaterial.put(e.getKey(), amt + camt);
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
		for(PNode act : currentState.getReadyList()){

			//1. Compute work rate
			ResourceAllocation alloc = getResourceAllocation(act.getParentAct(), resourcerequest);//Get RA for this activity
			double baseworkrate = alloc.computeWorkQuantityMultiplier(interval, dayofweek);
			//double materialrate = alloc.computeMaterialQuantityMultipler(); //Relies on getAvailable amount **********
			double materialrate = -1;

			ContinV productivity = (ContinV)currentState.environment.getContinuousVariable(act.getParentAct(), "Productivity");
			double workrate = baseworkrate * productivity.getState();
			
			HashMap<MaterialType, Integer> avail = new HashMap<MaterialType, Integer>();
			
			for(Entry<MaterialType, Integer> e : act.getParentAct().getMaterialUse().entrySet())
			{
				//2. Remove requested materials from stock
				int available = currentState.stock.remove(e.getKey(), alloc.getRequested(e.getKey())); //put Amount in Available
				avail.put(e.getKey(), available);
				
				//2.5 Compute material rate
				double perc = (double)available / (double)e.getValue();
				if(materialrate == -1 || perc < materialrate)
					materialrate = perc;
			}
			
			if(materialrate == -1) materialrate = 0;

			double rate = workrate;
			if(materialrate < rate)
				rate = materialrate;
			
			for(Entry<MaterialType, Integer> e : act.getParentAct().getMaterialUse().entrySet())
			{
				int available = avail.get(e.getKey());
				
				//3. Compute the amount of work done, use resources... Done in percentages
				int getAmt = (int)(e.getValue() * rate); //baseuse * rate
				int amtused = getAmt > available ? available : getAmt; //take Amount from Available
				available = available - amtused;
				act.setTotalWorkLeft(act.getTotalWorkLeft() - e.getKey().getCost() * (double)amtused); //setTotalWorkLeft
				
				//4. Cost computation
				sched.addMaterial(act.getParentAct(), day, amtused * e.getKey().getCost()); //here as well
				cost += amtused * e.getKey().getCost();
				
				//5. (#6 on the list) available is the amount that needs to go back into stock...
				currentState.stock.add(e.getKey(), available);
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
					int cday = currentState.currentDate.get(Calendar.DAY_OF_WEEK);

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
		
		sched.setStockValue(day, currentState.stock.getValue());

		//doing this circumvents how this was intended to work, but the intended way has a bug and I don't have
		//the time to fix it at the moment. Normally, fast_day_total gets updated by the addIndirect, addMaterial,
		//and addLabor calls to sched. But, there is a bug with that. So, I just compute the cost change
		if(sched.getQueryFuturesTotal().containsKey(day-1))
			cost += sched.getQueryFuturesTotal().get(day - 1);
		sched.setqueryFuturesTotal(cost);

		//Update the schedule tracker (day, total day's cost)
		sched.updateDayTotal(currentState.getTime());
	}
	
	/**
	 * End of the day, Remove the present nodes from the readylist if they have finished
	 * 
	 * @param currentProj - the current state
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
			if(out.getHeadNode().getEarlyStart() <= currentState.getTime() + 1)
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
	 * Increment all present nodes by one timestep
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
	 * Progress the present node by one timestep
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
	
	/**
	 * Determine which activities are "active" for this time step
	 * set them as "active" and put them in the readylist
	 * 
	 * @param currentProj = the current state
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
	 * Delay an activity by a certain amount
	 * Activity
	 * 
	 * @param a
	 * @param delay
	 */
	public void delayActivity(Activity a, int delay)
	{
		for (ANode node:currentState.aNodeSet)
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
	 * Delays an activity by a certain amount. This handles negative delays properly.
	 * PNode
	 */
	public void delayActivity(PNode node, int delay)
	{	
		ANode start = (ANode)node.getInPrimaryArc().getTailNode();
		ANode end = (ANode)node.getOutPrimaryArc().getHeadNode();

		end.setEarlyStart(end.getEarlyStart() + delay);
		if(end.getEarlyStart() < currentState.getTime())
			end.setEarlyStart(currentState.getTime());
		if(end.getEarlyStart() < start.getEarlyStart())
			end.setEarlyStart(start.getEarlyStart());

		boolean finished = false;
		while(!finished)
		{
			finished = true;
			Iterator<ANode> iter = currentState.aNodeSet.iterator();
			while(iter.hasNext())
			{
				end = iter.next();
				//make sure this actually is an end node
				if(end.getOutPrimaryArc() == null)
				{
					start = (ANode)end.getInPrimaryArc().getTailNode().getInPrimaryArc().getTailNode();
					//make sure the activity hasn't started yet before moving it around
					if(start.getEarlyStart() > currentState.getTime())
					{
						Set<Arc> constraints = start.getInConstraints();
						Iterator<Arc> citer = constraints.iterator();
						//make sure no activities get set to start before this latest day
						int latest = currentState.getTime();
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
	
	//gets the resource allocation from the work performed list
	public static ResourceAllocation getResourceAllocation(Activity act, Vector<ResourceAllocation> workperformed)
	{
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
	
	/**
	 * Indicates whether or not the project is finished
	 * 
	 * @return true if there are no more activities in this project, false otherwise
	 */
	public boolean isFinished()
	{
		return currentState.getTime() >= getLastTimeStep();
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
	
	/**
	 * "Query the future"
	 * Run the simulation starting from currentState until "the end"
	 * then return the result of the query.
	 * 
	 * @return Some kinda cost distribution <x,y> = <day, totalCost>
	 */
	public TreeMap<Integer, Double> queryFuture()
	{
		//Save the current state
		TONAEState p = currentState;

		//Work starting with a copy of the current state
		currentState = (TONAEState)Serializer.copy(currentState);

		//Continue progress until the project finishes or 1000 timesteps (days, weeks) have passed
		// (Run one "full" simulation)
		while(!isFinished() && currentState.getTime() < 1000)
		{
			update(getDefaultResourceAllocation(), new HashMap<MaterialType, Integer>(), getDefaultLaborCrews(), new LaborCrew(0, "Unmapped"), new LaborCrew(0, "Hired"));
		}

		//System.out.println("days = "+currentState.getTime());
		//currentState is now at the end of the project
		TONAEState completed = currentState;

		//Restore the current state
		currentState = p;

		return completed.mathAgent.getAsBuilt().getQueryFuturesTotal();
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
		//QueryResult result = new QueryResult(bucketsize);
		QueryResult2 result = new QueryResult2();

		for(int x = 0; x < num; x++)
			result.addResult(queryFuture());

		return result;
	}
	
	//Create and return a default ResourceAllocation for the running activities (and stock)
	public Vector<ResourceAllocation> getDefaultResourceAllocation()
	{
		Vector<ResourceAllocation> alloc = new Vector<ResourceAllocation>();

		alloc.add(new ResourceAllocation(null));	//add an empty resource allocation for the stock

		//for each activity in the ready list
		for(PNode node : currentState.readyList)
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
}
