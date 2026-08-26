package mtu.construction.gui.old;

import mtu.construction.gui.InitListener;
import mtu.construction.gui.wrapper.G_LaborCrew;
import mtu.construction.gui.wrapper.G_ResourceAlloc;
import mtu.construction.icdma.Simulator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import mtu.construction.dialog.IntroDialog;
import mtu.construction.dialog.LaborAllocationDialog;
import mtu.construction.dialog.SpaceViolationDialog;
import mtu.construction.dialog.WarningDialog;

import mtu.construction.listener.G_SpaceViolationListener;
import mtu.construction.listener.LaborAlteredListener;
import mtu.construction.listener.LaborChangeListener;
import mtu.construction.listener.RuleListener;
import mtu.construction.listener.SpaceViolationListener;

import mtu.construction.project.Activity;
import mtu.construction.project.LaborCrew;
import mtu.construction.project.MaterialType;
import mtu.construction.project.TONAE;

import mtu.construction.tonae.QueryResult;
import mtu.construction.tonae.QueryResult2;
import mtu.construction.tonae.ResourceAllocation;
import mtu.construction.variable.Rule;

/**
 * @author catebo
 *
 */
public class MainWindow extends JFrame implements ActionListener, RuleListener, G_SpaceViolationListener, LaborChangeListener, LaborAlteredListener, WindowListener, InitListener
{
	private static final long serialVersionUID = -6506150572291820144L;
	public static final int AS_PLANNED_CHART = 1;
	public static final int AS_BUILT_CHART = 2;
	public static final int AS_PROJECTED_CHART = 4;
	public static final int HARD_CONSTRAINTS = 7;
	public static final int SOFT_CONSTRAINTS = 8;
	
	private SchedulePanel schedulepanel;
	private CostPanel costpanel;
	private ActivityPanel activitypanel;
	private ResourcePanel resourcepanel;
	private LaborCrewPanel laborcrewpanel;
	//private SalesPanel salespanel; // IS NO LONGER USED
	private CenterInfoPanel infobar;
	
	private JMenuItem save;
	private JMenuItem load;
	private JMenuItem exit;
	
	private Simulator sim;
	

	/**
	 * this function locks the gantchart from updating at inappropriate times
	 */
	public void lock()
	{
		schedulepanel.lock();
		infobar.enableSimButton(false);
	}
	
	public void unlock()
	{
		schedulepanel.unlock();
		//update this GUI
		this.update();
		this.resetLaborCrews();
		if(!sim.isFinished())
			infobar.enableSimButton(true);
		else {
			System.out.println("You Have Completed the Simulation!!! Your score is 99! Try Again?");
			WarningDialog.show("Congratulations!", "Congratulations! You have completed the simulation. Now go have a cup of coffee or something. Or you could check out your history (historyid " + (sim.getHistoryID() > 0 ? sim.getHistoryID() : "N/A") +"). Have a nice day!", null, false, this);
			//System.exit(0);
		}
			
		//Repaint components
		//System.out.println("### Update GUI ###");
		this.repaint();
	}
	
	//Entry point for the program. Begin the simulation.
	public static void main(String[] args){
		new MainWindow();
		//IntroDialog.showdialog();
		//Simulator sim = new Simulator();
		//sim.run();
		//MainWindow win = new MainWindow(sim);
		//win.setVisible(true);
	}
	
	//Create the GUI
	public MainWindow(Simulator sim)
	{
		this.sim=sim;
		initWindow();
	}
	
	//Creates the GUI, which initiates the introDialog.
	//Once the dialog is finished, a simulator is created, and
	//the mainwindow is initiated (onInit).
	public MainWindow(){
		new IntroDialog(this).setVisible(true);
	}
	
	//Register the listeners
	private void registerListeners(){
		sim.registerRuleListener(this);
		sim.registerSpaceViolationListener(this);
		sim.registerLaborChangeListener(this);
		sim.registerLaborAlteredListener(this);
	}
	
	private void unregisterListeners(){
		sim.unregisterLaborAlteredListener(this);
		sim.unregisterLaborChangeListener(this);
		sim.unregisterSpaceViolationListener(this);
		sim.unregisterRuleListener(this);
		costpanel.unregisterListeners();
	}
	
	public void update()
	{
		schedulepanel.update();
		costpanel.update(sim.getCurrentTimeStep());
		activitypanel.update();
		resourcepanel.update();
		//salespanel.update();// IS NOLONGER USED
		laborcrewpanel.update();
		infobar.update();
		
		//Start the Turn timer
		sim.signalTimerStart();
	} 
	
	public QueryResult2 getQueryResults()
	{
		return costpanel.getQueryResults();
	}
	
	public G_LaborCrew[] getRequestedCrews()
	{
		return laborcrewpanel.getCrews();
	}
	
	public LaborCrew getUnmapped()
	{
		return laborcrewpanel.getUnmapped();
	}
	
