package es.urjc.etsii.grafo.CLSP.model.neighborhoods;

import es.urjc.etsii.grafo.CLSP.model.CLSPInstance;
import es.urjc.etsii.grafo.CLSP.model.CLSPSolution;
import es.urjc.etsii.grafo.CLSP.model.WorkSlot;
import es.urjc.etsii.grafo.CLSP.model.moves.EfficientInsertMove;
import es.urjc.etsii.grafo.CLSP.model.moves.EfficientSwapMove;
import es.urjc.etsii.grafo.CLSP.model.moves.InsertMove;
import es.urjc.etsii.grafo.solution.neighborhood.ExploreResult;

import java.util.ArrayList;
import java.util.List;

public class EfficientInsertBySwapNeighborhood extends EfficientInsertNeighborhood {

    private static final int LEFT = 0;
    private static final int RIGHT = 1;


    public ExploreResult<InsertMove, CLSPSolution, CLSPInstance> explore(CLSPSolution solution) {

        List<InsertMove> list = new ArrayList<>();

        // For each element in the solution, generate moves by swapping it with the next element

        // Consider one machine:
        double moveValue;

        for (int machineIni = 0; machineIni < solution.getInstance().getNumMachines(); machineIni++) {

            for (int pIni = 0; pIni < solution.getNumberOfMachineWorkSlots()[machineIni]; pIni++) {

                double changeoverTime = solution.getChangeoverTime();
                double shortage = solution.getShortage();

                // Auxiliary data structures
                int[][][] newMachineWeeklyProducedParts = new int[solution.getInstance().getNumMachines()][solution.getInstance().getNumPeriods()][solution.getInstance().getNumParts()];
                // Copy of the worklots of the machine
                WorkSlot[] workslotSequence = new WorkSlot[solution.getNumberOfMachineWorkSlots()[machineIni]];
                prepareDataStructures(solution, workslotSequence, machineIni, newMachineWeeklyProducedParts);

                // Calculate weekly produced parts for the current situation in newMachineWeeklyProducedParts
                int [][] weeklyProducedParts = new int[solution.getInstance().getNumPeriods()][solution.getInstance().getNumParts()];
                CLSPSolution.calculateWeeklyProducedParts(newMachineWeeklyProducedParts, weeklyProducedParts,solution.getInstance());

                // Goes to the LEFT
                for (int pEnd = pIni; pEnd > 0; pEnd--) {

                    // Always compute the move that swaps pEnd-1 with pEnd

                    // Calculate move value: fill in newWeeklyProducedParts
                    EfficientSwapMove move = createSwapMoveFromAdjacentWorkslots(solution, workslotSequence, pIni, pEnd, changeoverTime, machineIni, machineIni, shortage, newMachineWeeklyProducedParts[machineIni], newMachineWeeklyProducedParts[machineIni], weeklyProducedParts, LEFT);
                    if (move != null) {
                        list.add(move);

                        // Update current data
                        changeoverTime = move.getChangeoverTime();
                        shortage = move.getShortage();
                    }

                }

                // Auxiliary data structures
                newMachineWeeklyProducedParts = new int[solution.getInstance().getNumMachines()][solution.getInstance().getNumPeriods()][solution.getInstance().getNumParts()];
                changeoverTime = solution.getChangeoverTime();
                shortage = solution.getShortage();
                prepareDataStructures(solution, workslotSequence, machineIni, newMachineWeeklyProducedParts);
                weeklyProducedParts = new int[solution.getInstance().getNumPeriods()][solution.getInstance().getNumParts()];
                CLSPSolution.calculateWeeklyProducedParts(newMachineWeeklyProducedParts, weeklyProducedParts,solution.getInstance());

                // Goes to the RIGHT
                for (int pEnd = pIni + 1; pEnd < workslotSequence.length; pEnd++) {

                    // Always compute the move that swaps pEnd-1 with pEnd

                    // Calculate move value: fill in newWeeklyProducedParts
                    EfficientSwapMove move = createSwapMoveFromAdjacentWorkslots(solution, workslotSequence, pIni, pEnd, changeoverTime, machineIni, machineIni, shortage, newMachineWeeklyProducedParts[machineIni], newMachineWeeklyProducedParts[machineIni], weeklyProducedParts, RIGHT);
                    if (move != null) {
                        list.add(move);

                        // Update current data
                        changeoverTime = move.getChangeoverTime();
                        shortage = move.getShortage();
                    }

                }

                if (solution.getInstance().getNumMachines() == 1) continue;

                // Update last workslot timing before removing it --> It is not necessary to update the last workslot because the last right move did not change it
                CLSPSolution.updateIndividualWorkSlotTiming(workslotSequence.length-1,workslotSequence, machineIni, solution.getInstance());

                // Extract the last workslot from the machine and add it to the other machines.
                // It is located at the end of the sequence
                WorkSlot workslot = new WorkSlot(workslotSequence[workslotSequence.length-1]);

                // Remove its production from the initial machine starting from the initial period of the workslot
                double machineCapacity = solution.getInstance().getMachineCapacity(machineIni, workslot.getIniPeriod());
                double iniTime = workslot.getIniTime();
                double endTime = workslot.getIniTime() + workslot.getDuration();
                int currPeriod = workslot.getIniPeriod();
                if (endTime >= machineCapacity) {
                    int production = (int) Math.ceil((machineCapacity-iniTime) * solution.getInstance().productionRate[workslot.getPartId()][machineIni]);
                    if (currPeriod < solution.getInstance().getNumPeriods()) {
                        newMachineWeeklyProducedParts[machineIni][currPeriod][workslot.getPartId()] -= production;
                        if (newMachineWeeklyProducedParts[machineIni][currPeriod][workslot.getPartId()] < 0) {
                            if (newMachineWeeklyProducedParts[machineIni][currPeriod][workslot.getPartId()] == -1)
                                newMachineWeeklyProducedParts[machineIni][currPeriod][workslot.getPartId()] = 0;  // Correct rounding problem
                            else
                                throw new RuntimeException("Error calculating produced parts: negative value.");
                        }
                    }
                    endTime -= machineCapacity;
                    currPeriod++;
                    for (int i = currPeriod; i < solution.getInstance().getNumPeriods(); i++) {
                        double duration = Math.min(endTime, machineCapacity);
                        production += (int) Math.ceil(duration * solution.getInstance().productionRate[workslot.getPartId()][machineIni]);
                        if (i < solution.getInstance().getNumPeriods()) {
                            newMachineWeeklyProducedParts[machineIni][i][workslot.getPartId()] -= production;
                            if (newMachineWeeklyProducedParts[machineIni][currPeriod][workslot.getPartId()] < 0) {
                                if (newMachineWeeklyProducedParts[machineIni][currPeriod][workslot.getPartId()] == -1)
                                    newMachineWeeklyProducedParts[machineIni][currPeriod][workslot.getPartId()] = 0;  // Correct rounding problem
                                else
                                   throw new RuntimeException("Error calculating produced parts: negative value.");
                            }
                        }
                        endTime -= duration;
                    }
                } else {
                    for (int i = currPeriod; i < solution.getInstance().getNumPeriods(); i++) {
                        newMachineWeeklyProducedParts[machineIni][i][workslot.getPartId()] -= (int) Math.ceil(workslot.getDuration() * solution.getInstance().productionRate[workslot.getPartId()][machineIni]);
                        if (newMachineWeeklyProducedParts[machineIni][i][workslot.getPartId()] < 0) {
                            if (newMachineWeeklyProducedParts[machineIni][i][workslot.getPartId()] == -1)
                                newMachineWeeklyProducedParts[machineIni][i][workslot.getPartId()] = 0;  // Correct rounding problem
                            else
                                throw new RuntimeException("Error calculating produced parts: negative value.");
                        }
                    }
                }

// Assert debug
//checkMachineProducedParts("Update", newMachineWeeklyProducedParts);

                // Remove changeover time of the last workslot. Store the value after removing the workslot
                if (workslotSequence.length > 1)
                    changeoverTime -= solution.getInstance().changeoverTime[workslotSequence[workslotSequence.length-2].getPartId()][workslot.getPartId()];

                // Include the workslot in the other machines
                for (int machineEnd = 0; machineEnd < solution.getInstance().getNumMachines() ; machineEnd++) {
                    // If the destination machine is not able to produce the part, the move is not possible.
                    if ((machineEnd != machineIni) && (solution.getInstance().productionRate[workslot.getPartId()][machineEnd] > 0)) {

                        // Keep a copy of end machine to later recover
                        var machineEndWeeklyProducedParts = new int[solution.getInstance().getNumPeriods()][solution.getInstance().getNumParts()];
                        for (int i = 0; i < solution.getInstance().getNumPeriods(); i++)
                            if (solution.getInstance().getNumParts() >= 0)
                                System.arraycopy(newMachineWeeklyProducedParts[machineEnd][i], 0, machineEndWeeklyProducedParts[i], 0, solution.getInstance().getNumParts());

                        // Create new workslot sequence with the destination machine and the new workslot
                        workslotSequence = new WorkSlot[solution.getNumberOfMachineWorkSlots()[machineEnd]+1];
                        for (int i = 0; i < solution.getNumberOfMachineWorkSlots()[machineEnd]; i++) {
                            workslotSequence[i] = new WorkSlot(solution.getSolutionData()[machineEnd][i]);
                        }
                        // Add the slot as the last one of the other machine and update its timing
                        workslotSequence[workslotSequence.length-1] = workslot;
                        CLSPSolution.updateIndividualWorkSlotTiming(workslotSequence.length-1, workslotSequence, machineEnd, solution.getInstance());

                        // Calculate new changeover time
                        double newChangeoverTime = changeoverTime + solution.getInstance().changeoverTime[workslotSequence[workslotSequence.length-2].getPartId()][workslot.getPartId()];

                        // Update newWeeklyProducedParts for the end machine
                        machineCapacity = solution.getInstance().getMachineCapacity(machineEnd, workslot.getIniPeriod());
                        iniTime = workslot.getIniTime();
                        endTime = workslot.getIniTime() + workslot.getDuration();
                        currPeriod = workslot.getIniPeriod();
                        if (endTime >= machineCapacity) {
                            int production = (int) Math.ceil((machineCapacity-iniTime) * solution.getInstance().productionRate[workslot.getPartId()][machineEnd]);
                            if (currPeriod < solution.getInstance().getNumPeriods()) {
                                newMachineWeeklyProducedParts[machineEnd][currPeriod][workslot.getPartId()] += production;
                            }
                            endTime -= machineCapacity;
                            currPeriod++;
                            for (int i = currPeriod; i < solution.getInstance().getNumPeriods(); i++) {
                                // Calculate the remaining production in the following period:
                                double duration = Math.min(endTime, machineCapacity);
                                // Accumulate production and sum it to the new weekly produced parts
                                production += (int) Math.ceil(duration * solution.getInstance().productionRate[workslot.getPartId()][machineEnd]);
                                if (i < solution.getInstance().getNumPeriods())
                                    newMachineWeeklyProducedParts[machineEnd][i][workslot.getPartId()] += production;
                                endTime -= duration;
                            }
                        } else {
                            for (int i = currPeriod; i < solution.getInstance().getNumPeriods(); i++)
                                newMachineWeeklyProducedParts[machineEnd][i][workslot.getPartId()] += (int) Math.ceil(workslot.getDuration() * solution.getInstance().productionRate[workslot.getPartId()][machineEnd]);
                        }

                        // Calculate score
                        var newWeeklyProductShortage = new int[solution.getInstance().getNumPeriods()][solution.getInstance().getNumParts()];
                        var newWeeklyShortage = solution.fullCalculateWeeklyShortage(newMachineWeeklyProducedParts,newWeeklyProductShortage);
                        shortage = solution.calculateShortageFunction(newWeeklyShortage);
                        double newScore = solution.scoreCalculation(newChangeoverTime, shortage);

                        moveValue = newScore - solution.getScore();

                        // Create the move
                        EfficientInsertMove newMove = new EfficientInsertMove(solution, machineIni, pIni, machineEnd, solution.getNumberOfMachineWorkSlots()[machineEnd], moveValue, newMachineWeeklyProducedParts[machineIni], newMachineWeeklyProducedParts[machineEnd], newChangeoverTime, shortage);
                        list.add(newMove);
/*
// Assert debug
checkMachineProducedParts("Insert NEW MACHINE", newMachineWeeklyProducedParts);
var debugWeeklyProducedParts = new int[solution.getInstance().getNumPeriods()][solution.getInstance().getNumParts()];
CLSPSolution.calculateWeeklyProducedParts(newMachineWeeklyProducedParts, debugWeeklyProducedParts, solution.getInstance());
var debugNewWeeklyProductShortage = new int[solution.getInstance().getNumPeriods()][solution.getInstance().getNumParts()];
var debugNewWeeklyShortage = solution.fullCalculateWeeklyShortage(newMachineWeeklyProducedParts,debugNewWeeklyProductShortage);

CLSPSolution clone = new CLSPSolution(solution);
newMove.execute(clone);
int diff = (int) Math.abs(clone.getScore() - clone.recalculateScore());
if (diff != 0) {
    System.out.println("ERROR insert NEW MACHINE");
    if (diff > 2) {
        throw new RuntimeException("ERROR insert move NEW MACHINE");
    }
}

 */

                        // Calculate weekly produced parts for the current situation in newMachineWeeklyProducedParts
                        weeklyProducedParts = new int[solution.getInstance().getNumPeriods()][solution.getInstance().getNumParts()];
                        CLSPSolution.calculateWeeklyProducedParts(newMachineWeeklyProducedParts, weeklyProducedParts,solution.getInstance());

                        // Move slot to the LEFT
                        for (int pEnd = workslotSequence.length-1; pEnd > 0; pEnd--) {

                            // Always compute the move that swaps pEnd-1 with pEnd

                            // Calculate move value: fill in newWeeklyProducedParts
                            EfficientSwapMove move = createSwapMoveFromAdjacentWorkslots(solution, workslotSequence, pIni, pEnd, newChangeoverTime, machineIni, machineEnd, shortage, newMachineWeeklyProducedParts[machineIni], newMachineWeeklyProducedParts[machineEnd], weeklyProducedParts, LEFT);
                            if (move != null) {
                                list.add(move);

                                // Update current data
                                newChangeoverTime = move.getChangeoverTime();
                                shortage = move.getShortage();
                            }

                        }

                        // Recover machineEndWeeklyProducedParts
                        for (int i = 0; i < solution.getInstance().getNumPeriods(); i++)
                            if (solution.getInstance().getNumParts() >= 0)
                                System.arraycopy(machineEndWeeklyProducedParts[i], 0, newMachineWeeklyProducedParts[machineEnd][i], 0, solution.getInstance().getNumParts());

                    }
                }

            }

        }


        return ExploreResult.fromList(list);
    }

//    private void checkMachineProducedParts(String desc, int[][][] newMachineWeeklyProducedParts) {
//        for (int machine = 0; machine < newMachineWeeklyProducedParts.length; machine++) {
//            for (int period = 0; period < newMachineWeeklyProducedParts[machine].length; period++) {
//                for (int part = 0; part < newMachineWeeklyProducedParts[machine][period].length; part++) {
//                    if (period > 0) {
//                        if (newMachineWeeklyProducedParts[machine][period][part] < newMachineWeeklyProducedParts[machine][period-1][part]) {
//                            throw new RuntimeException("New produced parts calculation error (former period smaller than previous) in " + desc + " machine " + machine + ", period " + period + ", part " + part);
//                        }
//                    }
//                }
//            }
//        }
//    }


