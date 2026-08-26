package mtu.construction.gui.old;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class PaddedPanel extends JPanel
{
	private JPanel left, right, top, bottom;
	
	public PaddedPanel()
	{
		this(0, null);
	}
	
	public PaddedPanel(int i)
	{
		this(i, null);
	}
	
	public PaddedPanel(int i, JComponent c)
	{
		super();
		
		setLayout(new BorderLayout());
		left = new JPanel();
		add(left, BorderLayout.WEST);
		right = new JPanel();
		add(right, BorderLayout.EAST);
		top = new JPanel();
		add(top, BorderLayout.NORTH);
		bottom = new JPanel();
		add(bottom, BorderLayout.SOUTH);
		
		add(c);
		
		setPadding(i);
		
		setOpaque(false);
	}
	
	public void setOpaque(boolean b)
	{
		super.setOpaque(b);
		
		if(left != null)
		{
			left.setOpaque(b);
			right.setOpaque(b);
			top.setOpaque(b);
			bottom.setOpaque(b);
		}
	}
	
	public void setBackground(Color c)
	{
		super.setBackground(c);
		if(left != null)
		{
			left.setBackground(c);
			right.setBackground(c);
			top.setBackground(c);
			bottom.setBackground(c);
		}
	}
	
	public void add(JComponent c)
	{
		if(c != null)
			super.add(c, BorderLayout.CENTER);
	}
	
	public void setPadding(int i)
	{
		setBottomPadding(i);
		setTopPadding(i);
		setLeftPadding(i);
		setRightPadding(i);
	}
	
	public void setBottomPadding(int i)
	{
		setPadding(i, bottom);
	}
	
	public void setTopPadding(int i)
	{
		setPadding(i, top);
	}
	
	public void setLeftPadding(int i)
	{
		setPadding(i, left);
	}
	
	public void setRightPadding(int i)
	{
		setPadding(i, right);
	}
	
	private void setPadding(int i, JPanel p)
	{
		p.setPreferredSize(new Dimension(i, i));
	}
}
