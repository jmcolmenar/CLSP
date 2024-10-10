package es.urjc.etsii.grafo.CLSP.model.moves;

import es.urjc.etsii.grafo.CLSP.model.CLSPSolution;

public class EfficientSwapMove extends EfficientInsertMove {


    public EfficientSwapMove(CLSPSolution solution, int initialMachineId, int initialPosition, int finalMachineId, int finalPosition, double moveValue, double changeoverTime, double shortage, int[][] iniMachineWeeklyProducedParts, int[][] endMachineWeeklyProducedParts) {
        super(solution, initialMachineId, initialPosition, finalMachineId, finalPosition, moveValue, iniMachineWeeklyProducedParts, endMachineWeeklyProducedParts, changeoverTime, shortage);

        this.iniMachineWeeklyProducedParts = new int[solution.getInstance().getNumPeriods()][solution.getInstance().getNumParts()];
        for (int p = 0; p < solution.getInstance().getNumPeriods(); p++) {
            if (solution.getInstance().getNumParts() >= 0)
                System.arraycopy(iniMachineWeeklyProducedParts[p], 0, this.iniMachineWeeklyProducedParts[p], 0, solution.getInstance().getNumParts());
        }

        this.endMachineWeeklyProducedParts = new int[solution.getInstance().getNumPeriods()][solution.getInstance().getNumParts()];
        for (int p = 0; p < solution.getInstance().getNumPeriods(); p++) {
            if (solution.getInstance().getNumParts() >= 0)
                System.arraycopy(endMachineWeeklyProducedParts[p], 0, this.endMachineWeeklyProducedParts[p], 0, solution.getInstance().getNumParts());
        }

    }

    @Override
    public String toString() {
        return "EfficientSwapMove{" +
                "initialMachineId=" + initialMachineId +
                ", initialPosition=" + initialPosition +
                ", finalMachineId=" + finalMachineId +
                ", finalPosition=" + finalPosition +
                ", moveValue=" + moveValue +
                ", changeoverTime=" + changeoverTime +
                ", shortage=" + shortage +
               '}';
    }
}
