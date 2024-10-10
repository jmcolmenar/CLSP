package es.urjc.etsii.grafo.CLSP.hexaly;

import es.urjc.etsii.grafo.CLSP.model.CLSPInstance;
import localsolver.LocalSolver;

public class CLSPModel {

    LocalSolver localSolver;

    CLSPInstance instance;

    int nParts;
    int nMachines;

    int nPeriods;

    /**
     * Workslot definition
     */
    HexalyWorkSlot[] workslots;

    int[] partIds;

    double[] durations;
}
