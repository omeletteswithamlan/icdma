package mtu.construction.tonae;

import java.io.Serializable;
import java.util.TreeMap;
import java.util.Map.Entry;

public class QueryResult implements Serializable
{
	protected TreeMap<Integer, Double> bestcase;
	protected TreeMap<Integer, Double> worstcase;
	protected TreeMap<Double, Integer> buckets;
	protected double bucketsize;
	private int numResults = 0; //total number of results
	
	public QueryResult(double bucketsize)
	{
		this.bucketsize = bucketsize;
		buckets = new TreeMap<Double, Integer>();
	}
	
	public void addResult(TreeMap<Integer, Double> ccase)
	{
		double amt = ccase.get(ccase.lastKey());

		if(bestcase == null || amt < bestcase.get(bestcase.lastKey()))
			bestcase = ccase;
		if(worstcase == null || amt > worstcase.get(worstcase.lastKey()))
			worstcase = ccase;
		
		amt /= bucketsize;
		int i = (int)amt;
		double d = (double)i;
		d *= bucketsize;
		
		if(buckets.containsKey(d))
			buckets.put(d, buckets.get(d) + 1);
		else
			buckets.put(d, 1);
		numResults++; //numResults
	}
	
	public double getBucketSize(){
		return bucketsize;
	}
	
	public TreeMap<Integer, Double> getBestCase()
	{
		return bestcase;
	}
	
	public TreeMap<Integer, Double> getWorstCase()
	{
		return worstcase;
	}
	
	public TreeMap<Double, Integer> getDistribution()
	{
		return buckets;
	}
	
	public int numResults(){
		return numResults;
	//	return buckets.size();
	/*
 		int amt = 0;
		for(Entry<Double, Integer> e : buckets.entrySet()){
			amt += e.getValue();
		}
		return amt;
	//*/
	}
	
	//Combine a QueryResult with this one
	public void add(QueryResult r){
		if(r == null){ System.out.println("NULL result :("); return;}
//		System.out.println("Adding "+r.numResults()+" results...");
		if(bestcase == null) bestcase = r.getBestCase();
		else if(r.getBestCase().get(r.getBestCase().lastKey()) < bestcase.get(bestcase.lastKey())){
			bestcase = r.getBestCase();
		}
		if(worstcase == null) worstcase = r.getWorstCase();
		else if(r.getWorstCase().get(r.getWorstCase().lastKey()) > worstcase.get(worstcase.lastKey())){
			worstcase = r.getWorstCase();
		}
		for(Entry<Double, Integer> e : r.getDistribution().entrySet()){
			int oldamt = 0;
			if(buckets.containsKey(e.getKey())) oldamt = buckets.get(e.getKey());
			buckets.put(e.getKey(), e.getValue()+oldamt);
			numResults += e.getValue(); //numResults
		}
	}
}
