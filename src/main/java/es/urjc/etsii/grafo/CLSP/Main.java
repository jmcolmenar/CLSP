package es.urjc.etsii.grafo.CLSP;

import es.urjc.etsii.grafo.algorithms.FMode;
import es.urjc.etsii.grafo.solver.Mork;
import es.urjc.etsii.grafo.util.DoubleComparator;

public class Main {
    public static void main(String[] args) {
        // DoubleComparator.setPrecision(0.00001);
        Mork.start(args, FMode.MINIMIZE);
    }
}
