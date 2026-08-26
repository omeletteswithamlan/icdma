package mtu.construction.gui.old;

import mtu.construction.gui.wrapper.G_LaborCrew;

import mtu.construction.icdma.Simulator;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import mtu.construction.project.LaborCrew;
import mtu.construction.project.LaborType;

/**
 * Works with a copy of the crews from TONAE
 * updateProductivity() is not used.
 * 
 * use getHired() and getUnmapped() to get the final datas
 * 
 * @author kkaaikal
 *
 */
public class LaborCrewPanel extends JPanel implements ActionListener
{
	private static final long serialVersionUID = 1L;
	public static LaborCrewPanel lcpanel;
	
	protected TreeMap<Integer, G_LaborCrew> crewmap = new TreeMap<Integer, G_LaborCrew>();
	
	//Unassigned Laborers
	protected TreeMap<LaborType, UnassignedListElement> upool = new TreeMap<LaborType, UnassignedListElement>();
	
	//Assigned Laborers (and crew they're assigned to)
	//Crew(ID) + Laborers(LabotType, Amount) From plan
	protected TreeMap<Integer, TreeMap<LaborType, AssignedListElement>> apool = new TreeMap<Integer, TreeMap<LaborType, AssignedListElement>>();
	
	private JTabbedPane activityTabs;
	
	private LaborCrew hired; //Dummy Laborcrew containing all hired labor
	private LaborCrew unmapped; //Dummy laborcrew containing all unmapped labor
	private LaborCrew[] crewlist;
	
	Simulator sim;
	
	public LaborCrewPanel(Simulator s)
	{
		sim = s;
		setLayout(new BorderLayout());
		JPanel northpanel = new JPanel(new GridLayout(1, 2));
		northpanel.add(new JLabel("Assigned Labor"));
		northpanel.add(new JLabel("Unassigned Labor"));
		add(northpanel, BorderLayout.NORTH);
		
		activityTabs = new JTabbedPane();
		activityTabs.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
		activityTabs.setTabPlacement(JTabbedPane.LEFT);
		
		JPanel centerpanel = new JPanel(new GridLayout(1, 2));
		centerpanel.add(activityTabs);
		
		JPanel upanel = new JPanel(new BorderLayout());
		JPanel headerpanel = new JPanel(new GridLayout(1, 4));
		headerpanel.add(new JLabel("Labor Type"));
		headerpanel.add(new JLabel("Quantity"));
		headerpanel.add(new JLabel());
		headerpanel.add(new JLabel());
		upanel.add(headerpanel, BorderLayout.NORTH);
		
		
		JPanel current = new JPanel();
		JPanel last = upanel;
		
		for(LaborType t : sim.getLaborTypes())
		{
			last.add(current, BorderLayout.CENTER);
			current.setLayout(new BorderLayout());
			UnassignedListElement e = new UnassignedListElement(t, this);
			upool.put(t, e);
			current.add(e, BorderLayout.NORTH);
			
			last = current;
			current = new JPanel();
		}
		
		centerpanel.add(upanel);
		
		add(centerpanel, BorderLayout.CENTER);
		
		unmapped = new LaborCrew(0, "Unmapped");
		hired = new LaborCrew(0, "Hired");
		crewlist = new LaborCrew[sim.getLaborCrews().length];
		int x = 0;
		for(G_LaborCrew l : sim.getLaborCrews())
		{
			crewlist[x] = l.getCrew();
			x++;
		}
	}
	
	public void update()
	{
		activityTabs.removeAll();
		
		crewlist = new LaborCrew[sim.getLaborCrews().length];
		int x = 0;
		for(G_LaborCrew l : sim.getLaborCrews())
		{
			crewlist[x] = l.getCrew();
			x++;
		}
		for(G_LaborCrew l : sim.getLaborCrews())
		{
			TreeMap<LaborType, AssignedListElement> assignedLaborList = new TreeMap<LaborType, AssignedListElement>();
			JPanel cpanel = new JPanel(new BorderLayout());
			JPanel headerpanel = new JPanel(new GridLayout(1, 3));
			headerpanel.add(new JLabel("Labor Type"));
			headerpanel.add(new JLabel("Needed"));
			headerpanel.add(new JLabel("Hired"));
			cpanel.add(headerpanel, BorderLayout.NORTH);
			
			JPanel current = new JPanel();
			JPanel last = cpanel;
			for(LaborType t : l.getLaborerTypes())
			{
				last.add(current, BorderLayout.CENTER);
				
				current.setLayout(new BorderLayout());
				AssignedListElement e = new AssignedListElement(this, t, l.getAmount(t));
				assignedLaborList.put(t, e);
				current.add(e, BorderLayout.NORTH);
				
				last = current;
				current = new JPanel();
			}
			
			activityTabs.add(l.getLabel(), cpanel);
			crewmap.put(l.getID(), l);
			apool.put(l.getID(), assignedLaborList);
		}
	}

