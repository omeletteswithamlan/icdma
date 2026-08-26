package mtu.construction.gui.old;

import mtu.construction.icdma.Simulator;

import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import mtu.construction.tonae.QueryResult;
import mtu.construction.tonae.QueryResult2;
import mtu.construction.listener.QueryResultListener;


/**
 * Modify queryFutures in the update method 100, 1000 for more accurate projection
 * TODO: setup queryFutures
 */
public class CostPanel extends JPanel implements QueryResultListener
{
	private GraphPanel direct;
	private GraphPanel indirect;
	private TotalCostPanel totalcostpanel;
	private ProjectedCost projectedcost;
	private CostDistributionPanel costdistributionpanel;
	private Simulator sim;
	private QueryResult2 results;

	public CostPanel(Simulator sim)
	{
		this.sim = sim;
		totalcostpanel = new TotalCostPanel("Total Cost");
		projectedcost = new ProjectedCost("Projected Cost", "Time (Weeks)", "Cost ($)");
		costdistributionpanel = new CostDistributionPanel("Projected Cost Distribution", "Cost ($)", "Probability");
		direct = new GraphPanel("Direct", "Time (Weeks)", "Cost ($)");
		indirect = new GraphPanel("Indirect", "Time (Weeks)", "Cost ($)");
		
		JTabbedPane tabbedpane = new JTabbedPane();
		tabbedpane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
		tabbedpane.setTabPlacement(JTabbedPane.LEFT);
		tabbedpane.addTab("Total", totalcostpanel);
		tabbedpane.addTab("Direct", direct);
		tabbedpane.addTab("Indirect", indirect);
		tabbedpane.addTab("Projected", projectedcost);
		tabbedpane.addTab("Distribution", costdistributionpanel);

		setLayout(new GridLayout(1, 1));
		add(tabbedpane);
		
		//Register as listener in TONAE
		registerListeners();
		
		//needed for initial cost projection
		results = sim.queryFutures(sim.numFutures, sim.QUERY_FUTURES_QUANTINIZATION);
	}
	
	public void registerListeners(){
		sim.registerQueryResultListener(this);
	}
	
	public void unregisterListeners(){
		sim.unregisterQueryResultListener(this);
	}
	
	public void update(int currentday)
	{
		//Moved queryFutures to TONAE, made this class a QueryResultListener
		//results = tonae.queryFutures(100, QUERY_FUTURES_QUANTINIZATION);

		direct.update(currentday, sim.getAsPlannedDirect(), sim.getAsBuiltDirect());
		indirect.update(currentday, sim.getAsPlannedIndirect(), sim.getAsBuiltIndirect());
		totalcostpanel.update(sim.getCurrentTimeStep(), sim.getAsPlannedTotal(), sim.getAsBuiltTotal());
		if(results != null){
			projectedcost.update(sim.getCurrentTimeStep(), sim.getAsPlannedTotal(), sim.getAsBuiltTotal(), results);
			costdistributionpanel.update(results);
		}
	}
	
	public QueryResult2 getQueryResults()
	{
		return results;
	}

	@Override
	public void onQueryFinished(QueryResult2 r) {
		results = r;
	}
}
