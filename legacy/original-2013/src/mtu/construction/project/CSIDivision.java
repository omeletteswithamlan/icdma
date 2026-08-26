package mtu.construction.project;

import java.io.Serializable;

/**
 * Contains information about CSI divisions used in a construction project.
 * 
 * @author  Jessica Anderson
 */
public class CSIDivision implements Serializable
{
	private int csiId;
	private String csiName;
	private String csiDescr;

	/*
	 * Constructor sets csi id, name, and description
	 */
	public CSIDivision(int id, String name, String descr)
	{
		csiId = id;
		csiName = name;
		csiDescr = descr;
	}

	/**
	 * Decides if object passed in is a CSIDivision object and if so,
	 * compares this and object and returns true if id and name are equal,
	 * otherwise false.
	 * 
	 */
	public boolean equals(Object obj)
	{
		if(obj instanceof CSIDivision)
		{
			CSIDivision d = (CSIDivision)obj;
			return csiId == d.csiId && csiName.equals(d.csiName);
		}
		return false;
	}

	public int getCSIId(){
		return csiId;
	}

	public void setCSIId(int id){
		csiId = id;
	}

	public String getCSIName(){
		return csiName;
	}

	public void setCSIName(String name){
		csiName = name;
	}

	public String getCSIDescr(){
		return csiDescr;
	}

	public void setCSIDescr(String descr){
		csiDescr = descr;
	}

	public String toString()
	{
		return "[id: " + csiId + ", name: " + csiName + ", descr: " + csiDescr + "]" ;
	}
}//end of CSIDivision class


