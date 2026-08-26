import mtu.construction.icdma.Simulator;
import mtu.construction.gui.wrapper.G_Variable;
public class VarProbe {
  public static void main(String[] a) {
    Simulator sim = new Simulator("localhost", 5433, "vcdb", a.length>0?Integer.parseInt(a[0]):523, "postgres", "");
    System.out.println("== global variables in environment ==");
    for (G_Variable v : sim.getGlobalVariables())
      System.out.println("  '" + v.getLabel() + "' = " + v.getStringState());
    System.exit(0);
  }
}
