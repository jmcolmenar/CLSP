package es.urjc.etsii.grafo.CLSP.model.neighborhoods;

import es.urjc.etsii.grafo.CLSP.model.moves.CLSPBaseMove;
import es.urjc.etsii.grafo.CLSP.model.CLSPInstance;
import es.urjc.etsii.grafo.CLSP.model.CLSPSolution;
import es.urjc.etsii.grafo.solution.neighborhood.ExploreResult;
import es.urjc.etsii.grafo.solution.neighborhood.RandomizableNeighborhood;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class BasicNeighborhood<M extends CLSPBaseMove> extends RandomizableNeighborhood<M, CLSPSolution, CLSPInstance> {
    @Override
    public Optional<M> getRandomMove(CLSPSolution solution) {
        throw new UnsupportedOperationException("Random move not implemented");
    }

    @Override
    public abstract ExploreResult<M, CLSPSolution, CLSPInstance> explore(CLSPSolution solution);

    protected List<Coordinate> generateMoves (CLSPSolution solution, boolean isExchange) {
        List<Coordinate> list = new ArrayList<>();
        for (int machineIni = 0; machineIni < solution.getInstance().getNumMachines(); machineIni++) {
            for (int posIni = 0; posIni < solution.getNumberOfMachineWorkSlots()[machineIni]; posIni++) {
                // Moves can be made in the same machine.
                for (int machineEnd = 0; machineEnd < solution.getInstance().getNumMachines(); machineEnd++) {
                    // Insertion must allow to include a workslot at the end of the machine.
                    for (int posEnd = 0; posEnd <= solution.getNumberOfMachineWorkSlots()[machineEnd]; posEnd++) {
                        // Moves in different places.
                        if ((machineIni == machineEnd) && (posIni == posEnd)) {
                            continue;
                        }
                        // If the destination machine is not able to produce the part, the move is not possible.
                        if (solution.getInstance().productionRate[solution.getSolutionData()[machineIni][posIni].getPartId()][machineEnd] == 0) {
                            continue;
                        }
                        // Exchange is not possible after the last slot of the machine.
                        // No insert after the last slot of the machine is possible in the same machine.
                        if ((posEnd == solution.getNumberOfMachineWorkSlots()[machineEnd]) && ((machineIni == machineEnd) || isExchange)) {
                            continue;
                        }
                        // Exchange must check that the initial machine can produce the part from the final machine.
                        if (isExchange && (solution.getInstance().productionRate[solution.getSolutionData()[machineEnd][posEnd].getPartId()][machineIni] == 0)) {
                            continue;
                        }
                        list.add(new Coordinate(machineIni, posIni, machineEnd, posEnd));
                    }
                }
            }
        }
        return list;
    }

    public record Coordinate (int machineIni, int positionIni, int machineEnd, int positionEnd) implements Comparable<Coordinate>{
        @Override
        public int compareTo(Coordinate o) {
            if (machineIni < o.machineIni) {
                return -1;
            } else if (machineIni > o.machineIni) {
                return 1;
            } else {
                if (positionIni < o.positionIni) {
                    return -1;
                } else if (positionIni > o.positionIni) {
                    return 1;
                } else {
                    if (machineEnd < o.machineEnd) {
                        return -1;
                    } else if (machineEnd > o.machineEnd) {
                        return 1;
                    } else {
                        if (positionEnd < o.positionEnd) {
                            return -1;
                        } else if (positionEnd > o.positionEnd) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                }
            }
        }
    }



}
