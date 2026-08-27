import mtu.construction.gui.wrapper.G_Activity;
import mtu.construction.gui.wrapper.G_ResourceAlloc;
import mtu.construction.icdma.Simulator;
import java.util.Vector;

public class NetProbe {
	public static void main(String[] args) throws Exception {
		int projectNo = Integer.parseInt(args[0]);
		int cap = Integer.parseInt(args[1]);
		Simulator sim = new Simulator("localhost", 5433, "vcdb", projectNo, "postgres", "");
		Simulator.numFutures = 0;
		for (int turn = 0; turn < cap; turn++) {
			System.out.println("TURN " + sim.getCurrentTimeStep() + " ready:");
			for (G_Activity a : sim.getReadyActivities())
				System.out.println("  R " + a.getID() + " " + a.getLabel() + " ES=" + a.getEarlyStart() + " end=" + a.getEnd());
			Vector<G_ResourceAlloc> req = new Vector<G_ResourceAlloc>();
			for (G_Activity a : sim.getReadyActivities()) {
				a.isActive();
				G_ResourceAlloc r = new G_ResourceAlloc(a);
				for (java.util.Map.Entry<mtu.construction.gui.wrapper.G_Material, Integer> e : a.getAsPlannedMaterialUse().entrySet())
					r.requestMaterial(e.getKey(), e.getValue());
				r.setWorkDays(5); r.setWorkHours(8); r.setWageIncentive(1.0);
				double totalOrdered = a.getTotalMaterialsOrdered();
				int duration = a.getEnd() - a.getStart();
				double val = (100.0 - totalOrdered) * duration;
				double orderDaily = 100 > val ? val : 100;
				r.setOrder(orderDaily);
				r.setTotalOrdered(totalOrdered + orderDaily / duration);
				req.add(r);
			}
			sim.setgResourceRequest(req);
			sim.setCrewRequest(sim.getLaborCrews());
			sim.update();
		}
		System.exit(0);
	}
}