    /**
     * Creates a copy of the workslots of the solution, gets the machine weekly produced parts and calculates the
     * weekly produced parts for all the machines given the current solution.
     * @param solution Current solution
     * @param workslotSequence Given worklot sequence
     * @param machine Current machine of the current solution
     * @param newWeeklyProducedParts New weekly produced parts, updated only in the current machine
     */
    private void prepareDataStructures(CLSPSolution solution, WorkSlot[] workslotSequence, int machine, int[][][] newWeeklyProducedParts) {
        // Copy worklots of the machine to a new variable
        for (int i = 0; i < solution.getNumberOfMachineWorkSlots()[machine]; i++) {
            workslotSequence[i] = new WorkSlot(solution.getSolutionData()[machine][i]);
        }

        // Machine weekly produced parts for the move
        // Calculate this machine's weekly production --> Use machineIniWeeklyProducedParts
        int[][] machineIniWeeklyProducedParts = new int[solution.getInstance().getNumPeriods()][solution.getInstance().getNumParts()];
        CLSPSolution.calculateMachineWeeklyProducedParts(solution.getInstance(), machineIniWeeklyProducedParts, workslotSequence, workslotSequence.length, machine);
        // And the new weekly production
        for (int m = 0; m < solution.getInstance().getNumMachines(); m++)
            for (int i = 0; i < solution.getInstance().getNumPeriods(); i++)
                for (int j = 0; j < solution.getInstance().getNumParts(); j++)
                    if (m == machine)
                        newWeeklyProducedParts[machine][i][j] = machineIniWeeklyProducedParts[i][j];
                    else
                        newWeeklyProducedParts[m][i][j] = solution.getMachineWeeklyProducedParts()[m][i][j];
    }


