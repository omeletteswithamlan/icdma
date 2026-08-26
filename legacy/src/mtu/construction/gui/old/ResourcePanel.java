package mtu.construction.gui.old;

import mtu.construction.gui.wrapper.G_Activity;
import mtu.construction.gui.wrapper.G_LaborCrew;
import mtu.construction.gui.wrapper.G_Material;
import mtu.construction.gui.wrapper.G_ResourceAlloc;
import mtu.construction.icdma.Simulator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Vector;
import java.util.Map.Entry;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import mtu.construction.project.Activity;
import mtu.construction.project.MaterialType;
import mtu.construction.project.ResourceType;
import mtu.construction.project.Stock;


public class ResourcePanel extends JPanel implements ActionListener//, ChangeListener
{

	public static ResourcePanel 	rp;
	private static final long 		serialVersionUID = 1347136026333519490L;

	private Simulator 				sim;

	private JPanel					priorityListPanel;

	private JList 					priorityList;

	private JButton					priorityButtonU,
	priorityButtonD;

	private Vector<ResourceViewer> 	viewlist = new Vector<ResourceViewer>();

	private Vector<G_Activity> 		priority = new Vector<G_Activity>();

	private JTabbedPane 			activityTabs;
	private JPanel					infoLables =new JPanel();
	private LaborCrewPanel lcpanel;

