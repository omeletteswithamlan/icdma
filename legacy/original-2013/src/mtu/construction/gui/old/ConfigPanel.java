package mtu.construction.gui.old;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import mtu.construction.icdma.Simulator;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import mtu.construction.project.TONAE;

/**
 * 
 * @author kkaaikal
 *
 * Challenge: To get info from this panel to the Simulator in order to connect to db
 * Need to add Load from File.
 */
public class ConfigPanel extends JPanel implements ActionListener {

	//The various variables we can set
	boolean threading; //whether or not to enable query threads
	int threads; //number of query threads to use
	int bucketsize; //bucketsize to use for the query results
	
	boolean fileoutput; //whether or not to output to file
	boolean debug; //whether or not to print debug text
	boolean database; //whether or not to record to the database
	int queryTimeout; //max amount of time allowed for querying
	int futures; //the number of futures to query
	
	/* Welcome */
	//Shows welcome text
	
	/* Debug */ //New idea
	//enable debug text
		//enable query result output
		//enable stock output
		//**enable cost output
		//**enable resource usage output
	
	/* Options */
	//enable threading
	//      //Number of threads
	//enable database writing
	//enable debug text
	//enable file output
	
	/* Query Specific */
	//bucketsize
	//#futures to query
	//query timeout
	
	/* Database */
	//host
	//port
	//database name
	//username
	//password
	//NOTE: read from a config file?
	
	JTextField hostField;
	JTextField port;
	JTextField dbName;
	JTextField projectNo;
	JTextField username;
	JPasswordField password;
	
	JCheckBox threadBox;
	JSpinner numThreads;
	JCheckBox dbrecordBox;
	JCheckBox debugBox;
	JCheckBox fileBox;
	
