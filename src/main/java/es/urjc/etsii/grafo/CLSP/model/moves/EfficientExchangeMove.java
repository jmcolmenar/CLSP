package es.urjc.etsii.grafo.CLSP.model.moves;

import es.urjc.etsii.grafo.CLSP.model.CLSPSolution;

public class EfficientExchangeMove extends ExchangeMove {

    protected int [][][] machineWeeklyProducedParts;
    protected double changeoverTime;
    protected double shortage;

    /**
     * Exchange move exchanges two WorkSlot elements
     *
     * @param solution         Solution
     * @param initialMachineId Initial Machine id
     * @param initialPosition  Initial position of the workslot
     * @param finalMachineId   Final machine id
     * @param finalPosition    Final position of the workslot
     */
    public EfficientExchangeMove(CLSPSolution solution, int initialMachineId, int initialPosition, int finalMachineId, int finalPosition, double moveValue) {
        super(solution, initialMachineId, initialPosition, finalMachineId, finalPosition, moveValue);
        this.machineWeeklyProducedParts = new int[solution.getInstance().getNumMachines()][solution.getInstance().getNumPeriods()][solution.getInstance().getNumParts()];
    }

    public EfficientExchangeMove(CLSPSolution solution, int initialMachineId, int initialPosition, int finalMachineId, int finalPosition, double moveValue, int[][][] newWeeklyProducedParts, double changeoverTime, double shortage) {
        this(solution, initialMachineId, initialPosition, finalMachineId, finalPosition, moveValue);
        for (int m = 0; m < solution.getInstance().getNumMachines(); m++) {
            for (int p = 0; p < solution.getInstance().getNumPeriods(); p++) {
                if (solution.getInstance().getNumParts() >= 0)
                    System.arraycopy(newWeeklyProducedParts[m][p], 0, this.machineWeeklyProducedParts[m][p], 0, solution.getInstance().getNumParts());
            }
        }
        this.changeoverTime = changeoverTime;
        this.shortage = shortage;
    }

    @Override
    protected boolean _execute(CLSPSolution solution) {
        boolean result = performMove(solution);

        solution.updateDataStructures(initialMachineId,initialPosition,finalMachineId,finalPosition,changeoverTime,machineWeeklyProducedParts, shortage);

        // Efficient score calculation
        solution.setScore(solution.getScore() + this.moveValue);

        return result;
    }

    @Override
    public String toString() {
        return "EfficientExchangeMove{" +
                "initialMachineId=" + initialMachineId +
                ", initialPosition=" + initialPosition +
                ", finalMachineId=" + finalMachineId +
                ", finalPosition=" + finalPosition +
                ", moveValue=" + moveValue +
                '}';
    }
}
