package mtu.construction.tonae;

import java.io.Serializable;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;

public class QueryResult2 implements Serializable
{
	protected TreeMap<Integer, Double> bestcase; //worst case result
	protected TreeMap<Integer, Double> worstcase; //best case result
	//protected TreeMap<Double, Integer> buckets; //Cost, #results (bucketed costs)
	//protected TreeMap<Double, Integer> results; //Cost, #results (not bucketed costs)
	protected Vector<TreeMap<Integer, Double>> fullResults; //Vector of runs, each run has (Day, Cost) for each day
	//protected double bucketsize = 0;
	//private int numResults = 0; //total number of results
	
	public QueryResult2()
	{
		fullResults = new Vector<TreeMap<Integer, Double>>();
	}
	
	/*public QueryResult2(double bucketsize)
	{
		this();
		this.bucketsize = bucketsize;
		//buckets = new TreeMap<Double, Integer>();
	}*/
	
	public void addResult(TreeMap<Integer, Double> ccase)
	{
		double amt = ccase.get(ccase.lastKey());

		if(bestcase == null || amt < bestcase.get(bestcase.lastKey()))
			bestcase = ccase;
		if(worstcase == null || amt > worstcase.get(worstcase.lastKey()))
			worstcase = ccase;
		
		fullResults.add(ccase);
		//numResults++; //numResults
	}
	
	/*public double getBucketSize(){
		return bucketsize;
	}*/
	
	public TreeMap<Integer, Double> getBestCase()
	{
		return bestcase;
	}
	
	public TreeMap<Integer, Double> getWorstCase()
	{
		return worstcase;
	} 
	
	//Get the raw data
	public Vector<TreeMap<Integer, Double>> getResults(){
		return fullResults;
	}
	
	//Get a full distribution //Cost, #results (not bucketed costs)
	//not entirely useful, should be bucketed
	public TreeMap<Double, Integer> getEndResults(){
		TreeMap<Double, Integer> distribution = new TreeMap<Double, Integer>();
		for(TreeMap<Integer, Double> t : fullResults){
			double endResult = t.get(t.lastKey());
			int i = (int)endResult;
			double d = (double)i;
			if(distribution.containsKey(d)){
				distribution.put(d, distribution.get(d)+1);
			}
			else
				distribution.put(d, 1);
		}
		return distribution;
	}
	
	//Get a full distribution //Day(length of run), #results
	public TreeMap<Integer, Integer> getDayDistribution(){
		TreeMap<Integer, Integer> distribution = new TreeMap<Integer, Integer>();
		for(TreeMap<Integer, Double> t : fullResults){
			int day = t.lastKey();
			
			if(distribution.containsKey(day))
				distribution.put(day, distribution.get(day) + 1);
			else
				distribution.put(day, 1);
		}
		return distribution;
	}
	
	//Get a bucketed distribution //Cost, #results (bucketed costs)
	public TreeMap<Double, Integer> getDistribution(int bucketSize)
	{
		TreeMap<Double, Integer> distribution = new TreeMap<Double, Integer>();
		for(TreeMap<Integer, Double> t : fullResults){
			double amt = t.get(t.lastKey());
			amt /= bucketSize;
			int i = (int)amt;
			double d = (double)i;
			d *= bucketSize;
			
			if(distribution.containsKey(d))
				distribution.put(d, distribution.get(d) + 1);
			else
				distribution.put(d, 1);
		}
		return distribution;
	}
	
	//Data stored as Day, Cost
	public Vector<Object[]> getEndData(){
		Vector<Object[]> thing = new Vector<Object[]>();
		for(TreeMap<Integer, Double> t : fullResults){
			int day = t.lastKey();
			double cost = t.get(day);
			Object[] value = {day, cost};
			thing.add(value);
		}
		return thing;
	}
	
	public int numResults(){
		//return numResults;
		return fullResults.size();
	}
	
	//Combine a QueryResult with this one
	public void add(QueryResult2 qr){
		if(qr == null){ System.out.println("NULL result :("); return;}
//		System.out.println("Adding "+r.numResults()+" results...");
		if(bestcase == null) bestcase = qr.getBestCase();
		else if(qr.getBestCase().get(qr.getBestCase().lastKey()) < bestcase.get(bestcase.lastKey())){
			bestcase = qr.getBestCase();
		}
		if(worstcase == null) worstcase = qr.getWorstCase();
		else if(qr.getWorstCase().get(qr.getWorstCase().lastKey()) > worstcase.get(worstcase.lastKey())){
			worstcase = qr.getWorstCase();
		}
		for(TreeMap<Integer, Double> t : qr.getResults()){
			addResult(t);
		}
	}
	
}
