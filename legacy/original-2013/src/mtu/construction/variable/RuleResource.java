/* author Corey Tebo
 * 
 */
package mtu.construction.variable;


import java.io.Serializable;

/************************
 * Represents a resource which is attached to a rule. This resource is given to the rule listener, to be used
 * by the front end
 * 
 * @author Matt Watkins
 *
 */
public class RuleResource implements Serializable
{
	private static final long serialVersionUID = -1085874296723568063L;
	private String type;
	private byte[] resource;

	/***********************
	 * Construct a rule resource
	 * @param type			type of resource
	 * @param resource		data for resource
	 */
	public RuleResource(String type, byte[] resource)
	{
		this.type=type;
		this.resource=resource;
	}
	
	/*************
	 * Sets the type
	 * @param type		new type
	 */
	public void setType(String type)
	{
		this.type=type;
	}
	
	/******************
	 * Sets the resource data
	 * @param resource		new data
	 */
	public void setResource(byte[] resource)
	{
		this.resource=resource;
	}
	
	/********************
	 * Gets the data type
	 * @return		type
	 */
	public String getType()
	{
		return type;
	}
	
	/***************
	 * Gets the data
	 * @return		data
	 */
	public byte[] getResource()
	{
		return resource;
	}
}
