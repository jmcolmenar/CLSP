package es.urjc.etsii.grafo.CLSP.constructives;

import es.urjc.etsii.grafo.CLSP.model.CLSPInstance;
import es.urjc.etsii.grafo.CLSP.model.CLSPSolution;
import es.urjc.etsii.grafo.CLSP.model.WorkSlot;
import es.urjc.etsii.grafo.create.Constructive;
import es.urjc.etsii.grafo.util.CollectionUtil;
import es.urjc.etsii.grafo.util.random.RandomManager;

import java.util.List;

public class SmallPiecesHeuristicConstructive extends Constructive<CLSPSolution, CLSPInstance> {

    /**
     * Constructive method. It will create a number of regular workslots able to fill all the demand.
     *
     * @param solution Empty solution to create.
     */
    @Override
    public CLSPSolution construct(CLSPSolution solution) {

        List<WorkSlot> slots = solution.getInstance().getCopyOfSlotsNoShortage();

        // Mix slots
        CollectionUtil.shuffle(slots);

        // Assign slots to machines
        for (WorkSlot slot : slots) {
            // Randomly select a VALID machine (production rate > 0)
            int rndMachine;
            do {
                rndMachine = (int) (RandomManager.getRandom().nextDouble() * solution.getInstance().getNumMachines());
            } while (solution.getInstance().productionRate[slot.getPartId()][rndMachine] == 0);
            solution.addWorkSlot(rndMachine,new WorkSlot(slot));
        }

        // Accumulate weekly production because it is not done in the constructor
        solution.accumulateWeeklyProductionAndCalculatesScore(true);

        solution.notifyUpdate();
        return solution;
    }
}
