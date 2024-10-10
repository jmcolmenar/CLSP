package es.urjc.etsii.grafo.CLSP.model.neighborhoods;

import es.urjc.etsii.grafo.CLSP.model.*;
import es.urjc.etsii.grafo.CLSP.model.moves.EfficientExchangeMove;
import es.urjc.etsii.grafo.CLSP.model.moves.ExchangeMove;
import es.urjc.etsii.grafo.solution.neighborhood.ExploreResult;
import es.urjc.etsii.grafo.util.random.RandomManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EfficientExchangeNeighborhood extends ExchangeNeighborhood {

    @Override
    public Optional<ExchangeMove> getRandomMove(CLSPSolution solution) {
        List<Coordinate> moves = generateMoves(solution,true);
        if (moves.isEmpty()) {
            return Optional.empty();
        }

        ExchangeMove move;
        do {
            // Get random coordinate from the list:
            Coordinate c = moves.get(RandomManager.getRandom().nextInt(moves.size()));
            // The move can be null if it is not possible
            move = generateMove(solution, c);
        } while (move == null);

        return Optional.of(move);
    }


    public ExploreResult<ExchangeMove, CLSPSolution, CLSPInstance> explore(CLSPSolution solution) {
        List<ExchangeMove> list = new ArrayList<>();
        List<Coordinate> moves = generateMoves(solution,true);

        for (Coordinate c : moves) {
            var move = generateMove(solution, c);
            if (move != null)
                list.add(move);
        }

        return ExploreResult.fromList(list);
    }

    /**
     * Generates a move from a coordinate.
     * @param solution current solution
     * @param c coordinate
     * @return move or null if the move is not possible with the current coordinates.
     */
    private ExchangeMove generateMove(CLSPSolution solution, Coordinate c) {
        double moveValue;
        // Efficiently create the insert move
        int mIni = c.machineIni();
        int mEnd = c.machineEnd();
        int pIni = c.positionIni();
        int pEnd = c.positionEnd();

        // Exchange moves between same type of workslots have no effect
        if ((solution.getSolutionData()[mIni][pIni].getPartId() == solution.getSolutionData()[mEnd][pEnd].getPartId()) &&
                (solution.getSolutionData()[mIni][pIni].getDuration() == solution.getSolutionData()[mEnd][pEnd].getDuration()))
            return null;

        // Machine weekly produced parts for the move
        int[][] machineIniWeeklyProducedParts = new int[solution.getInstance().getNumPeriods()][solution.getInstance().getNumParts()];
        int[][] machineEndWeeklyProducedParts = new int[solution.getInstance().getNumPeriods()][solution.getInstance().getNumParts()];

        double changeoverTime = solution.getChangeoverTime();

        // Same machine
        if (mIni == mEnd) {

            // New workslot sequence, copied from solution data
            WorkSlot[] afterMove = new WorkSlot[solution.getNumberOfMachineWorkSlots()[mIni]];
            System.arraycopy(solution.getSolutionData()[mIni], 0, afterMove, 0, solution.getNumberOfMachineWorkSlots()[mIni]);

            // Workslots still not moved. Calculate here to avoid different calculations due to adjacent workslots
            WorkSlot wsIni = afterMove[pIni];
            WorkSlot wsEnd = afterMove[pEnd];

            // Calculates new changeover time

            // Time for adjacent workslots are calculated differently
            if (Math.abs(pIni-pEnd) == 1) {
                int first = Math.min(pIni, pEnd);
                int second = Math.max(pIni, pEnd);
                if (first > 0) {
                    changeoverTime -= solution.getInstance().changeoverTime[afterMove[first - 1].getPartId()][afterMove[first].getPartId()];
                    changeoverTime += solution.getInstance().changeoverTime[afterMove[first - 1].getPartId()][afterMove[second].getPartId()];
                }
                if (second < (afterMove.length-1)) {
                    changeoverTime -= solution.getInstance().changeoverTime[afterMove[second].getPartId()][afterMove[second + 1].getPartId()];
                    changeoverTime += solution.getInstance().changeoverTime[afterMove[first].getPartId()][afterMove[second + 1].getPartId()];
                }
            } else {
                // Update the changeover times of the workslots that have been moved
                if (pIni > 0) {
                    changeoverTime -= solution.getInstance().changeoverTime[afterMove[pIni - 1].getPartId()][wsIni.getPartId()];
                    changeoverTime += solution.getInstance().changeoverTime[afterMove[pIni - 1].getPartId()][wsEnd.getPartId()];
                }
                if (pIni < (afterMove.length - 1)) {
                    changeoverTime -= solution.getInstance().changeoverTime[wsIni.getPartId()][afterMove[pIni + 1].getPartId()];
                    changeoverTime += solution.getInstance().changeoverTime[wsEnd.getPartId()][afterMove[pIni + 1].getPartId()];
                }
                if (pEnd > 0) {
                    changeoverTime -= solution.getInstance().changeoverTime[afterMove[pEnd - 1].getPartId()][wsEnd.getPartId()];
                    changeoverTime += solution.getInstance().changeoverTime[afterMove[pEnd - 1].getPartId()][wsIni.getPartId()];
                }
                if (pEnd < (afterMove.length - 1)) {
                    changeoverTime -= solution.getInstance().changeoverTime[wsEnd.getPartId()][afterMove[pEnd + 1].getPartId()];
                    changeoverTime += solution.getInstance().changeoverTime[wsIni.getPartId()][afterMove[pEnd + 1].getPartId()];
                }
            }

            // Move implemented as in "performMove" in InsertMove
            afterMove[pIni] = wsEnd;
            afterMove[pEnd] = wsIni;

            // Calculate this machine's weekly production --> Use machineIniWeeklyProducedParts
            CLSPSolution.calculateMachineWeeklyProducedParts(solution.getInstance(), machineIniWeeklyProducedParts, afterMove, afterMove.length, mIni);

        } else {

            // New workslot sequences copied
            WorkSlot[] afterMoveIni = new WorkSlot[solution.getNumberOfMachineWorkSlots()[mIni]];
            System.arraycopy(solution.getSolutionData()[mIni], 0, afterMoveIni, 0, solution.getNumberOfMachineWorkSlots()[mIni]);
            WorkSlot[] afterMoveEnd = new WorkSlot[solution.getNumberOfMachineWorkSlots()[mEnd]];
            System.arraycopy(solution.getSolutionData()[mEnd], 0, afterMoveEnd, 0, solution.getNumberOfMachineWorkSlots()[mEnd]);

            // Move implemented as in "performMove" in InsertMove
            WorkSlot auxMove = afterMoveIni[pIni];
            afterMoveIni[pIni] = afterMoveEnd[pEnd];
            afterMoveEnd[pEnd] = auxMove;

            // Calculate this machine's weekly production --> Use machineIniWeeklyProducedParts
            CLSPSolution.calculateMachineWeeklyProducedParts(solution.getInstance(), machineIniWeeklyProducedParts, afterMoveIni, afterMoveIni.length, mIni);
            CLSPSolution.calculateMachineWeeklyProducedParts(solution.getInstance(), machineEndWeeklyProducedParts, afterMoveEnd, afterMoveEnd.length, mEnd);

            // Workslots already moved, but taking into account their previous position
            WorkSlot wsIni = afterMoveEnd[pEnd];
            WorkSlot wsEnd = afterMoveIni[pIni];

            // Calculates new changeover time
            if (pIni > 0) {
                changeoverTime -= solution.getInstance().changeoverTime[afterMoveIni[pIni - 1].getPartId()][wsIni.getPartId()];
                changeoverTime += solution.getInstance().changeoverTime[afterMoveIni[pIni - 1].getPartId()][wsEnd.getPartId()];
            }
            if (pIni < (afterMoveIni.length - 1)) {
                changeoverTime -= solution.getInstance().changeoverTime[wsIni.getPartId()][afterMoveIni[pIni + 1].getPartId()];
                changeoverTime += solution.getInstance().changeoverTime[wsEnd.getPartId()][afterMoveIni[pIni + 1].getPartId()];
            }
            if (pEnd > 0) {
                changeoverTime -= solution.getInstance().changeoverTime[afterMoveEnd[pEnd - 1].getPartId()][wsEnd.getPartId()];
                changeoverTime += solution.getInstance().changeoverTime[afterMoveEnd[pEnd - 1].getPartId()][wsIni.getPartId()];
            }
            if (pEnd < (afterMoveEnd.length - 1)) {
                changeoverTime -= solution.getInstance().changeoverTime[wsEnd.getPartId()][afterMoveEnd[pEnd + 1].getPartId()];
                changeoverTime += solution.getInstance().changeoverTime[wsIni.getPartId()][afterMoveEnd[pEnd + 1].getPartId()];
            }
        }

        // New weekly produced parts for the move: deal with pointers! and don't use a function
        int[][][] newWeeklyProducedParts = new int[solution.getInstance().getNumMachines()][solution.getInstance().getNumPeriods()][solution.getInstance().getNumParts()];
        // Accumulate using the other machines' weekly production
        for (int machine = 0; machine < solution.getInstance().getNumMachines(); machine++)
            if (machine == mIni) {
                newWeeklyProducedParts[machine] = machineIniWeeklyProducedParts;
            } else {
                if (machine == mEnd)
                    newWeeklyProducedParts[machine] = machineEndWeeklyProducedParts;
                else
                    newWeeklyProducedParts[machine] = solution.getMachineWeeklyProducedParts()[machine];
            }

        // Calculate score
        var weeklyProductShortage = new int[solution.getInstance().getNumPeriods()][solution.getInstance().getNumParts()];
        var newWeeklyShortage = solution.fullCalculateWeeklyShortage(newWeeklyProducedParts,weeklyProductShortage);
        double shortage = solution.calculateShortageFunction(newWeeklyShortage);
        double newScore = solution.scoreCalculation(changeoverTime, shortage);

        moveValue = newScore - solution.getScore();
        return new EfficientExchangeMove(solution, mIni, pIni, mEnd, pEnd, moveValue, newWeeklyProducedParts, changeoverTime,shortage);
    }

}