    /* *
     * Creates new move when swapping two adjacent workslots: rightIdx-1 and rightIdx
     *
     * @param solution               Current solution
     * @param workslotSequence       Workslot sequence of the machine, which will be modified
     * @param pIni                   Needed to create the move
     * @param rightIdx               Index of the right workslot
     * @param changeoverTime         Changeover time of the current solution
     * @param machineIni             Machine of the current solution
     * @param newWeeklyProducedParts New weekly produced parts, updated only in the current machine
     * @param shortage
     * @return new EfficientInsertMove
     * /
    private EfficientInsertMove createMoveFromAdjacentWorkslots(CLSPSolution solution, WorkSlot[] workslotSequence, int pIni, int rightIdx, double changeoverTime, int machineIni, int machineEnd, int[][][] newWeeklyProducedParts, double shortage) {

        // Changeover time change
        if (rightIdx > 1) {
            changeoverTime -= solution.getInstance().changeoverTime[workslotSequence[rightIdx-2].getPartId(),workslotSequence[rightIdx-1].getPartId());
            changeoverTime += solution.getInstance().changeoverTime[workslotSequence[rightIdx-2].getPartId(),workslotSequence[rightIdx].getPartId());
        }

        if (rightIdx < (workslotSequence.length - 1)) {
            changeoverTime -= solution.getInstance().changeoverTime[workslotSequence[rightIdx].getPartId(),workslotSequence[rightIdx+1].getPartId());
            if (rightIdx > 0)
                changeoverTime += solution.getInstance().changeoverTime[workslotSequence[rightIdx-1].getPartId(),workslotSequence[rightIdx+1].getPartId());
        }

        // Perform insert move in copied structure
        var aux = workslotSequence[rightIdx];
        workslotSequence[rightIdx] = workslotSequence[rightIdx-1];
        workslotSequence[rightIdx-1] = aux;

        // Update timing of the workslots
        CLSPSolution.updateIndividualWorkSlotTiming(rightIdx-1,workslotSequence, machineIni, solution.getInstance());
        CLSPSolution.updateIndividualWorkSlotTiming(rightIdx,workslotSequence, machineIni, solution.getInstance());

        // Machine weekly produced parts for the move
        var machineIniWeeklyProducedParts = new int[solution.getInstance().getNumPeriods()][solution.getInstance().getNumParts()];
        // Calculate this machine's weekly production --> Use machineIniWeeklyProducedParts
        CLSPSolution.calculateMachineWeeklyProducedParts(solution.getInstance(), machineIniWeeklyProducedParts, workslotSequence, workslotSequence.length, machineIni);
        // And the new weekly production
        for (int i = 0; i < solution.getInstance().getNumPeriods(); i++)
            for (int j = 0; j < solution.getInstance().getNumParts(); j++)
                newWeeklyProducedParts[machineIni][i][j] = machineIniWeeklyProducedParts[i][j];
        // Calculate shortage
        var weeklyProductShortage = new int[solution.getInstance().getNumPeriods()][solution.getInstance().getNumParts()];
        var newWeeklyShortage = solution.fullCalculateWeeklyShortage(newWeeklyProducedParts, weeklyProductShortage);
        shortage = solution.calculateShortageFunction(newWeeklyShortage);

        // Calculate score
        double newScore = solution.scoreCalculation(changeoverTime, shortage);

        double moveValue = newScore - solution.getScore();

        EfficientInsertMove move;
        String direction;
        if (rightIdx > pIni) {
            // Right direction move
            direction = "Right";
            move = new EfficientInsertMove(solution, machineIni, pIni, machineEnd, rightIdx, moveValue, newWeeklyProducedParts, changeoverTime, shortage);
        } else {
            // Left direction move
            direction = "Left";
            move = new EfficientInsertMove(solution, machineIni, pIni, machineEnd, rightIdx-1, moveValue, newWeeklyProducedParts, changeoverTime, shortage);
        }

// Assert debug
//    checkMachineProducedParts("Insert LEFT", newWeeklyProducedParts);
//    CLSPSolution clone = new CLSPSolution(solution);
//    move.execute(clone);
//    int diff = (int) Math.abs(clone.getScore() - clone.recalculateScore());
//    if (diff != 0) {
//        throw new RuntimeException("ERROR insert move "+direction);
//    }

return move;

    }
*/


