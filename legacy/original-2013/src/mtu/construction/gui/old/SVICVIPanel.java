package mtu.construction.gui.old;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class SVICVIPanel extends JPanel
{
	private float xp, yp;
	private float svi,cvi;
	
	private final int TICK_HEIGHT = 5;
	
	public SVICVIPanel()
	{
		super();
		setLayout(null);
	}
	
	private synchronized void drawline(Graphics g, float x1, float y1, float x2, float y2)
	{
		g.drawLine(tX(x1), tY(y1), tX(x2), tY(y2));
	}
	
	private synchronized boolean bounded(float f, float base)
	{
		int interval = 65536;
		int cur = 0;
		
		while(interval != 0)
		{
			if(base * cur + .01 > f && base * cur - .01 < f)
				return true;
			interval /= 2;
			if(f < base * cur)
				cur -= interval;
			else
				cur += interval;
		}
		
		return false;
	}
	
	private int tX(float f)
	{
		return (int)((f + xp)/(xp*2)*getWidth());
	}
	
	private int tY(float f)
	{
		return (int)((f + yp)/(yp*2)*getHeight());
	}
	
	private float fX(int x)
	{
		float fv = (float)x;
		float fh = (float)getWidth();
		return (fv / fh) * (xp / 2.0f) - xp;
	}
	
	private float fY(int y)
	{
		float fy = (float)y;
		float fh = (float)getHeight();
		return (fy / fh) * (yp / 2.0f) - yp;
	}
	
	private synchronized void drawCircle(Graphics g, float x, float y, int rad)
	{ 
		g.fillOval(tX(x) - rad / 2, tY(y) - rad / 2, rad, rad);
		//g.drawLine((int)((x1 + xp)/(xp*2)*getWidth()), (int)((y1 + yp)/(yp*2)*getHeight()), (int)((x2 + xp)/(xp*2)*getWidth()), (int)((y2 + yp)/(yp*2)*getHeight()));
	}
	
	public void setSVICVI(float svi, float cvi, int activityid)
	{
		this.svi = svi;
		this.cvi = cvi;
	}

	public synchronized void paintComponent(Graphics g)
	{
		removeAll();
		
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());
		g.setColor(Color.BLACK);
		g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
		
		int my = getHeight() / 2;
		int mx = getWidth() / 2;
		
		g.drawLine(0, my, getWidth(), my);
		g.drawLine(mx, 0, mx, getHeight());
		
		xp = 2.0f;
		yp = 2.0f;
		
		JLabel cviLable=new JLabel("cvi");
		cviLable.setBounds(tX(xp)-20, tY(TICK_HEIGHT * 4 * yp / getHeight()), 50,10);
		add(cviLable);
		JLabel sviLable=new JLabel("svi");
		sviLable.setBounds(tX(TICK_HEIGHT * 4 * xp / getWidth())-50, tY(-yp)+5, 50,10);
		add(sviLable);
		for(float x = -xp; x <= xp; x += .1f)
		{
			
			float vc;
			
			if(bounded(x, .5f))
			{
				vc = TICK_HEIGHT * 4 * yp / getHeight();
				JLabel j = new JLabel(""+ Math.round(x*100)+"%");
				j.setBounds(tX(x) - 20- TICK_HEIGHT, tY(vc)+2, 50, 10);
				add(j);
			}
			else
			{
				vc = TICK_HEIGHT * 2 * yp / getHeight();
			}
			drawline(g, x, vc, x, -vc);
		}
		
		for(float y = -yp; y <= yp; y += .1f)
		{
			float hc;
			
			if(bounded(y, .5f))
			{
				hc = TICK_HEIGHT * 4 * xp / getWidth();
				JLabel j = new JLabel(""+ Math.round((-y)*100)+"%");
				j.setBounds(tX (hc) + 4, tY(y)+TICK_HEIGHT, 50, 10);
				
				add(j);
			}
			else
				hc = TICK_HEIGHT * 2 * xp / getWidth();
			drawline(g, hc, y, -hc, y);
		}
		
		drawCircle(g, svi, -cvi, 10);
	}
}