	public ResourcePanel(Simulator sim, LaborCrewPanel lcpanel)
	{
		this.lcpanel = lcpanel;
		this.sim = sim;
		ResourcePanel.rp = this;

		setLayout(new BorderLayout());

		//east
		priorityListPanel =	new JPanel(new GridLayout(4, 1));
		priorityList = 		new JList(new DefaultComboBoxModel());

		JPanel priorityListListPanel = new JPanel(new BorderLayout());
		priorityListListPanel.add(new JLabel("Activity Priority"), BorderLayout.NORTH);
		priorityListListPanel.add(new JScrollPane(	priorityList,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
				BorderLayout.CENTER);

		JPanel extrapanel = new JPanel(new BorderLayout());
		extrapanel.add(priorityListListPanel,BorderLayout.CENTER);
		priorityList.setPreferredSize(new Dimension(200, 200));
		priorityList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		//activity priority buttons
		JPanel priorityListButtonPanel = new JPanel(new GridLayout(2, 1));
		priorityButtonU= 	new JButton("Up");
		priorityListButtonPanel.add(new PaddedPanel(10, priorityButtonU));
		priorityButtonD= 	new JButton("Down");
		priorityListButtonPanel.add(new PaddedPanel(10, priorityButtonD));
		extrapanel.add(priorityListButtonPanel, BorderLayout.EAST);

		add(priorityListPanel,BorderLayout.EAST);
		priorityListPanel.add(new PaddedPanel(10, extrapanel));


		JPanel np = new JPanel();
		np.setOpaque(false);
		priorityListPanel.add(np);
		np = new JPanel();
		np.setOpaque(false);
		priorityListPanel.add(np);
		np = new JPanel();
		np.setOpaque(false);
		priorityListPanel.add(np);

		extrapanel.setPreferredSize(new Dimension(200, 200));
		//end east

		priorityButtonU.addActionListener(this);
		priorityButtonD.addActionListener(this);

		//start with a null priority list
		priority.add(null);

		activityTabs = new JTabbedPane();
		activityTabs.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
		activityTabs.setTabPlacement(JTabbedPane.LEFT);
		add(activityTabs, BorderLayout.CENTER);
		//activityTabs.addChangeListener(this);

		infoLables.setVisible(true);

		add(infoLables, BorderLayout.SOUTH);

	}

	//Determine if the activity is in the priority list
	private boolean isPrioritized(G_Activity act)
	{
		for(G_Activity node : priority)
		{
			if(node == act)
				return true;
		}

		return false;
	}

	//Get the size of the priority list
	private int getStockNum()
	{
		for(int x = 0; x < priority.size(); x++)
		{
			if(priority.get(x) == null)
				return x;
		}

		return -1;
	}

	//Empty GUI list, remove inactive nodes from the priority list
	//add ready activities to the priority list, repopulate the GUI list
	private void updatePriority()
	{
		((DefaultComboBoxModel)priorityList.getModel()).removeAllElements();

		for(int x = 0; x < priority.size(); x++)
		{
			if(priority.get(x) != null && !priority.get(x).isActive())
			{
				priority.remove(x);
				x--;
			}
		}

		int stocknum = getStockNum();

		for(G_Activity node : sim.getReadyActivities())
		{
			if(!isPrioritized(node))
			{
				priority.add(stocknum, node);
				stocknum++;
			}
		}

		for(G_Activity node : priority)
			((DefaultComboBoxModel)priorityList.getModel()).addElement(new ActivityContainer(node));
	}

	public void update()
	{
		removeAll();
		add(priorityListPanel,BorderLayout.EAST);
		add(activityTabs, BorderLayout.CENTER);

		updatePriority();
		viewlist.clear();

		//is this the right place to get our allocation list from, should it not reflect changes?
		Vector<G_ResourceAlloc> alloclist = sim.getDefaultResourceAllocations();

		activityTabs.removeAll();
		for(G_ResourceAlloc alloc : alloclist)
		{
			ResourceViewer alview = new ResourceViewer(alloc, this, sim);
			viewlist.add(alview);
			alview.setVisible(true);
			if(alloc.getActivity() == null){
				//System.out.println("Stock");
				activityTabs.add("Stock", alview);
			}
			else {
				activityTabs.add(alloc.getActivity().getLabel(), alview);
				//				System.out.println(alloc.getActivity().getLabel());
			}
		}
		updateInfoLables();
	}

	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == this.priorityButtonU)
		{
			//move up
			int i = priorityList.getSelectedIndex();
			if(i <= 0)
				return;

			G_Activity node = priority.remove(i);

			priority.add(i-1, node);
			updatePriority();
			priorityList.setSelectedIndex(i-1);
		}
		else if(e.getSource() == this.priorityButtonD)
		{
			//move down
			int i = priorityList.getSelectedIndex();
			if(i < 0 || i == priority.size() - 1)
				return;

			G_Activity node = priority.remove(i);

			priority.add(i+1, node);
			updatePriority();
			priorityList.setSelectedIndex(i+1);
		}
		updateInfoLables();
	}

	public G_ResourceAlloc getResourceAllocationFromViewer(G_Activity a, boolean update)
	{
		for(ResourceViewer v : viewlist)
		{
			if(v.getAlloc().getActivity() == null) ;//System.out.println("BadAct");
			else if (a == null) ;//System.out.println("BadAct 2");
			else if(v.getAlloc().getActivity().getID() == a.getID())
				return v.constructAllocationList(update);
		}

		throw new Error("Something has gone wrong!");
	}
	//These really should be moved into simulator	
	public Vector<G_ResourceAlloc> getResourceAllocation(boolean update)
	{
		Vector<G_ResourceAlloc> ralloc = new Vector<G_ResourceAlloc>();
		for(G_Activity p : priority)
		{
			if(p != null)
				ralloc.add(getResourceAllocationFromViewer(p, update));
			//else
			//	ralloc.add(getResourceAllocationFromViewer(null));
		}

		return ralloc;
	}

	//Calculate materialRate and laborRate and display
	public void updateInfoLables()
	{
		infoLables.removeAll();
		Vector<G_ResourceAlloc> ralloc = getResourceAllocation(true);
		Stock s = sim.getStock();
		double usedspace = s.getUsedSpace();
		for(G_ResourceAlloc r : ralloc)
		{
			for(G_Material t : sim.getMaterialTypes())
			{
				if(r.getActivity().getAsPlannedMaterialUse().containsKey(t)){
					
					//NOTE: don't remove from stock, because Ordering doesn't remove from stock.
					//This will change if ordering changes.
					int requested = (int)Math.ceil(r.getActivity().getAsPlannedMaterialUse().get(t)*r.getOrder()/100);
					//double d = s.remove(t.getMaterial(), requested/*r.getRequested(t)*/);
					//usedspace += requested/*r.getRequested(t)*/ - d;
					usedspace += requested;
				}
			}
		}

		for (ResourceViewer r: viewlist)
		{
			G_ResourceAlloc a = r.constructAllocationList(true);
			double matrate = 10000;
			double labrate;
			if(a.getActivity() != null)
			{
				//Get the bottleneck material (the one with the lowest as-planned to as-requested ratio)
				//and use that as the Material rate
				for(Entry<G_Material, Integer> e : a.getActivity().getAsPlannedMaterialUse().entrySet())
				{
					double perc = (double)a.getRequested(e.getKey()) / (double)e.getValue();
					if(matrate > perc)
						matrate = perc;
				}

				//Add labor to the resourceAllocation
				putCrews(a, lcpanel.getCrews());

				//calculate the Labor rate
				labrate = a.computeWorkQuantityMultiplier(sim.getTimeFrame().getInterval(), sim.getCalendar().get(Calendar.DAY_OF_WEEK));
			}
			else
			{
				matrate = 1;
				labrate = 1;
			}

			//Display Total Space, Space Left, Material rate, and Labor rate
			r.inf.setInfo(usedspace, sim.getSpace() - usedspace, matrate * 100, labrate * 100);
		}	
	}

	//Have the resourceAllocation request as many of the crews as it can...
	private void putCrews(G_ResourceAlloc a, G_LaborCrew[] crews)
	{
		if(a == null)
			return;

		for(G_LaborCrew c : crews)
		{
			for(G_LaborCrew c2 : a.getActivity().getAsPlannedLaborUse())
			{
				if(c != null && c2 != null && c.getID() == c2.getID())
					a.requestLaborCrew(c);
			}
		}
	}
}

