package es.urjc.etsii.grafo.CLSP.model.moves;

import es.urjc.etsii.grafo.CLSP.model.CLSPSolution;
import es.urjc.etsii.grafo.CLSP.model.WorkSlot;

public class ExchangeMove extends CLSPBaseMove {
    /**
     * Exchange move exchanges two WorkSlot elements
     *
     * @param solution        Solution
     * @param initialMachineId Initial Machine id
     * @param initialPosition Initial position of the workslot
     * @param finalMachineId Final machine id
     * @param finalPosition Final position of the workslot
     */
    public ExchangeMove(CLSPSolution solution, int initialMachineId, int initialPosition, int finalMachineId, int finalPosition) {
        super(solution, initialMachineId, initialPosition, finalMachineId, finalPosition);
    }

    public ExchangeMove(CLSPSolution solution, int initialMachineId, int initialPosition, int finalMachineId, int finalPosition, double moveValue) {
        super(solution, initialMachineId, initialPosition, finalMachineId, finalPosition, moveValue);
    }

    @Override
    protected boolean _execute(CLSPSolution solution) {
        boolean result = performMove(solution);

        solution.setScore(solution.recalculateScore());

        return result;
    }

    protected boolean performMove(CLSPSolution solution) {
        if ((initialMachineId == finalMachineId) && (initialPosition == finalPosition)) {
            return true;
        }
        WorkSlot aux = solution.getSolutionData()[initialMachineId][initialPosition];

        // Exchange the workslots
        solution.getSolutionData()[initialMachineId][initialPosition] = solution.getSolutionData()[finalMachineId][finalPosition];
        solution.getSolutionData()[finalMachineId][finalPosition] = aux;
        return true;
    }

    @Override
    public String toString() {
        return "ExchangeMove{" +
                "initialMachineId=" + initialMachineId +
                ", initialPosition=" + initialPosition +
                ", finalMachineId=" + finalMachineId +
                ", finalPosition=" + finalPosition +
                ", moveValue=" + moveValue +
                '}';
    }
}