	public void actionPerformed(ActionEvent arg0)
	{
	}
	
	public void updateMaxValues()
	{
		for(Entry<Integer, TreeMap<LaborType, AssignedListElement>> e : apool.entrySet())
		{
			for(Entry<LaborType, AssignedListElement> e2 : e.getValue().entrySet())
			{
				for(Entry<LaborType, UnassignedListElement> e3 : upool.entrySet())
				{
					if(e2.getKey() == e3.getKey())
					{
						//Assigned maxValue = value + amount unassigned
						e2.getValue().setMax(e2.getValue().getValue() + e3.getValue().quantity);
						//System.out.println("SetMax (" + e2.getValue().getValue() + ", " + e3.getValue().quantity + ")");
					}
				}
			}
		}
	}
	
	public void removeFromUnassigned(LaborType t, int i)
	{
		upool.get(t).quantity -= i;
		upool.get(t).update();
		
		updateMaxValues();
	}
	
	//Return a laborcrew with all the hired labor
	public LaborCrew getHired()
	{
		TreeMap<LaborType, Integer> hirelist = new TreeMap<LaborType, Integer>();
		
		for(Entry<Integer, TreeMap<LaborType, AssignedListElement>> e : apool.entrySet())
		{
			for(Entry<LaborType, AssignedListElement> e2 : e.getValue().entrySet())
			{
				int q = e2.getValue().getValue() - e2.getValue().needquant;
				
				if(!hirelist.containsKey(e2.getKey()))
					hirelist.put(e2.getKey(), 0);
				
				hirelist.put(e2.getKey(), hirelist.get(e2.getKey()) + q);
			}
		}
		
		hired.clear();
		for(Entry<LaborType, Integer> e : hirelist.entrySet())
		{
			if(e.getValue() > 0)
				hired.add(e.getKey(), e.getValue());
		}
		
		return hired;
	}
	
	public LaborCrew getUnmapped()
	{
		unmapped.clear();
		
		for(Entry<LaborType, UnassignedListElement> e : upool.entrySet())
		{
			if(e.getValue().quantity > 0)
				unmapped.add(e.getKey(), e.getValue().quantity);
		}
		
		return unmapped;
	}
	
	public void resetLaborCrewAssignment()
	{
		G_LaborCrew[] oldcrew = sim.getLaborCrews();
		crewlist = new LaborCrew[oldcrew.length];
		for(int x = 0; x < crewlist.length; x++)
			crewlist[x] = oldcrew[x].getCrew();
		unmapped = new LaborCrew(0, "Unmapped");
		hired = new LaborCrew(0, "Hired");

		for(Entry<LaborType, UnassignedListElement> e : upool.entrySet())
		{
			e.getValue().quantity = 0;
			e.getValue().update();
		}
		
		for(Entry<Integer, TreeMap<LaborType, AssignedListElement>> e : apool.entrySet())
		{
			for(Entry<LaborType, AssignedListElement> e2 : e.getValue().entrySet())
				e2.getValue().reset();
		}
		
		updateMaxValues();
	}
	
	public void setCrewList(LaborCrew[] c, LaborCrew u)
	{
		crewlist = c;
		unmapped = u;
		
		for(Entry<LaborType, UnassignedListElement> e : upool.entrySet())
		{
			e.getValue().quantity = u.getAmt(e.getKey());
			e.getValue().update();
		}
		
		for(LaborCrew l : c)
		{
			if (l!=null)
			{
//				int id=l.getID();
//				System.out.println("id "+id);
//				apool A=apool.get(id);
//				System.out.println("apool "+);
				TreeMap<LaborType, AssignedListElement> li = apool.get(l.getID());
				
				for(Entry<LaborType, AssignedListElement> e : li.entrySet())
				{
					e.getValue().force(l.getAmt(e.getKey()));
				}
			}
		}
		
		updateMaxValues();
	}
	
	
	private LaborCrew getCrewListByID(int i)
	{
		for(int x = 0; x < crewlist.length; x++)
		{
			if(crewlist[x] != null && crewlist[x].getID() == i)
				return crewlist[x];
		}
		
		return null;
	}
	