class ResourceViewer extends JPanel// implements ActionListener
{
	private static final long serialVersionUID = 1L;
	private AllocationPanel	materialPanel;//, laborPanel;
	private G_ResourceAlloc alloc;
	private ResourcePanel parent;
	//	private double space;

	//	private JPanel	infoLables;

	protected InfoLabels inf;

	public ResourceViewer(G_ResourceAlloc alloc, ResourcePanel p, Simulator sim)
	{
		//		if(alloc.getActivity() == null) System.out.println("Null Activity, Stock?");
		//		else System.out.println(alloc.getActivity().getLabel() + " is Good.");
		this.alloc = alloc;
		parent=p;

		setLayout(new BorderLayout());

		//alloc activity and tonae cross refrance
		materialPanel =	new AllocationPanel("Material Allocation", alloc, sim);

		add(materialPanel,BorderLayout.CENTER);

		inf = new InfoLabels();
		add(inf, BorderLayout.SOUTH);
	}

	public G_ResourceAlloc getAlloc()
	{
		return alloc;
	}

	//Create and return a new resourceallocation
	public G_ResourceAlloc constructAllocationList(boolean update)
	{
		G_ResourceAlloc ralloc = new G_ResourceAlloc(alloc.getActivity()); //create new resourceallocation based on old one...
		materialPanel.allocate(ralloc, update); //and put materials based on lineEntries
		ralloc.setWorkDays(materialPanel.getDays());
		ralloc.setWorkHours(materialPanel.getHours());
		ralloc.setWageIncentive(materialPanel.getIncentive());
		return ralloc;
	}
}

class AllocationPanel extends JPanel implements ActionListener
{
	private static final long serialVersionUID = -6980987565752727672L;
	private static final int ENTRY_SIZE = 25;
	private static final int PADDING = 5;

	protected Vector<LineEntry> lineEntries;
	protected JComboBox workSchedule;
	protected JComboBox wageIncentive = new JComboBox();
	private LineEntry topLabbbel;
	//private JLabel percentOfTotal;
	private OrderPanel order;

