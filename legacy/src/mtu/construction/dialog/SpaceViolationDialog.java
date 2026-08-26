package mtu.construction.dialog;

import mtu.construction.gui.old.MainWindow;
import mtu.construction.gui.old.ModalThread;
import mtu.construction.gui.wrapper.G_Activity;
import mtu.construction.gui.wrapper.G_Material;
import mtu.construction.gui.wrapper.G_ResourceAlloc;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Vector;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import mtu.construction.project.Activity;
import mtu.construction.project.MaterialType;
import mtu.construction.project.TONAE;
import mtu.construction.tonae.PNode;
import mtu.construction.tonae.ResourceAllocation;


public class SpaceViolationDialog extends JDialog implements ActionListener
{
	private double allowed;

	private HashMap<MaterialType, Integer> delivery;
	private HashMap<MaterialType, JSpinner> requested = new HashMap<MaterialType, JSpinner>();

	private JPanel current;
	private JScrollPane materials;
	private JLabel diff;
	private JLabel totallabel;
	private Vector<MaterialEntry> entries;
	private Vector<ActivityEntry> entry;

	private ModalThread thread;
	private long timeStart = 0;

	private JSpinner cutSpinner;
	private JButton applyCutButton;
	private double originalTotal;
	private Vector<Double> originalOrders;

	private static double round2(double x){ return Math.round(100*x)/100.0; }

