package es.urjc.etsii.grafo.CLSP.model.neighborhoods;


import es.urjc.etsii.grafo.CLSP.model.CLSPInstance;
import es.urjc.etsii.grafo.CLSP.model.CLSPSolution;
import es.urjc.etsii.grafo.CLSP.model.WorkSlot;
import es.urjc.etsii.grafo.CLSP.model.moves.EfficientInsertMove;
import es.urjc.etsii.grafo.CLSP.model.moves.InsertMove;
import es.urjc.etsii.grafo.solution.neighborhood.ExploreResult;
import java.util.ArrayList;
import java.util.List;

public class EfficientInsertNeighborhood extends InsertNeighborhood {

    protected EfficientInsertMove generateMoveSameMachine(CLSPSolution solution, int mIni, int pIni, int pEnd) {
        // Machine weekly produced parts for the move (same machine)
        int[][] machineIniWeeklyProducedParts = new int[solution.getInstance().getNumPeriods()][solution.getInstance().getNumParts()];
        int[][] machineEndWeeklyProducedParts = machineIniWeeklyProducedParts;
        int[][][] newWeeklyProducedParts = new int[solution.getInstance().getNumMachines()][solution.getInstance().getNumPeriods()][solution.getInstance().getNumParts()];

        double changeoverTime = solution.getChangeoverTime();

        // New workslot sequence, copied from solution data
        WorkSlot[] afterMove = new WorkSlot[solution.getNumberOfMachineWorkSlots()[mIni]];
        System.arraycopy(solution.getSolutionData()[mIni], 0, afterMove, 0, solution.getNumberOfMachineWorkSlots()[mIni]);

        // Move implemented as in "performMove" in InsertMove
        WorkSlot aux = afterMove[pIni];
        // If the machine is the same, the move is simpler
        if (pIni < pEnd) {
            for (int i = pIni; i < pEnd; i++) {
                afterMove[i] = afterMove[i + 1];
            }
        } else {
            for (int i = pIni; i > pEnd; i--) {
                afterMove[i] = afterMove[i - 1];
            }
        }
        afterMove[pEnd] = aux;

        // Calculate this machine's weekly production --> Use machineIniWeeklyProducedParts
        CLSPSolution.calculateMachineWeeklyProducedParts(solution.getInstance(), machineIniWeeklyProducedParts, afterMove, afterMove.length, mIni);

        // Calculates new changeover time
        // The update is different if the workslot is moved to the beginning or the end of the sequence
        if (pIni < pEnd) {
            // pIni = 1 -> pEnd = 10
            // 0 1 2 3 4 5 6 7 8 9 10 11 12  --> 0 2 3 4 5 6 7 8 9 10 1 11 12
            if (pIni > 0)
                changeoverTime -= solution.getInstance().changeoverTime[afterMove[pIni - 1].getPartId()][aux.getPartId()];
            if (pIni < (afterMove.length - 1))
                changeoverTime -= solution.getInstance().changeoverTime[aux.getPartId()][afterMove[pIni].getPartId()];
            if ((pIni > 0) && (pIni < (afterMove.length - 1)))
                changeoverTime += solution.getInstance().changeoverTime[afterMove[pIni - 1].getPartId()][afterMove[pIni].getPartId()];
            changeoverTime += solution.getInstance().changeoverTime[afterMove[pEnd - 1].getPartId()][aux.getPartId()];
            if (pEnd < (afterMove.length - 1)) {
                changeoverTime += solution.getInstance().changeoverTime[aux.getPartId()][afterMove[pEnd + 1].getPartId()];
                changeoverTime -= solution.getInstance().changeoverTime[afterMove[pEnd - 1].getPartId()][afterMove[pEnd + 1].getPartId()];
            }
        } else {
            // pIni = 10 -> pEnd = 1
            // 0 1 2 3 4 5 6 7 8 9 10 11 12  --> 0 10 1 2 3 4 5 6 7 8 9 11 12
            changeoverTime -= solution.getInstance().changeoverTime[afterMove[pIni].getPartId()][aux.getPartId()];
            if (pIni < (afterMove.length - 1)) {
                changeoverTime -= solution.getInstance().changeoverTime[aux.getPartId()][afterMove[pIni + 1].getPartId()];
                changeoverTime += solution.getInstance().changeoverTime[afterMove[pIni].getPartId()][afterMove[pIni + 1].getPartId()];
            }
            changeoverTime += solution.getInstance().changeoverTime[aux.getPartId()][afterMove[pEnd + 1].getPartId()];
            if (pEnd > 0) {
                changeoverTime += solution.getInstance().changeoverTime[afterMove[pEnd - 1].getPartId()][aux.getPartId()];
                changeoverTime -= solution.getInstance().changeoverTime[afterMove[pEnd - 1].getPartId()][afterMove[pEnd + 1].getPartId()];
            }
        }

        // New weekly produced parts for the move: deal with pointers! and don't use a function
        // Accumulate using the other machines' weekly production
        for (int machine = 0; machine < solution.getInstance().getNumMachines(); machine++)
            if (machine == mIni) {
                newWeeklyProducedParts[machine] = machineIniWeeklyProducedParts;
            } else {
                newWeeklyProducedParts[machine] = solution.getMachineWeeklyProducedParts()[machine];
            }


        // Calculate score
        var weeklyProductShortage = new int[solution.getInstance().getNumPeriods()][solution.getInstance().getNumParts()];
        var newWeeklyShortage = solution.fullCalculateWeeklyShortage(newWeeklyProducedParts, weeklyProductShortage);
        double shortage = solution.calculateShortageFunction(newWeeklyShortage);
        double newScore = solution.scoreCalculation(changeoverTime, shortage);

        double moveValue = newScore - solution.getScore();
        return new EfficientInsertMove(solution, mIni, pIni, mIni, pEnd, moveValue, machineIniWeeklyProducedParts, machineEndWeeklyProducedParts, changeoverTime, shortage);
    }