	public AllocationPanel(String title, G_ResourceAlloc act, Simulator sim)
	{
		JPanel mainpanel = new JPanel();
		//ScrollPanel mainpanel = new ScrollPanel();

		//edit: maby//never adds a scrole bar because mainpanel has a null layout
		JScrollPane pane = new JScrollPane(mainpanel);
		pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		pane.setBounds(0, 0, 100, 100);
		setLayout(new GridLayout(1, 1));
		//mainpanel.setLayout(new BoxLayout(mainpanel, BoxLayout.Y_AXIS));
		mainpanel.setLayout(null);
		
		if(act.getActivity() == null){
			Stock s = sim.getStock();
			lineEntries = new Vector<LineEntry>();
			lineEntries.add(new LineEntry("Material", "Stock Qty", "Space", "Cost", Color.gray));
			int space = 0;
			double cost = 0;
			int color = 0;
			Color one = new Color(200, 200, 200);
			Color two = new Color(220, 220, 220);
			for(G_Material t : sim.getMaterialTypes()){
				int amount = s.get(t.unwrap());
				if(amount > 0){
					//lineEntries.add(new LineEntry(t.getLabel(), amount+"", t.getSize()*amount+"", t.getCost()*amount+""));
					lineEntries.add(new LineEntry(t.getLabel(), amount+"", t.getSize()*amount+"", t.getCost()*amount+"", (color%2 == 0) ? one : two));
					space += t.getSize()*amount;
					cost += t.getCost()*amount;
					color++;
				}
			}
			lineEntries.add(new LineEntry("TOTALS:", "", ""+space, ""+cost, Color.white));
			int ptr = 5;
			for(LineEntry l : lineEntries)
			{
				mainpanel.add(l);
				l.setBounds(0, ptr, 500, ENTRY_SIZE);
				ptr += ENTRY_SIZE + PADDING;
			}
			mainpanel.setPreferredSize(new Dimension(10000, ptr));
			ptr += ENTRY_SIZE + PADDING;
			//add(pane);
			add(new PaddedPanel(10, pane));
		}
		else {
			JPanel reallyouterpanel = new JPanel(new BorderLayout());

			JPanel activityTabs = new JPanel(new BorderLayout());
			activityTabs.setOpaque(false);

			JPanel laborPannel = new JPanel(new GridLayout(1, 2));

			JPanel laborP1 = new JPanel(new BorderLayout());
			workSchedule = new JComboBox();
			workSchedule.addItem(new DayHourPair(5, 8));
			workSchedule.addItem(new DayHourPair(5, 9));
			workSchedule.addItem(new DayHourPair(5, 10));
			workSchedule.addItem(new DayHourPair(6, 8));
			workSchedule.addItem(new DayHourPair(7, 8));
			workSchedule.addActionListener(this);

			laborP1.add(new JLabel("Work Schedule"), BorderLayout.NORTH);
			laborP1.add(workSchedule, BorderLayout.CENTER);
			laborPannel.add(new PaddedPanel(10, laborP1));

			JPanel laborP2 = new JPanel(new BorderLayout());
			wageIncentive.addItem(new Incentive(1.0));
			wageIncentive.addItem(new Incentive(1.25));
			wageIncentive.addItem(new Incentive(1.5));
			wageIncentive.addItem(new Incentive(1.75));
			wageIncentive.addItem(new Incentive(2.0));
			wageIncentive.addActionListener(this);

			laborP2.add(new JLabel("Labor Budget"), BorderLayout.NORTH);
			laborP2.add(wageIncentive, BorderLayout.CENTER);
			laborPannel.add(new PaddedPanel(10, laborP2));



			activityTabs.add(laborPannel, BorderLayout.NORTH);
			reallyouterpanel.add(activityTabs, BorderLayout.NORTH);
			
			//Make the Order Percentage label
			//ordered = act.getTotalOrdered();
			//days = act.getActivity().getEnd() - act.getActivity().getStart();
			//percentOfTotal = new JLabel("Allocated: "+act.getTotalOrdered()+"%, Ordering: ");
			order = new OrderPanel(act, this);

			JPanel outermostpanel = new JPanel(new BorderLayout());
			//topLabbbel=new LineEntry(this);
			JPanel upperouterpanel = new JPanel(new BorderLayout());
			upperouterpanel.add(new JLabel("Activity Wide " + title), BorderLayout.NORTH);
			//upperouterpanel.add(percentOfTotal);
			upperouterpanel.add(order);
			//upperouterpanel.add(topLabbbel, BorderLayout.CENTER);

			JPanel bottomouterpanel = new JPanel(new BorderLayout());
			bottomouterpanel.add(new JLabel("Specific " + title), BorderLayout.NORTH);
			lineEntries = new Vector<LineEntry>();
			lineEntries.add(new LineEntry("Type of Material", "Quantity Ordered", "Percent of Needed", "Cost of Line Item"));
			if(act.getActivity() != null)
			{
				//int space = 0;
				//double cost = 0;
				for(Entry<G_Material, Integer> e : act.getActivity().getAsPlannedMaterialUse().entrySet()){
					lineEntries.add(new LineEntry(e.getKey(), act));
					//space += e.getValue()*e.getKey().getSize();
					//cost += e.getValue()*e.getKey().getCost();
				}
				//lineEntries.add(new LineEntry("TOTALS:", "", ""+space, ""+cost, Color.white));
			}
			else //Stock, so use all material types
			{
				for(G_Material t : sim.getMaterialTypes()){
					lineEntries.add(new LineEntry(t, null));
				}
			}
			
			bottomouterpanel.add(pane, BorderLayout.CENTER);

			PaddedPanel p = new PaddedPanel(10, upperouterpanel);
			//no double padding
			p.setBottomPadding(0);
			outermostpanel.add(p, BorderLayout.NORTH);
			outermostpanel.add(new PaddedPanel(10, bottomouterpanel), BorderLayout.CENTER);

			reallyouterpanel.add(outermostpanel, BorderLayout.CENTER);
			add(reallyouterpanel);
			int ptr = 5;
			for(LineEntry l : lineEntries)
			{
				mainpanel.add(l);
				l.setBounds(0, ptr, 500, ENTRY_SIZE);
				ptr += ENTRY_SIZE + PADDING;
			}
			mainpanel.setPreferredSize(new Dimension(10000, ptr));
			ptr += ENTRY_SIZE + PADDING;
		}
	}