	public LaborCrew getHired()
	{
		return laborcrewpanel.getHired();
	}
	
	public void resetLaborCrews()
	{
		laborcrewpanel.resetLaborCrewAssignment();
	}
	
	public Vector<G_ResourceAlloc> getResourceAllocation()
	{
		Vector<G_ResourceAlloc> alloc = resourcepanel.getResourceAllocation(false);
		
		return alloc;
	}
	
// IS NO LONGER USED
//	public HashMap<MaterialType, Integer> getSoldMaterial()
//	{
//		return salespanel.getSoldMaterial();
//	}
	
	public void actionPerformed(ActionEvent e)
	{
		//Save the state of the simulation
		if(e.getSource() == save)
		{
			JFileChooser savedialog = new JFileChooser();
			savedialog.showSaveDialog(this);
			if(savedialog.getSelectedFile() != null)
//				tonae.save(savedialog.getSelectedFile());
//				sim.save(location, name)
				
				//Un-Register listeners so that we don't have to save the GUI.
				unregisterListeners();
				sim.save(savedialog.getSelectedFile());
//				;
		}
		//Load a saved state of the simulation
		else if(e.getSource() == load)
		{
			JFileChooser loaddialog = new JFileChooser();
			loaddialog.showOpenDialog(this);
//			try
//			{
				if(loaddialog.getSelectedFile() != null)
				{
//					tonae = TONAE.load(loaddialog.getSelectedFile());
//					sim.load(location);
					sim.load(loaddialog.getSelectedFile());
					initWindow();
				}
//			}
//			catch (IOException e1){}
		}
		//Exit the simulation
		else if(e.getSource() == exit)
		{
			quit();
		}
		else //Sim Step button pressed
		{
			long oldSimPressed = simPressed;
			simPressed = System.currentTimeMillis();
			if(TONAE.paperGant){
				System.out.println("\n(Week "+sim.getCurrentTimeStep()+") Turn took " + (simPressed - oldSimPressed)/1000 + " seconds.");
			}
			sim.setHired(getHired());
			sim.setUnmapped(getUnmapped());
			sim.setCrewRequest(getRequestedCrews());
			//sim.setSoldMaterial(this.getSoldMaterial()); //IS NO LONGER USED
			sim.setgResourceRequest(getResourceAllocation());

			this.lock();
			ActionThread thread = new ActionThread(sim, this);
			thread.start();
			
		}
	}

	private void initWindow()
	{
		getContentPane().removeAll();
		JPanel mainpanel = new JPanel(new BorderLayout());
		mainpanel.setBackground(new Color(192, 192, 255));
		
		schedulepanel = new SchedulePanel(sim);
		costpanel = new CostPanel(sim);
		activitypanel = new ActivityPanel(sim);
		
		laborcrewpanel = new LaborCrewPanel(sim);
		resourcepanel = new ResourcePanel(sim, laborcrewpanel);
//		salespanel = new SalesPanel(sim); //IS NO LONGER USED
		infobar = new CenterInfoPanel(this, sim);
		
		mainpanel.add(infobar, BorderLayout.NORTH);
		
		JTabbedPane tabbedpane = new JTabbedPane();
		tabbedpane.setOpaque(false);
		tabbedpane.addTab("Schedule", schedulepanel);
		tabbedpane.addTab("Cost", costpanel);
		tabbedpane.addTab("All Activities", activitypanel);
		tabbedpane.addTab("Resources", resourcepanel);
		tabbedpane.addTab("Labor Crews", laborcrewpanel);
		//tabbedpane.addTab("Stock", salespanel); //IS NO LONGER USED
		
		mainpanel.add(new PaddedPanel(30, tabbedpane), BorderLayout.CENTER);

		setBounds(0, 0, 800, 800);
		
		update();
		getContentPane().add(mainpanel);
//		sim.setListeners();
		setBounds(getX(), getY(), getWidth(), getHeight()+1);
		
		JMenuBar menubar = new JMenuBar();
		JMenu filemenu = new JMenu("File");
		menubar.add(filemenu);
		
		save = new JMenuItem("Save");
		filemenu.add(save);
		save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		save.addActionListener(this);

		
		load = new JMenuItem("Open");
		filemenu.add(load);
		load.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		load.addActionListener(this);
		
		filemenu.addSeparator();
		
		exit = new JMenuItem("Exit");
		filemenu.add(exit);
		exit.addActionListener(this);
		
		this.addWindowListener(this);
		
		this.setJMenuBar(menubar);
		
		//Register the listeners here!
		registerListeners();
		
		//Set begin timer
		simPressed = System.currentTimeMillis();
	}