	private SpaceViolationDialog(String title, double spaceOccupied, double spaceAllowed, double deliveryspace, HashMap<MaterialType, Integer> delivery, double overstockPenalty, Vector<G_ResourceAlloc> request)
	{
		super();

		this.delivery = delivery;
		allowed = spaceAllowed;

		//System.out.println("Space allowed: " + allowed);

		setTitle(title);

		current = new JPanel();

		JPanel mainpanel = new JPanel(new BorderLayout());
		this.getContentPane().add(mainpanel);

		JButton done = new JButton("Done");
		done.addActionListener(this);
		mainpanel.add(done, BorderLayout.SOUTH);
		mainpanel.add(current, BorderLayout.CENTER);



		// Build the per-activity rows first so the space totals are known
		// before the explanatory header is laid out.
		materials = new JScrollPane(
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		entries = new Vector<MaterialEntry>();
		entry = new Vector<ActivityEntry>();
		for(G_ResourceAlloc a : request)
			entry.add(new ActivityEntry(a, this));

		originalOrders = new Vector<Double>();
		for(ActivityEntry e : entry)
			originalOrders.add(e.getOrderValue());
		originalTotal = getTotalSpace();
		double minimumCut = Math.max(0.0, round2(originalTotal - allowed));

		// Establish the limits, then the required cut
		addComponent(new JLabel("The materials ordered this turn exceed the space available on site."));
		addComponent(new JLabel("Space available on site: " + round2(allowed)));
		addComponent(new JLabel("Space required by current orders: " + round2(originalTotal)));
		addComponent(new JLabel("Minimum cut to fit on site: " + round2(minimumCut)
				+ "   (returned materials are refunded at " + Math.round(overstockPenalty*100) + "% of value)"));

		// Default option: cut exactly what is needed, spread proportionally
		// across the activities below; raise the spinner for a deeper cut.
		JPanel cutpanel = new JPanel(new GridLayout(1, 3));
		cutpanel.add(new JLabel("Space to cut:"));
		cutSpinner = new JSpinner(new SpinnerNumberModel(minimumCut, minimumCut,
				Math.max(minimumCut, round2(originalTotal)), 1.0));
		cutpanel.add(cutSpinner);
		applyCutButton = new JButton("Apply cut proportionally");
		applyCutButton.addActionListener(this);
		cutpanel.add(applyCutButton);
		addComponent(cutpanel);

		addComponent(new JLabel("Or adjust each activity's order yourself:"));

		JPanel headerpanel = new JPanel(new GridLayout(1, 4));
		headerpanel.add(new JLabel("Activity"));
		headerpanel.add(new JLabel(""));
		headerpanel.add(new JLabel("Enter your space"));
		headerpanel.add(new JLabel("Space occupied"));

		addComponent(headerpanel);

		for(ActivityEntry act : entry)
			addComponent(act);
		/*
		for(Entry<MaterialType, Integer> e : delivery.entrySet())
		{
			if(e.getValue() != 0)
			{
				MaterialEntry en = new MaterialEntry(e.getKey(), e.getValue().intValue(), this);
				addComponent(en);
				entries.add(en);
				requested.put(e.getKey(), en.getSpinner());
			}
		}
		*/

		totallabel = new JLabel();
		JPanel footerpanel = new JPanel(new GridLayout(1, 4));
		footerpanel.add(new JLabel());
		footerpanel.add(new JLabel());
		footerpanel.add(new JLabel("Total space required:"));
		footerpanel.add(totallabel);
		addComponent(footerpanel);

		diff = new JLabel();
		JPanel differpanel = new JPanel(new GridLayout(1, 4));
		differpanel.add(new JLabel());
		differpanel.add(new JLabel());
		differpanel.add(new JLabel("Space still to remove:"));
		differpanel.add(diff);
		addComponent(differpanel);

		update();
		setBounds(100, 100, 500, 500);
		
		if(TONAE.paperGant){
			System.out.println("<======================= Event  Occured! =======================>");
			System.out.println("Space Violation Has Occurred!");
			System.out.println("<===============================================================>");
		}
		timeStart = System.currentTimeMillis();
		
		//setModal(true); 
		setModal(false);
		setVisible(true);
		
		thread = new ModalThread();
		thread.start();
	}

	private void addComponent(JComponent c)
	{
		current.setLayout(new BorderLayout());
		current.add(c, BorderLayout.NORTH);
		JPanel np = new JPanel();
		current.add(np, BorderLayout.CENTER);
		current = np;
	}

	public static void show(String title, double spaceOccupied, double spaceAllowed, double deliveryspace, HashMap<MaterialType, Integer> delivery, double overstockPenalty, Vector<G_ResourceAlloc> request)
	{
		new SpaceViolationDialog(title, spaceOccupied, spaceAllowed, deliveryspace, delivery, overstockPenalty, request);
	}

	public void actionPerformed(ActionEvent a)
	{
		if(a.getSource() == applyCutButton)
		{
			// Scale every activity's original order down by the same factor so
			// the requested cut is spread proportionally.
			double cut = ((Number)cutSpinner.getValue()).doubleValue();
			double factor = originalTotal <= 0 ? 0.0 : Math.max(0.0, 1.0 - cut/originalTotal);
			for(int i = 0; i < entry.size(); i++)
				entry.get(i).setOrderValue(originalOrders.get(i) * factor);
			update();
			return;
		}

		double val = 0.0;

		//Calcualte space
		//for(Entry<MaterialType, JSpinner> e : requested.entrySet())
		//	val += ((Integer)e.getValue().getValue()).floatValue() * e.getKey().getSize();
		//for(ActivityEntry act : entry)
		//	val += act.getArea();
		val = getTotalSpace();

		if(val > allowed)
		{
			WarningDialog.show("Warning", "Your orders still require " + round2(val) +
					" units of space, but the site only has " + round2(allowed) +
					". Cut at least " + round2(val - allowed) + " more before continuing.",
					"image/warning.jpg", false/*true*/, this.getOwner());//false was changed to true tofix conflicting modal dialogs
		}
		else
		{
			if(TONAE.paperGant){
				System.out.println("Space Violation Decision took "+ ((System.currentTimeMillis() - timeStart)/1000) + " seconds.");
				System.out.println("Time since \"Sim\" button pressed: "+ ((System.currentTimeMillis() - MainWindow.lastSimPressed())/1000) + " seconds.");
			}
			/*
			for(Entry<MaterialType, Integer> en : delivery.entrySet())
			{
				if(requested.containsKey(en.getKey()))
				{
					JSpinner spinner = requested.get(en.getKey());
					int newval = (Integer)spinner.getValue();
					delivery.put(en.getKey(), newval);
				}
			}
			*/
			//Place the new amounts into delivery
			delivery.clear();
			for(ActivityEntry e : entry){
				/*for(Entry<G_Material, Integer> x : e.getActivity().getAsPlannedMaterialUse().entrySet()){
					int amt = 0;
					MaterialType t = x.getKey().unwrap();
					if(t==null) System.out.println("No MaterialType..");
					else if(delivery.containsKey(t))
						amt = delivery.get(t);
					delivery.put(x.getKey().unwrap(), (e.getQuantity() * x.getValue()) + amt);
				}*/
				HashMap<G_Material, Integer> allocation = e.getAllocation();
				for(Entry<G_Material, Integer> x : allocation.entrySet()){
					int amt = 0;
					MaterialType t = x.getKey().unwrap();
					if(t==null) System.out.println("No MaterialType..");
					else if(delivery.containsKey(t))
						amt = delivery.get(t);
					delivery.put(x.getKey().unwrap(), x.getValue() + amt);
				}
				
				//Reset the percentage
				e.resetPercentage();
			}
			
			setVisible(false);
			if(thread != null) thread.die();
		}
	}

	private double getTotalSpace()
	{
		double total = 0.0;

		//for(MaterialEntry e : entries)
		//	total += e.getArea();
		for(ActivityEntry act : entry)
			total += act.getArea();

		return total;
	}

	public void update()
	{
		double total = getTotalSpace();
		totallabel.setText("" + round2(total));
		diff.setText(total > allowed ? "" + round2(total - allowed) : "0 (fits on site)");
	}
}

class MaterialEntry extends JPanel implements ChangeListener
{
	private SpaceViolationDialog dialog;
	private MaterialType type;
	private JSpinner spinner;
	private JLabel spacelabel;