	public double getOrder(){
		//if(topLabbbel == null)
		//	return 0;
		//return topLabbbel.getOrder();
		if(order == null)
			return 0;
		return order.getOrder();
	}

	public double getIncentive()
	{
		Incentive i= (Incentive)wageIncentive.getSelectedItem();
		if(i == null) return 0;
		return i.incentive;
	}

	public int getDays()
	{
		if(workSchedule == null)
			return 0;
		DayHourPair p = (DayHourPair)workSchedule.getSelectedItem();
		return p.day;
	}

	public int getHours()
	{
		if(workSchedule == null)
			return 0;
		DayHourPair p = (DayHourPair)workSchedule.getSelectedItem();
		return p.hour;
	}

	public void setBounds(int x, int y, int w, int h)
	{
		super.setBounds(x, y, w, h);

		for(LineEntry e : lineEntries)
		{
			e.setBounds(e.getX(), e.getY(), w, e.getHeight());
		}
	}

	public void allocate(G_ResourceAlloc alloc, boolean update)
	{
		if(alloc.getActivity() == null)
			return;
		int duration = alloc.getActivity().getEnd() - alloc.getActivity().getStart();
		double orderAmount = getOrder(); //daily order amount %
		alloc.setOrder(orderAmount);
		
		if(!update){
			double total = order.getTotal();
			System.out.println("Setting Total TO: "+total);
			alloc.setTotalOrdered(order.getTotal());
		}
		
		for(LineEntry e : lineEntries)
		{
			if(e.noType) ;//System.out.println("NO TYPE");

			else {

				//if(alloc.getActivity() != null) System.out.println(alloc.getActivity().getLabel() + " requests "+e.getValue()+" "+e.getType().getLabel()+"'s.");
				alloc.requestMaterial(e.getType(), e.getQuantity());
			}
		}
	}

	public void setPerc(double d)
	{
		for(LineEntry e : lineEntries)
			e.setPerc(d);
	}

	public void stateChanged(ChangeEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void actionPerformed(ActionEvent arg0) {
		ResourcePanel.rp.updateInfoLables();
	}
}

class InfoLabels extends JPanel
{
	private JLabel usedspace;
	private JLabel remainingspace;
	private JLabel matrate;
	private JLabel labrate;

	public InfoLabels()
	{
		setLayout(new GridLayout(1, 4));
		usedspace = new JLabel("Used");
		remainingspace = new JLabel("Remaining");
		matrate = new JLabel("Material Rate");
		labrate = new JLabel("Labor Rate");
		add(usedspace);
		add(remainingspace);
		add(matrate);
		add(labrate);
	}

