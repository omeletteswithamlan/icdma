package mtu.construction.gui.old;

import java.awt.Color;
import java.awt.GridLayout;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class PicturePanel extends JPanel
{
	private JLabel[] pictures;
	
	public PicturePanel(String[] picture)
	{
		setLayout(new GridLayout(1, 1));
		setBackground(Color.BLACK);
		
		pictures = new JLabel[picture.length];
		for(int x = 0; x < picture.length; x++)
		{
			ImageIcon icon = new ImageIcon(picture[x]);
			pictures[x] = new JLabel();
			pictures[x].setHorizontalAlignment(SwingConstants.CENTER);
			pictures[x].setIcon(icon);
		}
		
		setPicture(0);
	}
	public PicturePanel(URL[] picture){
		setLayout(new GridLayout(1, 1));
		setBackground(Color.BLACK);
		
		pictures = new JLabel[picture.length];
		for(int x = 0; x < picture.length; x++)
		{
			if(picture[x]==null)
				;//System.out.println("Bad.");
			else{
				ImageIcon icon = new ImageIcon(picture[x]);
				pictures[x] = new JLabel();
				pictures[x].setHorizontalAlignment(SwingConstants.CENTER);
				pictures[x].setIcon(icon);
			}
		}
		
		setPicture(0);
	}
	
	public void setPicture(int i)
	{
		if(pictures.length != 0)
		{
			if(i >= pictures.length)
				i = pictures.length - 1;
			if(i < 0)
				i = 0;
			
			removeAll();
			if(pictures[i] != null)
				add(pictures[i]);
			
			repaint();
		}
	}
}
