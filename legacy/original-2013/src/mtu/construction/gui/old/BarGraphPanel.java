package mtu.construction.gui.old;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JPanel;

public abstract class BarGraphPanel extends JPanel
{
	private static final long serialVersionUID = -7340201728741678981L;
	int leftpad = 10;
	int toppad = 10;
	int barpad = 10;
	int rightpad = 30;
	int bottompad = 50;
	int heightstep;

	public BarGraphPanel()
	{
		super();
		
		setLayout(null);
	}
	
	protected abstract int getBarCount();
	protected abstract int getHeight(int i);
	protected abstract String getBaseText(int i);
	
	protected void drawRect(int left, int top, int right, int bottom, Color c, Graphics g)
	{
		g.setColor(c);
		g.fillRect(left, top, right - left, bottom - top);

		g.setColor(Color.BLACK);
		g.drawRect(left, top, right - left, bottom - top);
	}
	
	private int findmax(int bars)
	{
		int max = 0;
		for(int x = 0; x < bars; x++)
			max = Math.max(max, getHeight(x));
		
		heightstep = 1;
		
		while(max > heightstep * 10)
			heightstep *= 10;
		
		max = ((max / heightstep) + 1) * heightstep;

		return max;
	}
	
	public void paintComponent(Graphics g)
	{
		removeAll();
		
		int top = toppad;
		
		int bars = getBarCount();
		int max = findmax(bars);
		
		int textwidth = 0;
		Vector<JLabel> sidelabels = new Vector<JLabel>();
		
		if(max / heightstep < 6 && heightstep != 1)
			heightstep /= 2;
		if(max / heightstep < 6 && heightstep != 1)
			heightstep /= 2;
		
		for(int x = 0; x <= max; x += heightstep)
		{
			JLabel j = new JLabel("" + x);
			j.setSize(j.getPreferredSize());
			textwidth = Math.max(textwidth, j.getWidth());
			sidelabels.add(j);
		}
		
		int left = textwidth + leftpad;
		
		int right = getWidth() - rightpad;
		int bottom = getHeight() - bottompad;

		drawRect(left, top, right, bottom, Color.WHITE, g);
		
		g.setColor(Color.BLACK);
		for(int x = 0; x <= max; x += heightstep)
		{
			float perc = (float)x / (float)max;
			
			int height = (int)((float)(bottom - top) * (1 - perc)) + top;
			g.drawLine(left, height, right, height);
			JLabel j = sidelabels.get(x/heightstep);
			j.setBounds(textwidth - j.getWidth(), height - j.getHeight() / 2, j.getWidth(), j.getHeight());
			add(j);
		}
		
		int step = (right - left) / bars;
		int width = step - barpad;
		int ptr = left + barpad - 2;
		
//		boolean b = true;
		for(int x = 0; x < bars; x++)
		{
			float perc = (float)getHeight(x) / (float)max;
			
			drawRect(ptr, (int)(top + (float)(bottom - top) * (1 - perc)), ptr + width, bottom, Color.RED, g);
			JLabel l = new JLabel(getBaseText(x));
			l.setSize(l.getPreferredSize());

			l.setBounds(ptr + (width - l.getWidth()) / 2 ,( bottom + 10*(x% (int)Math.ceil(Math.sqrt(bars)))), l.getWidth(), l.getHeight());
			add(l);
			ptr += step;
		}
	}
}
