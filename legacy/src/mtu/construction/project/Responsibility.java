package mtu.construction.project;

import java.io.Serializable;

/**
 * Contains information about Responsibilities in a construction project.
 * 
 * @author  Jessica Anderson
 */
public class Responsibility implements Serializable
{
	private int responsibilityId;
	private String responsibilityName;

	/*
	 * Constructor sets csi id, name, and description
	 */
	public Responsibility(int id, String name)
	{
		responsibilityId = id;
		responsibilityName = name;

	}

	/*Decides if object passed in is a Responsibility object and if so,
	 * compares this and object and returns true if id and name are equal,
	 * otherwise false.
	 * 
	 */
	public boolean equals(Object obj)
	{
		if(obj instanceof Responsibility)
		{
			Responsibility r = (Responsibility)obj;
			return responsibilityId == r.responsibilityId && responsibilityName.equals(r.responsibilityName);
		}
		return false;
	}

	public int getResponsibilityId(){
		return responsibilityId;
	}

	public void setResponsibilityId(int id){
		responsibilityId = id;
	}

	public String getResponsibilityName(){
		return responsibilityName;
	}

	public void setResponsibilityName(String name){
		responsibilityName = name;
	}

	public String toString()
	{
		return "[id: " + responsibilityId + ", name: " + responsibilityName + "]" ;
	}
}//end of Responsibility class

