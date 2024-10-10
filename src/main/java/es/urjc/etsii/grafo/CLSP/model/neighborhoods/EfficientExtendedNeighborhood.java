package es.urjc.etsii.grafo.CLSP.model.neighborhoods;

import es.urjc.etsii.grafo.CLSP.model.moves.CLSPBaseMove;
import es.urjc.etsii.grafo.CLSP.model.CLSPInstance;
import es.urjc.etsii.grafo.CLSP.model.CLSPSolution;
import es.urjc.etsii.grafo.solution.neighborhood.ExploreResult;

import java.util.ArrayList;
import java.util.List;

public class EfficientExtendedNeighborhood extends BasicNeighborhood<CLSPBaseMove> {

    EfficientInsertNeighborhood insertNeighborhood = new EfficientInsertNeighborhood();
    EfficientExchangeNeighborhood exchangeNeighborhood = new EfficientExchangeNeighborhood();

    @Override
    public ExploreResult<CLSPBaseMove, CLSPSolution, CLSPInstance> explore(CLSPSolution solution) {
        List<CLSPBaseMove> list = new ArrayList<>();

        // The implementation is different than the one in ExtendedNeighborhood. Coordinates are processed differently.
        // Best improvement should behave similarly, but not first improvement.
        list.addAll(insertNeighborhood.explore(solution).moves().toList());
        list.addAll(exchangeNeighborhood.explore(solution).moves().toList());

        return ExploreResult.fromList(list);
    }
}