	//Updates the crewlist with values from the Assigned Labor pool (apool) and returns it 
	public G_LaborCrew[] getCrews()
	{
		for(Entry<Integer, TreeMap<LaborType, AssignedListElement>> e : apool.entrySet())
		{
			LaborCrew nc = getCrewListByID(e.getKey());
			if(nc != null)
			{
				nc.clear();
	
				for(Entry<LaborType, AssignedListElement> e2 : e.getValue().entrySet())
					nc.add(e2.getKey(), e2.getValue().getValue());
			}
		}
		
		G_LaborCrew[] newCrews = new G_LaborCrew[crewlist.length];
		for(int i=0; i<crewlist.length; i++){
			newCrews[i] = new G_LaborCrew(crewlist[i]);
		}
		return newCrews;
	}
	
	//Update the productivity based on the new Labor Allocation
	/*
	protected void updateProductivity()
	{
		LaborCrew[] crews = getCrews();
		for(LaborCrew c : crews)
		{
			for(Activity a : sim.getActivities())
			{
				ResourceAllocation ra = new ResourceAllocation(a);
				for(LaborCrew cr : a.getLaborUse())
				{
					if(cr.getID() == c.getID())
						ra.request(c);
				}
				double percent = sim.computeWorkQuantityMultiplier(ra);
			}
		}
	}*/
}

//List Element for an Unassigned Labor Type
class UnassignedListElement extends JPanel implements ActionListener
{
	private static final long serialVersionUID = 1L;

	public LaborType type;
	public int quantity;
	
	private JLabel namelabel;
	private JLabel quantlabel;
	private JButton hirebutton;
	private JButton firebutton;
	
	private LaborCrewPanel lcp;
	
	public UnassignedListElement(LaborType t, LaborCrewPanel lcp)
	{
		type = t;
		this.lcp = lcp;
		
		setLayout(new GridLayout(1, 4));
		
		namelabel = new JLabel();
		quantlabel = new JLabel();
		hirebutton = new JButton("Hire");
		firebutton = new JButton("Fire");
		
		hirebutton.addActionListener(this);
		firebutton.addActionListener(this);
		
		add(namelabel);
		add(quantlabel);
		add(new PaddedPanel(5, hirebutton));
		add(new PaddedPanel(5, firebutton));
		
		update();
	}
	
	public void update()
	{
		namelabel.setText(type.getDescription());
		quantlabel.setText("" + quantity);
	}
	
	public void actionPerformed(ActionEvent a)
	{
		if(a.getSource() == hirebutton)
		{
			quantity++;
		}
		
		if(a.getSource() == firebutton)
		{
			if(quantity != 0)
				quantity--;
		}
		
		lcp.updateMaxValues();
		
		update();
	}
}

//List Element for an Assigned Labor Type
class AssignedListElement extends JPanel implements ChangeListener
{
	private static final long serialVersionUID = 1L;
	
	public LaborType type;
	public int needquant;
	
	private int curquant;
	private JLabel namelabel;
	private JLabel quantlabel;
	private JSpinner spinner;
	private LaborCrewPanel lcp;
	
	public AssignedListElement(LaborCrewPanel lcp, LaborType t, int q)
	{
		type = t;
		this.lcp = lcp;
		
		needquant = q;
		setLayout(new GridLayout(1, 3));
		
		namelabel = new JLabel();
		quantlabel = new JLabel();
		curquant = q;
		spinner = new JSpinner(new SpinnerNumberModel(q, 0, q, 1));
		spinner.addChangeListener(this);
		
		add(namelabel);
		add(quantlabel);
		add(new PaddedPanel(5, spinner));
		
		update();
	}
	
	public void reset()
	{
		force(needquant);
	}
	
	public void setMax(int i)
	{
		SpinnerNumberModel m = (SpinnerNumberModel)(spinner.getModel());
		m.setMaximum(i);
	}
	
	public void setValue(int i)
	{
		spinner.setValue(i);
	}
	
	public void force(int i)
	{
		setMax(i);
		curquant = i;
		setValue(i);
	}
	
	public int getValue()
	{
		return (Integer)spinner.getValue();
	}
	
	public void update()
	{
		namelabel.setText(type.getDescription());
		quantlabel.setText("" + needquant);
	}

	public void stateChanged(ChangeEvent arg0)
	{
		int rem = getValue() - curquant;
		curquant = getValue();
		
		if(rem != 0)
			lcp.removeFromUnassigned(type, rem);
		//lcp.updateProductivity();
	}
}