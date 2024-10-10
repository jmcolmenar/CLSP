package es.urjc.etsii.grafo.CLSP.model.neighborhoods;

import es.urjc.etsii.grafo.CLSP.model.CLSPInstance;
import es.urjc.etsii.grafo.CLSP.model.CLSPSolution;
import es.urjc.etsii.grafo.CLSP.model.moves.ExchangeMove;
import es.urjc.etsii.grafo.solution.neighborhood.ExploreResult;

import java.util.ArrayList;
import java.util.List;

public class ExchangeNeighborhood extends BasicNeighborhood<ExchangeMove> {

    @Override
    public ExploreResult<ExchangeMove, CLSPSolution, CLSPInstance> explore(CLSPSolution solution) {

        List<ExchangeMove> list = new ArrayList<>();

        List<Coordinate> moves = generateMoves(solution,true);
        for (Coordinate c : moves) {
            list.add(new ExchangeMove(solution, c.machineIni(), c.positionIni(), c.machineEnd(), c.positionEnd()));
        }

        return ExploreResult.fromList(list);
    }
}
