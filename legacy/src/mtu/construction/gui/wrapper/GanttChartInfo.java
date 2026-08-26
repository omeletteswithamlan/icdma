package mtu.construction.gui.wrapper;

public class GanttChartInfo {
	G_Activity activity;
	int start;
	int finish;
	boolean critical;
	
	public GanttChartInfo(G_Activity a, int s, int f, boolean c){
		activity = a;
		start = s;
		finish = f;
		critical = c;
	}
	
	public int getStart(){
		return start;
	}
	
	public int getEnd(){
		return finish;
	}
	
	public boolean getCritical(){
		return critical;
	}
	
	public G_Activity getActivity(){
		return activity;
	}
	
}