	public MaterialEntry(MaterialType t, int quant, SpaceViolationDialog d)
	{
		dialog = d;
		type = t;
		setLayout(new GridLayout(1, 5));
		add(new JLabel(t.getDescription()));

		add(new JLabel("" + t.getSize()));

		spinner = new JSpinner(new SpinnerNumberModel(quant, 0, Math.max(10000, quant), 1)); // restoration patch: scenario quantities may exceed the old cap
		spinner.addChangeListener(this);
		add(spinner);

		spacelabel = new JLabel("Space");
		add(spacelabel);

		update();
	}

	public JSpinner getSpinner()
	{
		return spinner;
	}

	public void update()
	{
		spacelabel.setText("" + Math.round(100*getArea())/100);
	}

	public double getArea()
	{
		return (double)(getQuantity()) * type.getSize();
	}

	public int getQuantity()
	{
		return (Integer)spinner.getValue();
	}

	public void stateChanged(ChangeEvent arg0)
	{
		update();
		dialog.update();
	}
}

class ActivityEntry extends JPanel implements ChangeListener
{

	private SpaceViolationDialog dialog;
	private JSpinner spinner;
	private JLabel spacelabel;
	private G_ResourceAlloc p;
	private double oldPercentage;
	private HashMap<G_Material, Integer> allocation;
	private int duration;
	double space;
	
	public ActivityEntry(G_ResourceAlloc p, SpaceViolationDialog dialog){
		
		this.p = p;
		this.dialog = dialog;
		oldPercentage = p.getOrder();
		allocation = new HashMap<G_Material, Integer>();
		duration = p.getActivity().getEnd() - p.getActivity().getStart();
		
		setLayout(new GridLayout(1,5));
		
		//int quant1 = 0;
		//int quant2 = 0;
		//int duration = p.getActivity().getEnd() - p.getActivity().getStart();
		//for(Entry<G_Material, Integer> e : p.getActivity().getAsPlannedMaterialUse().entrySet()){
		//	quant1 += e.getKey().getSize()*e.getValue();
		//	quant2 += e.g
		//}
		double quant = p.getOrder();
		System.out.println("Quant: "+quant);
		spinner = new JSpinner(new SpinnerNumberModel(quant, 0, Math.max(10000, quant), 1)); // restoration patch: scenario quantities may exceed the old cap
		spinner.addChangeListener(this);
		
		spacelabel = new JLabel("Space");
		
		add(new JLabel(p.getActivity().getLabel())); //Activity Name
		add(new JLabel());
		add(spinner);
		add(spacelabel);
		
		update();
	}
	
	public void update(){
		//Update material quantities
		space = 0;
		for(Entry<G_Material, Integer> e : p.getActivity().getAsPlannedMaterialUse().entrySet()){
			int amount = (e.getValue()*getQuantity())/100;
			allocation.put(e.getKey(), amount);
			space += amount * e.getKey().getSize();
		}
		spacelabel.setText("" + Math.round(100*getArea())/100);
	}

	public double getOrderValue(){
		return ((Number)spinner.getValue()).doubleValue();
	}

	public void setOrderValue(double v){
		spinner.setValue(Double.valueOf(Math.max(0.0, v)));
	}
	
	public double getArea()
	{
		//double space = 0;
		//int percent = getQuantity();
		//for(Entry<G_Material, Integer> e : p.getActivity().getAsPlannedMaterialUse().entrySet()){
		//	space += e.getKey().getSize()*e.getValue();
		//}
		return space;
	}
	
	public int getQuantity()
	{
		return (int)(double)(Double)spinner.getValue();
	}
	
	public void resetPercentage(){
		p.setOrder(getQuantity()); //Set the new order amount
		double difference = (oldPercentage - p.getOrder())/duration;
		double newTotal = p.getTotalOrdered() - difference;
		System.out.println("NewTotal: "+newTotal);
		p.setTotalOrdered(newTotal);
	}
	
	public JSpinner getSpinner()
	{
		return spinner;
	}
	
	public G_Activity getActivity(){
		return p.getActivity();
	}
	
	public HashMap<G_Material, Integer> getAllocation(){
		return allocation;
	}
	
	@Override
	public void stateChanged(ChangeEvent e) {
		update();
		dialog.update();
	}

}
