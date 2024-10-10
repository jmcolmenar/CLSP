package es.urjc.etsii.grafo.CLSP.model.neighborhoods;

import es.urjc.etsii.grafo.CLSP.model.CLSPInstance;
import es.urjc.etsii.grafo.CLSP.model.CLSPSolution;
import es.urjc.etsii.grafo.CLSP.model.moves.InsertMove;
import es.urjc.etsii.grafo.solution.neighborhood.ExploreResult;

import java.util.ArrayList;
import java.util.List;


public class InsertNeighborhood extends BasicNeighborhood<InsertMove> {

    @Override
    public ExploreResult<InsertMove, CLSPSolution, CLSPInstance> explore(CLSPSolution solution) {

        List<InsertMove> list = new ArrayList<>();

        List<Coordinate> moves = generateMoves(solution,false);
        for (Coordinate c : moves) {
            list.add(new InsertMove(solution, c.machineIni(), c.positionIni(), c.machineEnd(), c.positionEnd()));
        }

        return ExploreResult.fromList(list);
    }


}