    private EfficientInsertMove generateMoveDifferentMachine(CLSPSolution solution, int mIni, int mEnd, int pIni, int pEnd) {

        // Machine weekly produced parts for the move (different machines)
        int[][] machineIniWeeklyProducedParts = new int[solution.getInstance().getNumPeriods()][solution.getInstance().getNumParts()];
        int[][] machineEndWeeklyProducedParts = new int[solution.getInstance().getNumPeriods()][solution.getInstance().getNumParts()];
        int[][][] newWeeklyProducedParts = new int[solution.getInstance().getNumMachines()][solution.getInstance().getNumPeriods()][solution.getInstance().getNumParts()];

        double changeoverTime = solution.getChangeoverTime();

        // New workslot sequences copying at the same time as moving
        WorkSlot aux = solution.getSolutionData()[mIni][pIni];

        WorkSlot[] afterMoveIni = new WorkSlot[solution.getNumberOfMachineWorkSlots()[mIni] - 1];
        int index = 0;
        for (int i = 0; i < solution.getNumberOfMachineWorkSlots()[mIni]; i++) {
            if (i != pIni) {
                afterMoveIni[index] = solution.getSolutionData()[mIni][i];
                index++;
            }
        }
        WorkSlot[] afterMoveEnd = new WorkSlot[solution.getNumberOfMachineWorkSlots()[mEnd] + 1];
        index = 0;
        for (int i = 0; i < solution.getNumberOfMachineWorkSlots()[mEnd]; i++) {
            if (i == pEnd) {
                afterMoveEnd[index] = aux;
                index++;
            }
            afterMoveEnd[index] = solution.getSolutionData()[mEnd][i];
            index++;
        }
        // Special case where the workslot is moved to the end of the machine
        if (pEnd == solution.getNumberOfMachineWorkSlots()[mEnd]) {
            afterMoveEnd[index] = aux;
        }

        // Calculate this machine's weekly production --> Use machineIniWeeklyProducedParts
        CLSPSolution.calculateMachineWeeklyProducedParts(solution.getInstance(), machineIniWeeklyProducedParts, afterMoveIni, afterMoveIni.length, mIni);
        CLSPSolution.calculateMachineWeeklyProducedParts(solution.getInstance(), machineEndWeeklyProducedParts, afterMoveEnd, afterMoveEnd.length, mEnd);

        // Calculates new changeover time
        if (pIni > 0)
            changeoverTime -= solution.getInstance().changeoverTime[afterMoveIni[pIni - 1].getPartId()][aux.getPartId()];
        if (pIni < afterMoveIni.length)
            changeoverTime -= solution.getInstance().changeoverTime[aux.getPartId()][afterMoveIni[pIni].getPartId()];
        if (pIni > 0 && pIni < afterMoveIni.length)
            changeoverTime += solution.getInstance().changeoverTime[afterMoveIni[pIni - 1].getPartId()][afterMoveIni[pIni].getPartId()];

        if (pEnd > 0)
            changeoverTime += solution.getInstance().changeoverTime[afterMoveEnd[pEnd - 1].getPartId()][aux.getPartId()];
        if (pEnd < (afterMoveEnd.length - 1))
            changeoverTime += solution.getInstance().changeoverTime[aux.getPartId()][afterMoveEnd[pEnd + 1].getPartId()];
        if (pEnd > 0 && pEnd < (afterMoveEnd.length - 1))
            changeoverTime -= solution.getInstance().changeoverTime[afterMoveEnd[pEnd - 1].getPartId()][afterMoveEnd[pEnd + 1].getPartId()];


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
        var newWeeklyShortage = solution.fullCalculateWeeklyShortage(newWeeklyProducedParts, weeklyProductShortage);
        double shortage = solution.calculateShortageFunction(newWeeklyShortage);
        double newScore = solution.scoreCalculation(changeoverTime, shortage);

        double moveValue = newScore - solution.getScore();
        return new EfficientInsertMove(solution, mIni, pIni, mEnd, pEnd, moveValue, machineIniWeeklyProducedParts, machineEndWeeklyProducedParts, changeoverTime, shortage);

    }