	public void setInfo(double i, double j, double d, double e)
	{
		usedspace.setText("Space Used: " + i);
		remainingspace.setText("Remaining: " + j);
		matrate.setText("Material Rate: " + d);
		labrate.setText("Labor Rate: " + e);
	}
}

class LineEntry extends JPanel implements ChangeListener
{
	private static final long serialVersionUID = 946314503238268302L;
	private JSpinner quantity;
	private JSpinner order;
	private JLabel percentOfNeeded;
	private JLabel cost;
	private G_Material type;
	private int base;
	private AllocationPanel panel;
	private ResourcePanel rp;
	public boolean noType = false;
	private boolean topLable = false;

	public LineEntry(String alice, String bob, String clide, String dilyan)
	{
		setLayout(new GridLayout(1, 4));
		add(new JLabel (alice));
		add(new JLabel (bob));
		add(new JLabel (clide));
		add(new JLabel (dilyan));
		type=null;
		noType = true;
	}
	
	//Colored Text LineEntry
	public LineEntry(String a, String b, String c, String d, Color y){
		this(a, b, c, d);
		this.setBackground(y);
	}

	public LineEntry(AllocationPanel panel)
	{
		this(null, null, panel);
	}

	public LineEntry(G_Material rt, G_ResourceAlloc alloc)
	{	
		this(rt, alloc, null);
		//		if(alloc == null) System.out.println("Null Alloc");
		//		else if (rt == null) System.out.println("Null Resource");
		//		else if (alloc.getActivity() ==  null) System.out.println("No Activity");
		//		else System.out.println("Line entry for "+alloc.getActivity().getLabel()+" of "+rt.getLabel() );
	}

	//List of Material, Amount Requested, Percent, and Cost
	private LineEntry(G_Material rt, G_ResourceAlloc alloc, AllocationPanel panel)
	{
		this.rp = ResourcePanel.rp;
		this.panel = panel;
		type = rt;

		if (rt!=null)
		{
			add(new JLabel(rt.getLabel()));
			setLayout(new GridLayout(1, 4));
		}
		if(rt == null)
		{
			topLable=true;
			//top label for ordering and such
			setLayout(new GridLayout(1, 6));
			add(new JLabel("Precent to Order"));
			if(alloc != null && alloc.getActivity() != null) System.out.println(alloc.getActivity().getLabel() + " has Bad Type!");
			base = 100;
			quantity = new JSpinner(new SpinnerNumberModel(base, 0, 1999, 1));
			order =new JSpinner(new SpinnerNumberModel(base, 0, 1999, 1));
			order.addChangeListener(this);
			order.setPreferredSize(new Dimension(40,10));
			add(order);
			add(new JLabel("Allocation Amount"));
		}
		else if(alloc == null)
		{
			base = 0;
			quantity = new JSpinner(new SpinnerNumberModel(base, 0, 1999, 1));
		}
		else
		{
			base = alloc.getRequested((G_Material)rt);
			// restoration patch: scenario quantities exceed the old 1999 cap (e.g. 6000
			// cutting members in project 523); a SpinnerNumberModel whose value is out of
			// bounds throws and kills the panel rebuild mid-turn.
			quantity = new JSpinner(new SpinnerNumberModel(base, Math.min(0, base), Math.max(1999, base), 1));
		}

		quantity.addChangeListener(this);
		quantity.setPreferredSize(new Dimension(40,10));
		add(quantity);
		if(base != 0)
			percentOfNeeded = new JLabel("100%");
		else
			percentOfNeeded = new JLabel("N/A");
		add(percentOfNeeded);

		if(type != null)
			cost = new JLabel("$" + ((double)((int)(base * 100.0 * type.getCost())) / 100.0));
		else
			cost = new JLabel("");
		add(cost);
	}
	public double getOrder() {
		// TODO Auto-generated method stub
		if(order == null)
			return 0;
		return (double)(Integer)order.getValue();
	}
	public void stateChanged(ChangeEvent e)
	{
		double nv = getQuantity();
		//if (topLable)


		if(base != 0)
			percentOfNeeded.setText(((int)(nv * 100.0 / base)) + "%");
		else
			percentOfNeeded = new JLabel("N/A");

		if(panel == null)
			cost.setText("$" + ((double)((int)(nv * 100.0 * type.getCost())) / 100.0));
		else
			panel.setPerc(nv / 100.0);
		rp.updateInfoLables();
	}