	@Override
	public void ruleTriggered(Rule r, Activity a, Object o) {
		String s = "";
		if(o != null)
		{
			if(o instanceof MissingLaborContainer)
			{
				MissingLaborContainer m = (MissingLaborContainer)o;
				s = "A " + m.getType().getDescription() + " called in sick from labor crew '" + m.getCrew().getName() + "'";
//				System.out.println(s);
			}
		}

		if(s.equals("") && r.getWarning() != null)
			WarningDialog.show("Warning", r.getWarning(), "/image/warning.jpg", true, this);
		else if(!s.equals(""))
			WarningDialog.show("Warning", s, "/image/warning.jpg", true, this);

//		for(Media m : r.getMedia())
//		{
			//System.out.println(new String((byte[])m.getObject()));
//		}
	}

	@Override
	public void spaceViolation(double spaceOccupied, double spaceAllowed,
			double deliveryspace, HashMap<MaterialType, Integer> delivery, Vector<G_ResourceAlloc> request) {
		SpaceViolationDialog.show("Space Violation", spaceOccupied, spaceAllowed, deliveryspace, delivery, sim.getOverstockPenalty(), request); 
		
	}

	@Override
	public void laborChanged(LaborCrew[] c, LaborCrew u, LaborCrew h) {
		LaborAllocationDialog.show(sim, c, u, h);	
	}

	@Override
	public void laborAltered() {
		// TODO Auto-generated method stub
		
	}
	
	public void simulate(){
		simPressed = System.currentTimeMillis();
		System.out.println("Hello!");

		//End Timer...
		sim.signalTimerEnd();
		
		//Send Requested Crews, Get Hired, Get ResourceAllocation, Unmapped
		sim.setHired(getHired());
		sim.setUnmapped(getUnmapped());
		sim.setCrewRequest(getRequestedCrews());
		//sim.setSoldMaterial(this.getSoldMaterial()); //Not used...
		sim.setgResourceRequest(getResourceAllocation());
		
		//TODO: Do update threading stuff...
		if(sim.isFinished())
		{
			new WarningDialog(this, "Finished", "This simulaton has reached it's completion. You may save it as is or close this window to exit.",null, true, true);
			System.out.println("The simulation has completed successfully");
			System.exit(0);
		}
		
		//Run a step of the simulation
			//Show "Simulating"
			WarningDialog warn = new WarningDialog(this, "Simulating", "Updating Simulation\n\n " +
					//"Events may occure that require your intervention\n " +
					"you may move this, or any other window to look at\n " +
					"information available to you in the tabs of the main window", "/image/time.jpg", false, false);
		sim.update();
			//Remove "Simulating"
			warn.dispose();
		
		//update this GUI
		this.update();
		this.resetLaborCrews();
		
		//Repaint components
		this.repaint();
		
		//Restart Timer
		sim.signalTimerStart();
	}
	
	public void quit(){
		System.out.println("GoodBye!");
		sim.quit();
		System.exit(0);
	}
	public void windowActivated(WindowEvent e) {}
	public void windowClosed(WindowEvent e) {
		
	}
	public void windowClosing(WindowEvent e) {
		quit();
	}
	public void windowDeactivated(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {}
	
	public void setSim(Simulator sim){
		this.sim = sim;
	}
	
	//////////////////////////////////////////////////
	// Sim Button Pressed Timer
	//////////////////////////////////////////////////
	private static long simPressed = 0;
	public static long lastSimPressed(){
		return simPressed;
	}

	@Override
	public void onInit(String host, int port, String database, int projectNo, String username,
			String password) {
		sim = new Simulator(host, port, database, projectNo, username, password);
		initWindow();
		setVisible(true);
	}
	
}

class UpdateThread extends Thread
{
/*	public MainWindow win;

	public UpdateThread(MainWindow w)
	{
		this.win = w;
	}
*/
	private MainWindow win;
	private Simulator sim;
	public UpdateThread(Simulator sim, MainWindow win){
		this.sim = sim;
		this.win = win;
	}
	public void run()
	{
		WarningDialog udDialog = new WarningDialog(win, "Simulating", "Updating Simulation\n\n " +
				//"Events may occure that require your intervention\n " +
				"you may move this, or any other window to look at\n " +
				"information available to you in the tabs of the main window", "/image/time.jpg", false, false);
//		udDialog.setVisible(true);
		sim.update();
		//one of our threding errors happns between thees two lines
//		win.lock();

//		win.simulate();
		
//		win.unlock();
		
		//udDialog.setVisible(false);
		//udDialog=null;
		udDialog.dispose();
	}
}
// enable browsing of information in the diffrent tabs of the gui while the simulator updates
class ActionThread extends Thread
{
	private Simulator sim;
	private MainWindow win;
	
	public ActionThread(Simulator sim, MainWindow win)
	{
		this.sim = sim;
		this.win = win;
	}

	public void run()
	{
		
		//sim.update();
		
		//UpdateThread up = new UpdateThread(win);
		UpdateThread up = new UpdateThread(sim, win);
		up.start();

		try {
			up.join();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		win.unlock();
	}
}