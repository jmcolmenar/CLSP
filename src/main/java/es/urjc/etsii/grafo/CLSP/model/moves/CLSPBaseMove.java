package es.urjc.etsii.grafo.CLSP.model.moves;

import es.urjc.etsii.grafo.CLSP.model.CLSPInstance;
import es.urjc.etsii.grafo.CLSP.model.CLSPSolution;
import es.urjc.etsii.grafo.solution.LazyMove;

import java.util.Objects;

/**
 * Base class for all movements for the CLSP problem. All movements should extend this class.
 */
public abstract class CLSPBaseMove extends LazyMove<CLSPSolution, CLSPInstance> {

    protected int initialMachineId;
    protected int initialPosition;
    protected int finalMachineId;
    protected int finalPosition;
    protected double moveValue;
    protected double priorityImprovement;

    /**
     * Move constructor
     * @param solution solution
     */
    public CLSPBaseMove(CLSPSolution solution, int initialMachineId, int initialPosition, int finalMachineId, int finalPosition) {
        super(solution);
        this.initialMachineId = initialMachineId;
        this.initialPosition = initialPosition;
        this.finalMachineId = finalMachineId;
        this.finalPosition = finalPosition;
        
        // Obtain the movement cost (inefficiently)
        var s = new CLSPSolution(solution);
        this._execute(s);
        this.moveValue = s.getScore() - solution.getScore();

        // Priority improvement is not considered
        this.priorityImprovement = 0;
    }

    /**
     * Move constructor designed to provide movement cost
     * @param solution solution
     */
    public CLSPBaseMove(CLSPSolution solution, int initialMachineId, int initialPosition, int finalMachineId, int finalPosition, double moveValue) {
        super(solution);
        this.initialMachineId = initialMachineId;
        this.initialPosition = initialPosition;
        this.finalMachineId = finalMachineId;
        this.finalPosition = finalPosition;
        this.moveValue = moveValue;
        // Priority improvement is not considered
        this.priorityImprovement = 0;
    }

    /**
     * Move constructor designed to provide priority improvement
     * @param solution solution
     */
    public CLSPBaseMove(CLSPSolution solution, int initialMachineId, int initialPosition, int finalMachineId, int finalPosition, double moveValue, double priorityImprovement) {
        this(solution, initialMachineId, initialPosition, finalMachineId, finalPosition, moveValue);
        this.priorityImprovement = priorityImprovement;
    }


    /**
     * Executes the proposed move,
     * to be implemented by each move type.
     * @param solution Solution where this move will be applied to.
     * @return true if the solution has changed,
     * false if for any reason the movement is not applied or the solution does not change after executing the move
     */
    @Override
    protected abstract boolean _execute(CLSPSolution solution);

    /**
     * Get the movement value, represents how much does the move changes the f.o of a solution if executed
     *
     * @return f.o change
     */
    @Override
    public double getValue() {
        return this.moveValue;
    }

    public double getPriorityImprovement() {
        return priorityImprovement;
    }

    public void setPriorityImprovement(double priorityImprovement) {
        this.priorityImprovement = priorityImprovement;
    }

    /**
     * Get next move in this sequence.
     * There are two main strategies to generate moves:
     * - eagerly: all at once, and store them on a list
     * - lazily: only under demand, using Streams, like an Iterator
     * Only the second implementation requires implementing this method. Ignore this method if using the first one.
     *
     * @param solution solution used to generate the previous move,
     *                and where data will be picked for the current move
     * @return the next move in this generator sequence if there is a next move, return null to signal end of sequence.
     */
    @Override
    public LazyMove<CLSPSolution, CLSPInstance> next(CLSPSolution solution) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Returns a String representation of the current movement. Only use relevant fields.
     * Tip: Default IntelliJ implementation is fine
     *
     * @return human readable string
     */
    @Override
    public String toString() {
        return "CLSPBaseMove{" +
                "initialMachineId=" + initialMachineId +
                ", initialPosition=" + initialPosition +
                ", finalMachineId=" + finalMachineId +
                ", finalPosition=" + finalPosition +
                ", moveValue=" + moveValue +
                '}';
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CLSPBaseMove that = (CLSPBaseMove) o;
        return initialMachineId == that.initialMachineId && initialPosition == that.initialPosition
               && finalMachineId == that.finalMachineId && finalPosition == that.finalPosition;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(initialMachineId, initialPosition, finalMachineId, finalPosition, this.getClass().getName());
    }

    public double getMoveValue() {
        return moveValue;
    }

    public int getInitialMachineId() {
        return initialMachineId;
    }

    public int getInitialPosition() {
        return initialPosition;
    }

    public int getFinalMachineId() {
        return finalMachineId;
    }

    public int getFinalPosition() {
        return finalPosition;
    }
}
