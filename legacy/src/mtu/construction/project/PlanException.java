package mtu.construction.project;

import java.io.Serializable;

/**
 * PlanException is thrown when the plan fails to be created.
 * 
 * @author mtwatkin
 */
public class PlanException extends Exception implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	private String reason;
	
	/**
	 * Constructs a PlanException which indicates why a plan
	 * failed to be created
	 * 
	 * @param reason   The reason for the failure in the plan
	 */
	public PlanException(String rsn, Exception cause)
	{
		super(rsn, cause);
		reason = rsn;
	}
	
	public void printStackTrace()
	{
		super.printStackTrace();
		System.out.println(reason);
	}
}
