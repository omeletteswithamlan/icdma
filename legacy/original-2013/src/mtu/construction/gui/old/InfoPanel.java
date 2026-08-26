package mtu.construction.gui.old;

import mtu.construction.gui.wrapper.G_Activity;
import mtu.construction.gui.wrapper.G_LaborCrew;
import mtu.construction.gui.wrapper.G_Material;
import mtu.construction.icdma.Simulator;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URL;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import mtu.construction.project.LaborType;
//import ptolemy.plot.Plot;
//import ptolemy.plot.PlotPoint;

public class InfoPanel extends JPanel implements ItemListener
{
	protected Simulator sim;
	
	protected JComboBox matcombo;
	protected JComboBox labcombo;
	
	//points to the current text location on the info panel
	protected int pointer;
	protected static final int pointerinc = 25;
	
	protected JPanel infopanel;
	protected JPanel mainpanel;
	
	protected JTabbedPane plotTabs;
	
	protected SVICVIPanel svicvi;
	
	protected GraphPanel completionPanel;
	protected GraphPanel costPanel;
	
	protected String leftTitle;
	protected String rightTitle;
	
	protected PicturePanel imgpanel;
	
	private G_Activity activeNode;
	
	private JLabel materialCost;
	private JLabel materialNeeded;
	private JLabel laborCost;
	private JLabel laborNeeded;
	
	public InfoPanel(G_Activity node, String plot1title, String plot1xaxis, String plot1yaxis, String plot2title, String plot2xaxis, String plot2yaxis, String picture, Simulator sim)
	{
		this(node, plot1title, plot1xaxis, plot1yaxis, plot2title, plot2xaxis, plot2yaxis, new String[]{picture}, sim);
	}
	
	public InfoPanel(G_Activity node, String plot1title, String plot1xaxis, String plot1yaxis, String plot2title, String plot2xaxis, String plot2yaxis, String[] picture, Simulator sim)
	{
		this.sim = sim;
		activeNode = node;
		
		leftTitle = plot1title;
		rightTitle = plot2title;
		
		setLayout(new BorderLayout());
		//begin center
		
		JPanel mainpanel = new JPanel();
		mainpanel.setLayout(new GridLayout(1, 2));
		add(mainpanel);
		
		infopanel = new JPanel();
		infopanel.setLayout(null);
		mainpanel.add(infopanel);
		
		completionPanel = new GraphPanel("Activity Completion", "Time", "Completion (percent)", true);
		costPanel = new GraphPanel("Activity Cost", "Time", "Cost", true);
		
		svicvi = new SVICVIPanel();
		
		//Create URL's from the string paths
		URL[] pictures = new URL[picture.length];
		for(int i=0; i<picture.length; i++){
			pictures[i] = this.getClass().getResource(picture[i]);
		}
		//imgpanel = new PicturePanel(picture);
		imgpanel = new PicturePanel(pictures);
		mainpanel.add(imgpanel);
		
		JPanel resourcePlot = new JPanel(new BorderLayout());
		ResourceBarGraph g = new ResourceBarGraph();
		for(Entry<G_Material, Integer> e : activeNode.getAsPlannedMaterialUse().entrySet())
			g.addMaterial(e.getKey(), e.getValue());
		
		resourcePlot.add(g, BorderLayout.CENTER);
		resourcePlot.add(new JLabel("Material Use Summary:"), BorderLayout.NORTH);
		resourcePlot.setPreferredSize(new Dimension(300, 300));
		
		JPanel laborPlot = new JPanel(new BorderLayout());
		ResourceBarGraph l = new ResourceBarGraph();
		HashMap<LaborType, Integer> lab_use = new HashMap<LaborType, Integer>();
		for(G_LaborCrew c : activeNode.getAsPlannedLaborUse())
		{
			for(LaborType t : c.getLaborerTypes())
			{
				int i = 0;
				if(lab_use.containsKey(t))
					i = lab_use.get(t);
				i += c.getAmount(t);
				lab_use.put(t, i);
			}
		}
		for(Entry<LaborType, Integer> e : lab_use.entrySet())
			l.addMaterial(e.getKey(), e.getValue());
		
		laborPlot.add(l, BorderLayout.CENTER);
		laborPlot.add(new JLabel("Labor Use Summary:"), BorderLayout.NORTH);
		laborPlot.setPreferredSize(new Dimension (300,100));
		plotTabs=new JTabbedPane();
		plotTabs.add(resourcePlot, "Resources");
		plotTabs.add(laborPlot, "Labor");
		//plotTabs.add(svicvi, "SVI/CVI");
		plotTabs.add(completionPanel, "Progress");
		plotTabs.add(costPanel, "Cost");
		add(plotTabs, BorderLayout.SOUTH);
		
		matcombo = new JComboBox();
		matcombo.addItemListener(this);
		
		labcombo = new JComboBox();
		labcombo.addItemListener(this);
	}
	
