package es.urjc.etsii.grafo.CLSP.model.neighborhoods;

import es.urjc.etsii.grafo.CLSP.model.CLSPInstance;
import es.urjc.etsii.grafo.CLSP.model.CLSPSolution;
import es.urjc.etsii.grafo.CLSP.model.moves.EfficientInsertMove;
import es.urjc.etsii.grafo.CLSP.model.moves.InsertMove;
import es.urjc.etsii.grafo.solution.neighborhood.ExploreResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class EfficientInsertNeighborhoodPriority extends EfficientInsertNeighborhood {

    @Override
    public ExploreResult<InsertMove, CLSPSolution, CLSPInstance> explore(CLSPSolution solution) {


        List<InsertMove> list = new ArrayList<>();
        
        // Take only improving moves and filter afterwards.
        List<InsertMove> movesList = new ArrayList<>();
        Stream<InsertMove> moves = super.explore(solution).moves();
        moves.filter(m -> m.getMoveValue() <= 0).forEach(movesList::add);

        for (InsertMove m : movesList) {
            EfficientInsertMove em = (EfficientInsertMove) m;

            // Only check priority for moves that improve priority
            int partId = solution.getSolutionData()[em.getInitialMachineId()][em.getInitialPosition()].getPartId();
            if (solution.getInstance().getPriority(partId,em.getInitialMachineId()) <= 1)
                continue;

            // Only changing to a different allowed machine will modify priority
            if ((em.getInitialMachineId() == em.getFinalMachineId()) || solution.getInstance().getPriority(partId, em.getFinalMachineId()) == 0)
                continue;

            // Only consider moves that improve priority
            if (solution.getInstance().getPriority(partId, em.getFinalMachineId()) >= solution.getInstance().getPriority(partId,em.getInitialMachineId()))
                continue;

            // Include the move with the cost in the new objective function.
            double priorityImprovement = ((solution.getInstance().getPriority(partId, em.getFinalMachineId()) - solution.getInstance().getPriority(partId,em.getInitialMachineId())) * solution.getSolutionData()[em.getInitialMachineId()][em.getInitialPosition()].getDuration());
            em.setPriorityImprovement(priorityImprovement);
            list.add(em);

        }

        return ExploreResult.fromList(list);
    }

}

