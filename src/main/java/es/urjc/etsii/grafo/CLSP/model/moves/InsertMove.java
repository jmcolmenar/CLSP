package es.urjc.etsii.grafo.CLSP.model.moves;

import es.urjc.etsii.grafo.CLSP.model.CLSPSolution;
import es.urjc.etsii.grafo.CLSP.model.WorkSlot;

public class InsertMove extends CLSPBaseMove {
    /**
     * Insertion move
     *
     * @param solution        Solution
     * @param initialMachineId Initial Machine id
     * @param initialPosition Initial position of the workslot
     * @param finalMachineId Final machine id
     * @param finalPosition Final position of the workslot
     */
    public InsertMove(CLSPSolution solution, int initialMachineId, int initialPosition, int finalMachineId, int finalPosition) {
        super(solution, initialMachineId, initialPosition, finalMachineId, finalPosition);
    }

    public InsertMove(CLSPSolution solution, int initialMachineId, int initialPosition, int finalMachineId, int finalPosition, double moveValue) {
        super(solution, initialMachineId, initialPosition, finalMachineId, finalPosition, moveValue);
    }

    public InsertMove(CLSPSolution solution, int initialMachineId, int initialPosition, int finalMachineId, int finalPosition, double moveValue, double priorityImprovement) {
        super(solution, initialMachineId, initialPosition, finalMachineId, finalPosition, moveValue, priorityImprovement);
    }

    @Override
    protected boolean _execute(CLSPSolution solution) {
        boolean result = performMove(solution);

        // Inefficient score recalculation
        solution.setScore(solution.recalculateScore());

        return result;
    }

    protected boolean performMove(CLSPSolution solution) {
        if ((initialMachineId == finalMachineId) && (initialPosition == finalPosition)) {
            return true;
        }
        WorkSlot aux = solution.getSolutionData()[initialMachineId][initialPosition];
        // If the machine is the same, the move is simpler
        if (initialMachineId == finalMachineId) {
            if (initialPosition < finalPosition) {
                for (int i = initialPosition; i < finalPosition; i++) {
                    solution.getSolutionData()[initialMachineId][i] = solution.getSolutionData()[initialMachineId][i + 1];
                }
            } else {
                for (int i = initialPosition; i > finalPosition; i--) {
                    solution.getSolutionData()[initialMachineId][i] = solution.getSolutionData()[initialMachineId][i - 1];
                }
            }
        } else {
            // If the machine is different, we have to move the workslot to the new machine updating the workslot count
            for (int i = initialPosition; i < solution.getNumberOfMachineWorkSlots()[initialMachineId] - 1; i++) {
                solution.getSolutionData()[initialMachineId][i] = solution.getSolutionData()[initialMachineId][i + 1];
            }
            solution.getNumberOfMachineWorkSlots()[initialMachineId]--;
            for (int i = solution.getNumberOfMachineWorkSlots()[finalMachineId]; i > finalPosition; i--) {
                solution.getSolutionData()[finalMachineId][i] = solution.getSolutionData()[finalMachineId][i - 1];
            }
            solution.getNumberOfMachineWorkSlots()[finalMachineId]++;
        }
        solution.getSolutionData()[finalMachineId][finalPosition] = aux;

        return true;
    }

    @Override
    public String toString() {
        return "InsertMove{" +
                "initialMachineId=" + initialMachineId +
                ", initialPosition=" + initialPosition +
                ", finalMachineId=" + finalMachineId +
                ", finalPosition=" + finalPosition +
                ", moveValue=" + moveValue +
                ", priorityImprovement=" + priorityImprovement +
                '}';
    }
}
