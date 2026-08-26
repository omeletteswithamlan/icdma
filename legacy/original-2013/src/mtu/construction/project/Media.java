package mtu.construction.project;

import java.io.Serializable;

public class Media implements Serializable
{
	private int id;
	private String type;
	private Object obj;

	public Media(int id, String type, Object obj)
	{
		this.id = id;
		this.type=type;
		this.obj=obj;
	}
	
	public int getID()
	{
		return id;
	}
	
	public void setID(int id)
	{
		this.id = id;
	}
	
	public void setType(String type)
	{
		this.type=type;
	}
	
	public void setResource(Object obj)
	{
		this.obj=obj;
	}
	
	public String getType()
	{
		return type;
	}
	
	public Object getObject()
	{
		return obj;
	}
}