    public ExploreResult<InsertMove, CLSPSolution, CLSPInstance> explore(CLSPSolution solution) {
        List<InsertMove> list = new ArrayList<>();

        for (int machineIni = 0; machineIni < solution.getInstance().getNumMachines(); machineIni++) {
            for (int posIni = 0; posIni < solution.getNumberOfMachineWorkSlots()[machineIni]; posIni++) {
                // Moves can be made in the same machine.
                for (int machineEnd = 0; machineEnd < solution.getInstance().getNumMachines(); machineEnd++) {
                    // Insertion must allow to include a workslot at the end of the machine.
                    for (int posEnd = 0; posEnd <= solution.getNumberOfMachineWorkSlots()[machineEnd]; posEnd++) {
                        // Moves in different places and also not after the workslots of the same machine.
                        if ((machineIni == machineEnd) && ((posIni == posEnd) || (posEnd == solution.getNumberOfMachineWorkSlots()[machineEnd]))) {
                            continue;
                        }
                        // If the destination machine is not able to produce the part, the move is not possible.
                        if (solution.getInstance().productionRate[solution.getSolutionData()[machineIni][posIni].getPartId()][machineEnd] == 0) {
                            continue;
                        }
                        EfficientInsertMove move;
                        if (machineIni == machineEnd) {
                            move = generateMoveSameMachine(solution, machineIni, posIni, posEnd);
                        } else {
                            move = generateMoveDifferentMachine(solution, machineIni, machineEnd, posIni, posEnd);
                        }
                        // Only store improving moves
                        if (move.getMoveValue() < 0)
                            list.add(move);
                    }
                }
            }
        }

        return ExploreResult.fromList(list);
    }

}
