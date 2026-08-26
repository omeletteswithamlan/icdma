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



		addComponent(new JLabel("Out of space. Selet materials from this dilivery to send back"));
		addComponent(new JLabel("at "+Math.round(overstockPenalty*100)+"% of the origional value"));

		JPanel headerpanel = new JPanel(new GridLayout(1, 4));
		headerpanel.add(new JLabel("Material Name"));
		headerpanel.add(new JLabel("Size"));
		headerpanel.add(new JLabel("Amount"));
		headerpanel.add(new JLabel("Space Occupied"));

		addComponent(headerpanel);

		materials = new JScrollPane(
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		entries = new Vector<MaterialEntry>();
		entry = new Vector<ActivityEntry>();
		for(G_ResourceAlloc a : request){
			ActivityEntry act = new ActivityEntry(a, this);
			addComponent(act);
			entry.add(act);
		}
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
		footerpanel.add(new JLabel("Total Area:"));
		footerpanel.add(totallabel);
		addComponent(footerpanel);

		diff = new JLabel();
		JPanel differpanel = new JPanel(new GridLayout(1, 4));
		differpanel.add(new JLabel());
		differpanel.add(new JLabel());
		differpanel.add(new JLabel("To Remove:"));
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
		double val = 0.0;

		//Calcualte space
		//for(Entry<MaterialType, JSpinner> e : requested.entrySet())
		//	val += ((Integer)e.getValue().getValue()).floatValue() * e.getKey().getSize();
		//for(ActivityEntry act : entry)
		//	val += act.getArea();
		val = getTotalSpace();

		if(val > allowed)
		{
			WarningDialog.show("Warning", "You have selected " + val +
					" units of material! You are only allowed to select " + allowed +
					" units. Please change your selected values.", "image/warning.jpg", false/*true*/, this.getOwner());//false was changed to true tofix conflicting modal dialogs
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
		totallabel.setText("" + total);
		diff.setText("" + (total - allowed));
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

		spinner = new JSpinner(new SpinnerNumberModel(quant, 0, 10000, 1));
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
		spinner = new JSpinner(new SpinnerNumberModel(quant, 0, 10000, 1));
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
