package es.urjc.etsii.grafo.CLSP.constructives.grasp;

import es.urjc.etsii.grafo.CLSP.model.CLSPInstance;
import es.urjc.etsii.grafo.CLSP.model.CLSPSolution;
import es.urjc.etsii.grafo.CLSP.model.WorkSlot;
import es.urjc.etsii.grafo.create.grasp.GRASPListManager;
import es.urjc.etsii.grafo.solution.EagerMove;
import es.urjc.etsii.grafo.util.DoubleComparator;
import es.urjc.etsii.grafo.util.random.RandomManager;
import java.util.ArrayList;
import java.util.List;

public class CLSPListManager extends GRASPListManager<CLSPListManager.CLSPGRASPMove, CLSPSolution, CLSPInstance> {

    /**
     * Generate initial candidate list. The list will be sorted if necessary by the constructive method.
     *
     * @param solution Current solution
     * @return a candidate list
     */
    @Override
    public List<CLSPGRASPMove> buildInitialCandidateList(CLSPSolution solution) {
        var list = new ArrayList<CLSPGRASPMove>();

        // If solution is empty, randomly add a workslot to a machine
        if (solution.isEmpty()) {
            // Select a random workslot
            WorkSlot rndWorkSlot = solution.getUnassignedWorkSlots().get((int) (RandomManager.getRandom().nextDouble() * (solution.getUnassignedWorkSlots().size() - 1)));
            // Randomly select a VALID machine (production rate > 0)
            int rndMachine;
            do {
                rndMachine = (int) (RandomManager.getRandom().nextDouble() * (solution.getInstance().getNumMachines()));
            } while (solution.getInstance().productionRate[rndWorkSlot.getPartId()][rndMachine] == 0);
            // Create a move with the selected parameters
            CLSPGRASPMove move = new CLSPGRASPMove(solution, rndMachine, rndWorkSlot);
            // Execute the move to get a more precise score of the move (all the structures are completely updated)
            move._execute(solution);
        }

        for (WorkSlot ws : solution.getUnassignedWorkSlots()) {
            for (int machine = 0; machine < solution.getInstance().getNumMachines(); machine++) {
                if (solution.getInstance().productionRate[ws.getPartId()][machine] > 0) {
                    // TODO: the calculation of the cost can be optimized
                    CLSPSolution copy = new CLSPSolution(solution);
                    // Create a move with the selected parameters
                    CLSPGRASPMove copyMove = new CLSPGRASPMove(copy, machine, new WorkSlot(ws));
                    // Execute the move
                    copyMove._execute(copy);
                    double sc = copy.recalculateScore();
                    double moveCost = sc - solution.getScore();
                    var move = new CLSPGRASPMove(solution, machine, ws, moveCost);
                    list.add(move);
                }
            }
        }

        return list;
    }

    /**
     * Update candidate list after each movement. The list will be sorted by the constructor.
     *
     * @param solution      Current solution, move has been already applied
     * @param move          Chosen move
     * @param index         index of the chosen move in the candidate list
     * @param candidateList original candidate list
     * @return an UNSORTED candidate list, where the best candidate is on the first position and the worst in the last
     */
    @Override
    public List<CLSPGRASPMove> updateCandidateList(CLSPSolution solution, CLSPGRASPMove move, List<CLSPGRASPMove> candidateList, int index) {

        return buildInitialCandidateList(solution);

    }


    // This is going to be an ADD move
    public static class CLSPGRASPMove extends EagerMove<CLSPSolution, CLSPInstance> {
        protected final int machine;
        protected final WorkSlot workSlot;
        protected double cost;

        public CLSPGRASPMove(CLSPSolution solution, int machine, WorkSlot ws) {
            super(solution);
            this.machine = machine;
            this.workSlot = ws;
        }

        public CLSPGRASPMove(CLSPSolution solution, int machine, WorkSlot ws, double cost) {
            this(solution, machine, ws);
            this.cost = cost;
        }

        @Override
        protected boolean _execute(CLSPSolution solution) {
            // Use field only to remove the workslot from the unassigned list
            solution.removeUnassignedWorkSlot(workSlot);
            // Create a copy to be inserted in the solution
            WorkSlot newWorkSlot = new WorkSlot(workSlot);
            // newWorkSlot is updated in addWorkSlot method
            solution.addWorkSlot(machine, newWorkSlot);
            // Update machineWeeklyProducedParts
            // Take into account change of period only if the workslot is not reaching the end of the horizon
            // We have to accumulate in the end period the units produced in the previous period.
            if ((newWorkSlot.getIniPeriod() != newWorkSlot.getEndPeriod()) &&
                    (newWorkSlot.getIniPeriod() < solution.getInstance().getNumPeriods()) &&
                    (newWorkSlot.getEndPeriod() < solution.getInstance().getNumPeriods())) {
                solution.getMachineWeeklyProducedParts()[machine][newWorkSlot.getEndPeriod()][newWorkSlot.getPartId()] += (int) Math.ceil((solution.getInstance().getMachineCapacity(machine, newWorkSlot.getIniPeriod()) - newWorkSlot.getIniTime()) * solution.getInstance().productionRate[newWorkSlot.getPartId()][machine]);
            }
            // Update the rest of the periods
            for (int i = newWorkSlot.getEndPeriod() + 1; i < solution.getInstance().getNumPeriods(); i++) {
                solution.getMachineWeeklyProducedParts()[machine][i][newWorkSlot.getPartId()] = solution.getMachineWeeklyProducedParts()[machine][i - 1][newWorkSlot.getPartId()];
            }
            solution.accumulateWeeklyProductionAndCalculatesScore(false);
            // Return true if the solution is modified,
            // false if the solution is the same, or for any reason the movement is not executed
            return true;
        }

        @Override
        public double getValue() {
            return this.cost;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CLSPGRASPMove that = (CLSPGRASPMove) o;

            if (machine != that.machine) return false;
            if (!DoubleComparator.equals(cost, that.cost)) return false;
            return workSlot.equals(that.workSlot);
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            result = machine;
            result = 31 * result + workSlot.hashCode();
            temp = Double.doubleToLongBits(cost);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return "CLSPGRASPMove{" +
                    "machine=" + machine +
                    ", workSlot=" + workSlot +
                    ", cost=" + cost +
                    '}';
        }

    }
}
