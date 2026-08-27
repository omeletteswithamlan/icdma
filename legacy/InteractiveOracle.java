import mtu.construction.gui.wrapper.G_Activity;
import mtu.construction.gui.wrapper.G_ResourceAlloc;
import mtu.construction.icdma.Simulator;

import java.util.TreeMap;
import java.util.Vector;

/**
 * Headless oracle for the INTERACTIVE (TONAE) path under default play:
 * reproduces exactly what the Swing GUI does when the player accepts every
 * default each turn — default allocations, full crews, and the OrderPanel's
 * default daily order min(100, (100 - totalOrdered) * plannedDuration),
 * advancing the PNode's ordered percent by order/duration before simulating.
 * With no GUI, space violations are no-ops (no listeners), so deliveries are
 * clamped by Stock.add — deterministic. Usage: java InteractiveOracle [proj] [cap]
 */
public class InteractiveOracle {
	public static void main(String[] args) {
		int projectNo = args.length > 0 ? Integer.parseInt(args[0]) : 523;
		int cap = args.length > 1 ? Integer.parseInt(args[1]) : 400;
		Simulator sim = new Simulator("localhost", 5433, "vcdb", projectNo, "postgres", "");
		Simulator.numFutures = 0; // skip the per-turn Monte-Carlo — irrelevant to the books

		int turns = 0;
		while (!sim.isFinished() && turns < cap) {
			turns++;
			Vector<G_ResourceAlloc> req = new Vector<G_ResourceAlloc>();
			for (G_Activity a : sim.getReadyActivities()) {
				// node-backed wrappers, as the GUI uses — setTotalOrdered reaches the PNode
				a.isActive(); // side effect: lazily binds the PNode so setTotalOrdered reaches the network (GUI does this while rendering)
				G_ResourceAlloc r = new G_ResourceAlloc(a);
				for (java.util.Map.Entry<mtu.construction.gui.wrapper.G_Material, Integer> e
						: a.getAsPlannedMaterialUse().entrySet()) {
					r.requestMaterial(e.getKey(), e.getValue());
				}
				r.setWorkDays(5);
				r.setWorkHours(8);
				r.setWageIncentive(1.0);
				double totalOrdered = a.getTotalMaterialsOrdered();
				int duration = a.getEnd() - a.getStart(); // static planned duration (GUI semantics)
				double val = (100.0 - totalOrdered) * duration;
				double orderDaily = 100 > val ? val : 100; // OrderPanel spinner default
				r.setOrder(orderDaily);
				r.setTotalOrdered(totalOrdered + orderDaily / duration);
				req.add(r);
			}
			sim.setgResourceRequest(req);
			sim.setCrewRequest(sim.getLaborCrews());
			sim.update();
		}

		System.out.println("== interactive default play, project " + projectNo + " ==");
		System.out.println("turns simulated: " + turns + "  (t_now=" + sim.getCurrentTimeStep() + ")");
		TreeMap<Integer, Double> total = sim.getAsBuiltTotal();
		TreeMap<Integer, Double> direct = sim.getAsBuiltDirect();
		TreeMap<Integer, Double> indirect = sim.getAsBuiltIndirect();
		if (!total.isEmpty()) {
			int lk = total.lastKey();
			System.out.printf("asbuilt total   lastKey=%d value=%.6f%n", lk, total.get(lk));
			System.out.printf("asbuilt direct  value=%.6f%n", direct.get(direct.lastKey()));
			System.out.printf("asbuilt indirect value=%.6f%n", indirect.get(indirect.lastKey()));
		}
		// full per-day cumulative total, for divergence bisection against the TS port
		for (java.util.Map.Entry<Integer, Double> en : total.entrySet()) {
			System.out.printf("SERIES %d %.6f%n", en.getKey(), en.getValue());
		}
		for (G_Activity a : sim.getSortedActivityList()) {
			System.out.printf("  [%d] %-30s %.0f%% complete%n", a.getID(), a.getLabel(), 100 * a.getPercentCompletion());
		}
		System.exit(0);
	}
}
