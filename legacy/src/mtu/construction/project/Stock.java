package mtu.construction.project;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

import mtu.construction.project.MaterialType;

public class Stock implements Serializable
{
	private HashMap<MaterialType, Integer> stock;
	private double size; //The total space we have for stock
	private double curamt; //The current amount to space used
	
	public Stock(double size)
	{
		this.size = size;
		clear();
	}
	
	private Stock(HashMap<MaterialType, Integer> mat, double size, double curamt)
	{
		this.size = size;
		this.curamt = curamt;
		stock = (HashMap<MaterialType, Integer>)mat.clone();
	}
	
	public void clearPerishable()
	{
		//removing elements from a list upon which you are iterating is undefined, so build
		//a list of materials to remove, and then iterate on the new list
		Vector<MaterialType> matlist = new Vector<MaterialType>();
		for(MaterialType m : stock.keySet())
		{
			if(m.getPerishable())
				matlist.add(m);
		}
		
		for(MaterialType m : matlist)
			stock.remove(m);
	}
	
	public Set<Entry<MaterialType, Integer>> entrySet()
	{
		return stock.entrySet();
	}
	
	public Stock clone()
	{
		return new Stock(stock, size, curamt);
	}
	
	public double getValue()
	{
		double val = 0;
		for(Entry<MaterialType, Integer> e : stock.entrySet())
		{
			val += e.getValue() * e.getKey().getCost();
		}
		
		return val;
	}
	
	public int get(MaterialType m)
	{
		if(stock.containsKey(m))
			return stock.get(m);
		else
			return 0;
	}
	
	/**
	 * Request to remove a certain amount of material
	 * from Stock
	 * 
	 * @param m - the type of material to remove
	 * @param amt - the amount of material to remove
	 * 
	 * @return - the actual amount of material removed.
	 */
	public int remove(MaterialType m, int amt)
	{
		int quant = get(m);
		
		//Don't remove more material than we have!
		if(amt > quant)
			amt = quant;

		stock.put(m, quant - amt);
		curamt -= amt * m.getSize();
		return amt;
	}
	
	/**
	 * Add a certain amount of a material to Stock
	 * 
	 * @param m - the type of material to add
	 * @param amt - the quantity to add
	 * 
	 * @return - the amount of the material added to Stock
	 */
	public int add(MaterialType m, int amt)
	{
		int quant = get(m); //the amount of the material already in stock
		
		//If trying to add an amount that would cause stock
		// to overflow, only add enough to fill stock
		if(amt*m.getSize() + curamt > size)
			amt = (int)((size - curamt)/m.getSize());
		
		stock.put(m, quant + amt);
		curamt += amt * m.getSize();
		return amt;
	}
	
	public void clear()
	{
		curamt = 0;
		stock = new HashMap<MaterialType, Integer>();
	}
	
	public double getTotalSpace()
	{
		return size;
	}
	
	public double getAvailableSpace()
	{
		return size - curamt;
	}
	
	public double getUsedSpace()
	{
		return curamt;
	}
}