	public int getQuantity()
	{
		//if(quantity == null){
		//	System.out.println("Too soon");
		//	return 0;
		//}
		return (Integer)quantity.getValue();
	}

	public G_Material getType()
	{
		return type;
	}

	public void setPerc(double d)
	{
		if(quantity != null)
			quantity.setValue((int)Math.ceil(d * base)); //Rounds up the %
			//quantity.setValue((int)Math.ceil(d * base)); //Doesn't Round up the %
	}
}

class Incentive
{
	public double incentive;

	public Incentive(double incentive)
	{
		this.incentive = incentive;
	}

	public String toString()
	{
		return "" + Math.round((incentive*100)-100) + "% more";
	}
}

class DayHourPair
{
	public int day;
	public int hour;

	public DayHourPair(int day, int hour)
	{
		this.day = day;
		this.hour = hour;
	}

	public String toString()
	{
		return "" + day + "x" + hour;
	}
}

class OrderPanel extends JPanel implements ChangeListener
{
	private ResourcePanel rp;
	private AllocationPanel ap;
	private G_ResourceAlloc alloc;
	private JLabel ordered;
	private JLabel orderLabel;
	private JSpinner orderOverall;
	private JSpinner orderDaily;
	private JLabel allocateLabel;
	private JSpinner allocate;
	double totalOrder;
	int duration;
	
	public OrderPanel(G_ResourceAlloc a, AllocationPanel p){
		this.alloc = a;
		this.ap = p;
		this.rp = ResourcePanel.rp;
		this.setLayout(new BorderLayout());
		
		GridLayout theGrid = new GridLayout(2, 5);
		JPanel grid = new JPanel(theGrid);
		
		
		duration = a.getActivity().getEnd() - a.getActivity().getStart();
		
		ordered = new JLabel("Already Ordered: "+a.getTotalOrdered()+"% of Total");
		orderLabel = new JLabel();
		totalOrder = a.getTotalOrdered();
		double val = 100.0/duration;
		//System.out.println("Value is: "+val+" = (" +totalOrder+" + "+100.0/duration+")");
		orderOverall = new JSpinner(new SpinnerNumberModel( Math.max(0, ( val > 100-totalOrder ? 100-totalOrder : val )) , 0, Math.max(0, 100-totalOrder), 1)); // restoration patch: keep bounds valid when over-ordered
		val = (100 - totalOrder)*duration;
		orderDaily = new JSpinner(new SpinnerNumberModel((100 > val ? val : 100), 0, val, 1));
		allocateLabel = new JLabel();
		allocate = new JSpinner(new SpinnerNumberModel(100, 0, 1000, 1));
		
		orderOverall.addChangeListener(this);
		orderDaily.addChangeListener(this);
		allocate.addChangeListener(this);
		
		grid.add(new JLabel("Order % (Total):"));
		grid.add(orderOverall);
		grid.add(new JLabel());
		grid.add(new JLabel("Allocation:"));
		grid.add(allocate);
		
		grid.add(new JLabel("Order % (Daily):"));
		grid.add(orderDaily);
		grid.add(new JLabel());
		grid.add(new JLabel());
		grid.add(new JLabel());
		
		add(ordered, BorderLayout.NORTH);
		add(grid, BorderLayout.CENTER);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if(e.getSource().equals(orderOverall)){
			orderDaily.setValue(((Double)orderOverall.getValue())*duration);
		}
		else if(e.getSource().equals(orderDaily)){
			orderOverall.setValue((Double)orderDaily.getValue()/duration);
		}
		else if(e.getSource().equals(allocate)){
			ap.setPerc((double)(Integer)allocate.getValue() / 100.0);
		}
		//Update Labels
		rp.updateInfoLables();
	}
	
	public double getOrder(){
		return (Double)orderDaily.getValue();
	}
	
	public double getTotal(){
		return totalOrder+getOrder()/duration;
	}
	
	
}