    /**
     * Creates new move when swapping two adjacent workslots: rightIdx-1 and rightIdx
     *
     * @param solution                      Current solution
     * @param workslotSequence              Workslot sequence of the machine, which will be modified
     * @param pIni                          Needed to create the move
     * @param rightIdx                      Index of the right workslot
     * @param changeoverTime                Changeover time of the current solution
     * @param machineIni                    Machine of the current solution
     * @param shortage                    Shortage of the current solution
     * @param machineEndWeeklyProducedParts Machine weekly produced parts of the end machine
     * @param weeklyProducedParts          Weekly produced parts of the current solution
     * @return new EfficientInsertMove
     */
    private EfficientSwapMove createSwapMoveFromAdjacentWorkslots(CLSPSolution solution, WorkSlot[] workslotSequence, int pIni, int rightIdx, double changeoverTime, int machineIni, int machineEnd, double shortage, int[][] machineIniWeeklyProducedParts, int[][] machineEndWeeklyProducedParts, int[][] weeklyProducedParts, int direction) {

        // Check if the move has any effect:
        if ((workslotSequence[rightIdx].getPartId() == workslotSequence[rightIdx-1].getPartId()) &&
            ((workslotSequence[rightIdx].getDuration() == workslotSequence[rightIdx-1].getDuration()))) {
            // No effect
            return null;
        }

        // Changeover time change
        if (rightIdx > 1) {
            changeoverTime -= solution.getInstance().changeoverTime[workslotSequence[rightIdx-2].getPartId()][workslotSequence[rightIdx-1].getPartId()];
            changeoverTime += solution.getInstance().changeoverTime[workslotSequence[rightIdx-2].getPartId()][workslotSequence[rightIdx].getPartId()];
        }

        if (rightIdx < (workslotSequence.length - 1)) {
            changeoverTime -= solution.getInstance().changeoverTime[workslotSequence[rightIdx].getPartId()][workslotSequence[rightIdx+1].getPartId()];
            changeoverTime += solution.getInstance().changeoverTime[workslotSequence[rightIdx-1].getPartId()][workslotSequence[rightIdx+1].getPartId()];
        }

        // Perform swap move in copied structure
        var aux = workslotSequence[rightIdx];
        workslotSequence[rightIdx] = workslotSequence[rightIdx-1];
        workslotSequence[rightIdx-1] = aux;

//        // Check if the changeover time has changed
//        if (changeoverTime == prevChangeoverTime) {
//            // Only update timing of the two workslots
//            CLSPSolution.updateIndividualWorkSlotTiming(rightIdx - 1, workslotSequence, machineEnd, solution.getInstance());
//            CLSPSolution.updateIndividualWorkSlotTiming(rightIdx, workslotSequence, machineEnd, solution.getInstance());
//            // Check if the move has any effect:
//            if ((workslotSequence[rightIdx].getIniPeriod() == workslotSequence[rightIdx-1].getEndPeriod())) {
//                // Same changeover time and same period --> No effect
//                return null;
//            }
//        } else {
//            // A full update of the workslot sequence is needed starting from the left workslot
//            for (int i = rightIdx - 1; i< workslotSequence.length; i++) {
//                CLSPSolution.updateIndividualWorkSlotTiming(i, workslotSequence, machineEnd, solution.getInstance());
//            }
//        }

        // A full update of the workslot sequence is needed starting from the left workslot
        for (int i = rightIdx - 1; i< workslotSequence.length; i++) {
            CLSPSolution.updateIndividualWorkSlotTiming(i, workslotSequence, machineEnd, solution.getInstance());
        }

        // Machine weekly produced parts for the move
        var newMachineWeeklyProducedParts = new int[solution.getInstance().getNumPeriods()][solution.getInstance().getNumParts()];
        // Calculate this machine's weekly production --> fills in  newMachineWeeklyProducedParts
        CLSPSolution.calculateMachineWeeklyProducedParts(solution.getInstance(), newMachineWeeklyProducedParts, workslotSequence, workslotSequence.length, machineEnd);

        // Calculate differences and shortage
        for (int period = 0; period < solution.getInstance().getNumPeriods(); period++)
            for (int part = 0; part < solution.getInstance().getNumParts(); part++) {
                if ((newMachineWeeklyProducedParts[period][part] - machineEndWeeklyProducedParts[period][part]) != 0) {
                    // Changes happened
                    // Remove previous difference
                    int prevDifference = weeklyProducedParts[period][part] + solution.getInstance().inventory[part][period];
                    if (prevDifference < 0)
                        shortage += prevDifference;  // Adding a negative number is the same as subtracting it
                    // Update production
                    weeklyProducedParts[period][part] += newMachineWeeklyProducedParts[period][part] - machineEndWeeklyProducedParts[period][part];
                    int newDifference = weeklyProducedParts[period][part] + solution.getInstance().inventory[part][period];
                    if (newDifference < 0)
                        shortage += (-1) * newDifference;  // Adding a negative number is the same as subtracting it
                }
            }

        // Update machineEndWeeklyProducedParts with newMachineWeeklyProducedParts
        for (int period = 0; period < solution.getInstance().getNumPeriods(); period++)
            if (solution.getInstance().getNumParts() >= 0)
                System.arraycopy(newMachineWeeklyProducedParts[period], 0, machineEndWeeklyProducedParts[period], 0, solution.getInstance().getNumParts());


        // Calculate score
        double newScore = solution.scoreCalculation(changeoverTime, shortage);

        double moveValue = newScore - solution.getScore();

        // Always left direction move
        EfficientSwapMove move;
        if (direction == RIGHT) {
            // Right direction move
            move = new EfficientSwapMove(solution, machineIni, pIni, machineEnd, rightIdx, moveValue, changeoverTime, shortage, machineIniWeeklyProducedParts, machineEndWeeklyProducedParts);
        } else {
            // Left direction move
            move = new EfficientSwapMove(solution, machineIni, pIni, machineEnd, rightIdx-1, moveValue, changeoverTime, shortage, machineIniWeeklyProducedParts, machineEndWeeklyProducedParts);
        }

//// Assert debug
//CLSPSolution clone = new CLSPSolution(solution);
//move.execute(clone);
//double dif = Math.abs(clone.getScore() - clone.recalculateScore());
//if (dif > 1) {
//    System.out.println("ERROR swap move");
//    clone.recalculateScore();
//}

        return move;

    }

}


