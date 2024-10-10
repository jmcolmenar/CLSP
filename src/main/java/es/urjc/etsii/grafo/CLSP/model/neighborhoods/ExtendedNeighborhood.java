package es.urjc.etsii.grafo.CLSP.model.neighborhoods;

import es.urjc.etsii.grafo.CLSP.model.*;
import es.urjc.etsii.grafo.CLSP.model.moves.CLSPBaseMove;
import es.urjc.etsii.grafo.CLSP.model.moves.ExchangeMove;
import es.urjc.etsii.grafo.CLSP.model.moves.InsertMove;
import es.urjc.etsii.grafo.solution.neighborhood.ExploreResult;

import java.util.ArrayList;
import java.util.List;

public class ExtendedNeighborhood extends BasicNeighborhood<CLSPBaseMove> {
    @Override
    public ExploreResult<CLSPBaseMove, CLSPSolution, CLSPInstance> explore(CLSPSolution solution) {
        List<CLSPBaseMove> list = new ArrayList<>();

        List<Coordinate> moves = generateMoves(solution,true);
        for (Coordinate c : moves) {
            list.add(new InsertMove(solution, c.machineIni(), c.positionIni(), c.machineEnd(), c.positionEnd()));
            if (c.positionEnd() < solution.getNumberOfMachineWorkSlots()[c.machineEnd()]) {
                list.add(new ExchangeMove(solution, c.machineIni(), c.positionIni(), c.machineEnd(), c.positionEnd()));
            }
        }

        return ExploreResult.fromList(list);
    }
}
