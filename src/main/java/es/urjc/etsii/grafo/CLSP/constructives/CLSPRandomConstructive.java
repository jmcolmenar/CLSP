package es.urjc.etsii.grafo.CLSP.constructives;

import es.urjc.etsii.grafo.CLSP.model.CLSPInstance;
import es.urjc.etsii.grafo.CLSP.model.CLSPSolution;
import es.urjc.etsii.grafo.CLSP.model.WorkSlot;
import es.urjc.etsii.grafo.create.Constructive;
import es.urjc.etsii.grafo.util.random.RandomManager;

import java.util.HashMap;
import java.util.Map;

public class CLSPRandomConstructive extends Constructive<CLSPSolution, CLSPInstance> {

    @Override
    public CLSPSolution construct(CLSPSolution solution) {
        // IN --> Empty solution from solution(instance) constructor
        // OUT --> Feasible solution with an assigned score
        int numParts = solution.getInstance().getNumParts();
        int numMachines = solution.getInstance().getNumMachines();
        int numPeriods = solution.getInstance().getNumPeriods();

        // For each product, obtain the maximum number of units that are needed according to last week inventory.
        Map<Integer,Integer> pendingParts = new HashMap<>();
        for (int i = 0; i < numParts; i++) {
            pendingParts.put(i,(-1)*solution.getInstance().inventory[i][numPeriods-1]);
        }


        // While totalInventory is not 0, randomly select a machine and a product, and create a WorkSlot with a random number of units.
        while (!pendingParts.isEmpty()) {
            // Randomly select an element from pendingParts
            int rndPart =  (int) pendingParts.keySet().toArray()[(int) (RandomManager.getRandom().nextDouble() * (pendingParts.size()-1))];

            // Randomly select a VALID machine (production rate > 0)
            int rndMachine;
            do {
                rndMachine = (int) (RandomManager.getRandom().nextDouble() * (numMachines));
            } while (solution.getInstance().productionRate[rndPart][rndMachine] == 0);

            // Random selection of a part remaining production amount
            double duration;
            int units = (int) (RandomManager.getRandom().nextDouble() * pendingParts.get(rndPart));
            double minimumDuration = solution.getInstance().getMinimumWorkload();
            double minimumProduction = minimumDuration * solution.getInstance().productionRate[rndPart][rndMachine];

            if ((minimumProduction >= pendingParts.get(rndPart)) || (units < minimumProduction)) {
                duration = minimumDuration;
                units = (int) (minimumProduction);
            } else {
                duration = ((1.0) * units) / solution.getInstance().productionRate[rndPart][rndMachine];
            }

            // Create a work slot with the selected parameters according to the production rate
            WorkSlot ws = new WorkSlot(rndPart, duration);
            solution.addWorkSlot(rndMachine,ws);

            // Update pendingParts
            if (pendingParts.get(rndPart) - units <= 0)
                pendingParts.remove(rndPart);
            else
                pendingParts.put(rndPart, pendingParts.get(rndPart) - units);
        }

        // Accumulate weekly production because it is not done in the constructor
        solution.accumulateWeeklyProductionAndCalculatesScore(true);

        // Remember to call solution.notifyUpdate() if the solution is modified without using moves!!
        solution.notifyUpdate();
        return solution;
    }
}
