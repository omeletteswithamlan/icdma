package mtu.construction.gui.old;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JPanel;



public class LegendPanel extends JPanel
{
	private Vector<LegendEntry> entries;
	private int prefx;
	private int prefy;
	
	public LegendPanel()
	{
		super();
		
		setLayout(new BorderLayout());
		
		entries = new Vector<LegendEntry>();
		
		setOpaque(true);
	}
	
	public void addEntry(String s, Color c)
	{
		LegendEntry en = new LegendEntry(s, c);
		entries.add(en);
		
		int xp = 5;
		int yp = 5;
		int padding = 7;
		
		removeAll();
		
		JLabel l = new JLabel("Legend:");
		l.setSize(l.getPreferredSize());
		
		add(l);
		l.setBounds(xp, yp, l.getWidth(), l.getHeight());
		
		yp += l.getHeight() + padding;
		
		prefx = xp + l.getWidth();
		
		for(LegendEntry e : entries)
		{
			
			JLabel lab = new JLabel(e.name);
			
			lab.setSize(lab.getPreferredSize());
			add(lab);

			lab.setBounds(xp + lab.getHeight() + padding, yp, lab.getWidth(), lab.getHeight());
			
			yp += lab.getHeight() + padding;
			
			if(xp + lab.getHeight() + padding + lab.getWidth() > prefx)
				prefx = xp + lab.getHeight() + padding + lab.getWidth();

		}
		
		prefx += 3;
		prefy = yp;
		
		//I don't know why this sillyness is needed.
		add(new JLabel(""));
	}
	
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		
		int xp = 5;
		int yp = 5;
		int padding = 7;
		
		g.setColor(Color.BLACK);
		g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
		
		
		JLabel l = new JLabel("Legend:");
		l.setSize(l.getPreferredSize());
		
		yp += l.getHeight() + padding;
		
		for(LegendEntry e : entries)
		{
			
			JLabel lab = new JLabel(e.name);
			
			lab.setSize(lab.getPreferredSize());

			g.setColor(e.color);
			g.fillRect(xp, yp, lab.getHeight(), lab.getHeight());
			g.setColor(Color.BLACK);
			g.drawRect(xp, yp, lab.getHeight(), lab.getHeight());
			
			yp += lab.getHeight() + padding;
			
		}
	}
	
	public Dimension getPreferredSize()
	{
		return new Dimension(prefx, prefy);
	}
}

class LegendEntry extends JPanel
{
	protected String name;
	protected Color color;
	
	public LegendEntry(String s, Color c)
	{
		setLayout(null);
		
		name = s;
		color = c;
	}
}