	JTextField futurField;
	JTextField bucketField;
	JTextField timeoutField;
	
	
	public ConfigPanel(){
		JTabbedPane tabconfig = new JTabbedPane();
		
		JPanel dbPanel = new JPanel();
		JPanel optionPanel = new JPanel();
		JPanel queryPanel = new JPanel();
		
		/*Database Panel*/
		dbPanel.setLayout(new BoxLayout(dbPanel, BoxLayout.X_AXIS));
		JPanel dLeft = new JPanel(); dLeft.setLayout(new BoxLayout(dLeft, BoxLayout.Y_AXIS));
		JPanel dRight = new JPanel(); dRight.setLayout(new BoxLayout(dRight, BoxLayout.Y_AXIS));
		
		JLabel hostLbl = new JLabel("host:");
		JLabel portLbl = new JLabel("port:");
		JLabel dbLbl = new JLabel("database:");
		JLabel pgLbl = new JLabel("project#:");
		JLabel userLbl = new JLabel("username:");
		JLabel passLbl = new JLabel("password:");
		
		hostField = new JTextField("construction.eecn.mtu.edu");
		port = new JTextField("5432");
		dbName = new JTextField("vcdb");
		projectNo = new JTextField("18");
		username = new JTextField("construction");
		password = new JPasswordField();
		
		dLeft.add(hostLbl);
		dLeft.add(portLbl);
		dLeft.add(dbLbl);
		dLeft.add(pgLbl);
		dLeft.add(userLbl);
		dLeft.add(passLbl);
		
		dRight.add(hostField);
		dRight.add(port);
		dRight.add(dbName);
		dRight.add(projectNo);
		dRight.add(username);
		dRight.add(password);
		
		dbPanel.add(dLeft);
		dbPanel.add(dRight);
		
		
		/*Options Panel*/
		optionPanel.setLayout(new BoxLayout(optionPanel, BoxLayout.X_AXIS));
		JPanel oLeft = new JPanel(); oLeft.setLayout(new BoxLayout(oLeft, BoxLayout.Y_AXIS));
		JPanel oRight = new JPanel(); oRight.setLayout(new BoxLayout(oRight, BoxLayout.Y_AXIS));
		
		JLabel threadLbl = new JLabel("Enable Threading?");
		JLabel nthreadLbl = new JLabel("# of threads:");
		JLabel dbrecordLbl = new JLabel("Enable Database Recording?");
		JLabel debugLbl = new JLabel("Enable Debug Text?");
		JLabel fileLbl = new JLabel("Enable Output File?");
		
		threadBox = new JCheckBox(); threadBox.setSelected(false); threadBox.addActionListener(this);
		numThreads = new JSpinner(); numThreads.setValue(10);
		dbrecordBox = new JCheckBox(); dbrecordBox.setSelected(false);
		debugBox = new JCheckBox();
		fileBox = new JCheckBox();
		
		oLeft.add(threadLbl);
		oLeft.add(nthreadLbl);
		oLeft.add(dbrecordLbl);
		oLeft.add(debugLbl);
		oLeft.add(fileLbl);
		
		oRight.add(threadBox);
		oRight.add(numThreads);
		oRight.add(dbrecordBox);
		oRight.add(debugBox);
		oRight.add(fileBox);
		
		optionPanel.add(oLeft);
		optionPanel.add(oRight);
		
		/*Query Panel*/
		queryPanel.setLayout(new BoxLayout(queryPanel, BoxLayout.X_AXIS));
		JPanel qLeft = new JPanel(); qLeft.setLayout(new BoxLayout(qLeft, BoxLayout.Y_AXIS));
		JPanel qRight = new JPanel(); qRight.setLayout(new BoxLayout(qRight, BoxLayout.Y_AXIS));
		
		JLabel futurLbl = new JLabel("# futures to query:");
		JLabel bucketLbl = new JLabel("Query result bucket size:");
		JLabel timeoutLbl = new JLabel("Query Timeout (s):");
		
		futurField = new JTextField("10");
		bucketField = new JTextField("50000");
		timeoutField = new JTextField("100000");
		
		qLeft.add(futurLbl);
		qLeft.add(bucketLbl);
		qLeft.add(timeoutLbl);
		
		qRight.add(futurField);
		qRight.add(bucketField);
		qRight.add(timeoutField);
		
		queryPanel.add(qLeft);
		queryPanel.add(qRight);
		
		tabconfig.addTab("Database", dbPanel);
		tabconfig.addTab("Options", optionPanel);
		tabconfig.addTab("Querying", queryPanel);
		
		add(tabconfig);
	}
	
	//For later use, if we decide to load from a config file...
	public void loadConfig(){
		
	}
	public boolean saveConfig(){
		return false;
	}
	
	//Set the system variables to the values set in the fields
	public void setVariables(){
		//Options
		TONAE.dbrecord = dbrecordBox.isSelected();
		TONAE.fileGant = fileBox.isSelected();
		TONAE.debugText = debugBox.isSelected();
		if(threadBox.isSelected())
			Simulator.queryThreads = (Integer)numThreads.getValue();
		else
			Simulator.queryThreads = 1;
		
		//Query Stuff
		Simulator.numFutures = Integer.parseInt(futurField.getText());
		Simulator.QUERY_FUTURES_QUANTINIZATION = Integer.parseInt(bucketField.getText());
		Simulator.query_timeout = Integer.parseInt(timeoutField.getText());
		System.out.println("NumFutures: "+Simulator.numFutures);
		System.out.println("BucketSize: "+Simulator.QUERY_FUTURES_QUANTINIZATION);
		System.out.println("Query Timeout: "+Simulator.query_timeout/1000+" seconds.");
		System.out.println("Query Threads: "+Simulator.queryThreads);
	}
	
	public String getHost(){
		return hostField.getText();
	}
	
	public String getDatabase(){
		return dbName.getText();
	}
	
	public int getProject(){
		return Integer.parseInt(projectNo.getText());
	}
	
	public String getUsername(){
		return username.getText();
	}
	
	public String getPassword(){
		return new String(password.getPassword());
	}
	
	public int getPort(){
		return Integer.parseInt(port.getText());
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource().equals(threadBox)){
				numThreads.setEnabled(threadBox.isSelected());
		}
	}
	
}
