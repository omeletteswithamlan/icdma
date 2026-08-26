import mtu.construction.gui.wrapper.G_Activity;
import mtu.construction.icdma.Simulator;

import java.util.Vector;

/**
 * Headless boot test for the restored vcdb: load a project, print the
 * as-planned network, advance the simulation with no user decisions,
 * and report schedule/cost state. Usage: java SmokeTest [projectNo] [days]
 */
public class SmokeTest {
	public static void main(String[] args) {
		int projectNo = args.length > 0 ? Integer.parseInt(args[0]) : 523;
		int days = args.length > 1 ? Integer.parseInt(args[1]) : 40;

		System.out.println("== booting project " + projectNo + " ==");
		Simulator sim = new Simulator("localhost", 5433, "vcdb", projectNo, "postgres", "");

		Vector<G_Activity> acts = sim.getSortedActivityList();
		System.out.println("== as-planned network: " + acts.size() + " activities ==");
		for (G_Activity a : acts) {
			System.out.printf("  [%d] %-38s ES=%d end=%d total=$%.0f%n",
					a.getID(), a.getLabel(), a.getEarlyStart(), a.getEnd(), a.getTotal());
		}

		System.out.println("== query futures (20 samples) from t=1 ==");
		mtu.construction.tonae.QueryResult2 qr = sim.queryFutures(20, 30000);
		System.out.println("futures returned: " + qr.numResults());
		System.out.println("completion-day distribution {day=count}: " + qr.getDayDistribution());
		if (!qr.getBestCase().isEmpty())
			System.out.printf("best-case final cost:  $%.2f at day %d%n",
					qr.getBestCase().get(qr.getBestCase().lastKey()), qr.getBestCase().lastKey());
		if (!qr.getWorstCase().isEmpty())
			System.out.printf("worst-case final cost: $%.2f at day %d%n",
					qr.getWorstCase().get(qr.getWorstCase().lastKey()), qr.getWorstCase().lastKey());

		System.out.println("== advancing " + days + " intervals with default (as-planned) allocations ==");
		for (int d = 0; d < days; d++) {
			Vector<mtu.construction.gui.wrapper.G_ResourceAlloc> req = new Vector<>();
			for (mtu.construction.gui.wrapper.G_ResourceAlloc r : sim.getDefaultResourceAllocations())
				if (r.getActivity() != null)
					req.add(r);
			sim.setgResourceRequest(req);
			sim.setCrewRequest(sim.getLaborCrews());
			sim.update();
			if (sim.isFinished()) {
				System.out.println("== project finished at t=" + sim.getCurrentTimeStep() + " ==");
				break;
			}
		}

		System.out.println("== state after " + days + " intervals (t=" + sim.getCurrentTimeStep() + ") ==");
		for (G_Activity a : sim.getSortedActivityList()) {
			System.out.printf("  [%d] %-38s %.0f%% complete%s%n",
					a.getID(), a.getLabel(), 100 * a.getPercentCompletion(),
					a.isActive() ? "  (active)" : "");
		}
		Integer lastPlanned = sim.getAsPlannedTotal().isEmpty() ? null : sim.getAsPlannedTotal().lastKey();
		Integer lastBuilt = sim.getAsBuiltTotal().isEmpty() ? null : sim.getAsBuiltTotal().lastKey();
		if (lastPlanned != null)
			System.out.printf("as-planned cumulative total: $%.2f (through t=%d)%n",
					sim.getAsPlannedTotal().get(lastPlanned), lastPlanned);
		if (lastBuilt != null)
			System.out.printf("as-built   cumulative total: $%.2f (through t=%d)%n",
					sim.getAsBuiltTotal().get(lastBuilt), lastBuilt);
		System.out.println("== smoke test complete ==");
		System.exit(0);
	}
}
