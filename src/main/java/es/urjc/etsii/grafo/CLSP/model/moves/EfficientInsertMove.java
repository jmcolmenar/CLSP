package es.urjc.etsii.grafo.CLSP.model.moves;

import es.urjc.etsii.grafo.CLSP.model.CLSPSolution;

public class EfficientInsertMove extends InsertMove {

    protected int[][] iniMachineWeeklyProducedParts;
    protected int[][] endMachineWeeklyProducedParts;
    protected double changeoverTime;
    protected double shortage;

    public EfficientInsertMove(CLSPSolution solution, int initialMachineId, int initialPosition, int finalMachineId, int finalPosition, double moveValue) {
        super(solution, initialMachineId, initialPosition, finalMachineId, finalPosition, moveValue);
        this.iniMachineWeeklyProducedParts = null;
        this.endMachineWeeklyProducedParts = null;
    }

    public EfficientInsertMove(CLSPSolution solution, int initialMachineId, int initialPosition, int finalMachineId, int finalPosition, double moveValue, int[][] iniMachineWeeklyProducedParts, int[][] endMachineWeeklyProducedParts, double changeoverTime, double shortage) {
        this(solution, initialMachineId, initialPosition, finalMachineId, finalPosition, moveValue);
        // Assume the arrays are already copied
        this.iniMachineWeeklyProducedParts = iniMachineWeeklyProducedParts;
        this.endMachineWeeklyProducedParts = endMachineWeeklyProducedParts;
        this.changeoverTime = changeoverTime;
        this.shortage = shortage;
    }

    public double getChangeoverTime() {
        return changeoverTime;
    }

    @Override
    protected boolean _execute(CLSPSolution solution) {
        boolean result = performMove(solution);

        solution.updateDataStructuresOptimized(initialMachineId,initialPosition,finalMachineId,finalPosition,changeoverTime,shortage,iniMachineWeeklyProducedParts,endMachineWeeklyProducedParts);

        // Efficient score calculation
        solution.setScore(solution.getScore() + this.moveValue);

        return result;
    }

    @Override
    public String toString() {
        return "EfficientInsertMove{" +
                "initialMachineId=" + initialMachineId +
                ", initialPosition=" + initialPosition +
                ", finalMachineId=" + finalMachineId +
                ", finalPosition=" + finalPosition +
                ", moveValue=" + moveValue +
                '}';
    }

    public double getShortage() {
        return shortage;
    }
}
