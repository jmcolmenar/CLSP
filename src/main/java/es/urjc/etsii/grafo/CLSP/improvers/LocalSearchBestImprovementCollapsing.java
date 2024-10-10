package es.urjc.etsii.grafo.CLSP.improvers;

import es.urjc.etsii.grafo.CLSP.model.CLSPInstance;
import es.urjc.etsii.grafo.CLSP.model.CLSPSolution;
import es.urjc.etsii.grafo.CLSP.model.moves.CLSPBaseMove;
import es.urjc.etsii.grafo.algorithms.FMode;
import es.urjc.etsii.grafo.improve.ls.LocalSearchBestImprovement;
import es.urjc.etsii.grafo.solution.neighborhood.Neighborhood;

public class LocalSearchBestImprovementCollapsing extends LocalSearchBestImprovement<CLSPBaseMove, CLSPSolution, CLSPInstance> {

    public LocalSearchBestImprovementCollapsing(FMode fmode, Neighborhood neighborhood) {
        super(fmode, neighborhood);
    }

    @Override
    public boolean iteration(CLSPSolution solution) {

        // Collapse solution
        solution.collapse();

        boolean result = super.iteration(solution);

        // Uncollapse solution
        solution.uncollapse();

        return result;
    }


}
