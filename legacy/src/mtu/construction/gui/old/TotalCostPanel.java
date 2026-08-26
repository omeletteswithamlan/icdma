package mtu.construction.gui.old;

import java.awt.BorderLayout;
import java.util.TreeMap;

import javax.swing.JPanel;

/**
 * For the moment, this serves only as a vestigial container for a GraphPanel
 * @author mtwatkin
 *
 */
public class TotalCostPanel extends JPanel
{
	private GraphPanel plot;
	
	public TotalCostPanel(String title)
	{
		setLayout(new BorderLayout());
		plot = new GraphPanel(title, "Time (Weeks)", "Cost ($)");
		add(plot, BorderLayout.CENTER);
	}
	
	public void update(int currentday, TreeMap<Integer, Double> apcost, TreeMap<Integer, Double> abcost)
	{
		plot.update(currentday, apcost, abcost);
	}
}