	public void update()
	{
		pointer = 0;
		infopanel.removeAll();
		svicvi.setSVICVI((float)activeNode.getSVI(), (float)activeNode.getCVI(), activeNode.getID());

		//this is the place that 
		completionPanel.update(sim.getCurrentTimeStep(), activeNode.getAsPlannedProgress(), activeNode.getAsBuiltProgress());
		costPanel.update(sim.getCurrentTimeStep(), activeNode.getAsPlannedTotal(), activeNode.getAsBuiltTotal());
		
		infopanel.add(matcombo);
		infopanel.add(labcombo);
		
		imgpanel.setPicture(0);
		
		addText("Cost Summary:");
		addText("Total Cost: $" + round(activeNode.getTotal(), 100));
		addText("Material Cost: $" + round(activeNode.getTotalMaterial(), 100));
		addText("Labor Cost: $" + round(activeNode.getTotalLabor(), 100));
		addText("CSI Division: " + activeNode.getCSIDivision().getCSIName());
		
		String laborcrewlist = "";
		
		for(G_LaborCrew c : activeNode.getAsPlannedLaborUse())
		{
			if(laborcrewlist.length() == 0)
				laborcrewlist += "" + c.getLabel();
			else
				laborcrewlist += ", "+ c.getLabel();
		}
		addText("Labor Crews: " + laborcrewlist);
		pointer += 20;
		
		
		this.repaint();
	}
	
	protected JLabel addText(String s)
	{
		JLabel l = new JLabel(s);
		l.setBounds(5, pointer, 1000, pointerinc);
		infopanel.add(l);
		pointer += pointerinc;
		return l;
	}
	
	protected double round(double d, double p)
	{
		int i = (int)((d * p) + .5);
		
		double n = (double)i;
		n /= p;
		
		return n;
	}
	
	private void updateMatInfo()
	{
		MaterialContainer c = (MaterialContainer)matcombo.getSelectedItem();
		if(c != null && materialCost != null)
		{
			materialCost.setText("Unit Cost: " + c.getType().getCost());
			materialNeeded.setText("Units Needed: " + activeNode.getAsPlannedMaterialUse().get(c.getType()));
		}
	}
	
	private void updateLabInfo()
	{
		LaborContainer c = (LaborContainer)labcombo.getSelectedItem();
		
		if(c != null && laborCost != null)
		{
			HashMap<LaborType, Integer> lab_use = new HashMap<LaborType, Integer>();
			for(G_LaborCrew cr : activeNode.getAsPlannedLaborUse())
			{
				for(LaborType t : cr.getLaborerTypes())
				{
					int i = 0;
					if(lab_use.containsKey(t))
						i = lab_use.get(t);
					i += cr.getAmount(t);
					lab_use.put(t, i);
				}
			}

			int unitnum = 0;
			if(lab_use.containsKey(c.getType()))
				unitnum = lab_use.get(c.getType());
			laborCost.setText("Unit Cost: " + c.getType().getCost());
			laborNeeded.setText("Units Needed: " + unitnum);
		}
	}

	public void itemStateChanged(ItemEvent e)
	{
		if(e.getSource() == matcombo)
			updateMatInfo();
		else if(e.getSource() == labcombo)
			updateLabInfo();
	}
}
