package mtu.construction.project;

import java.io.Serializable;

/****************
 * Time step size for the simulation
 * 
 * @author Matt Watkins
 *
 */
public class TimeFrame implements Serializable
{
	public static final TimeFrame ONE_DAY = new TimeFrame(1);
	public static final TimeFrame ONE_WEEK = new TimeFrame(7);
	public static final TimeFrame FOUR_WEEK = new TimeFrame(28);
	
	private int day_interval;
	
	/***********************
	 * New time frame for i day step interval
	 * @param i		size of interval
	 */
	private TimeFrame(int i)
	{
		day_interval = i;
	}
	
	/*******************
	 * Get the interval size
	 * @return	the interval size
	 */
	public int getInterval()
	{
		return day_interval;
	}
}
