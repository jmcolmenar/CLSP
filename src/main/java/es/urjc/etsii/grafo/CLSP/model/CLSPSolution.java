package es.urjc.etsii.grafo.CLSP.model;

import es.urjc.etsii.grafo.solution.Solution;
import es.urjc.etsii.grafo.util.DoubleComparator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class CLSPSolution extends Solution<CLSPSolution, CLSPInstance> {

    //private static final Logger log = LoggerFactory.getLogger(CLSPSolution.class);

    // For each machine, we store the work slots sequentially.
    private WorkSlot[][] solutionData;

    // Current number of work slots for each machine
    private int [] numberOfMachineWorkSlots;

    // For each machine, period and part, we store the number of produced parts.
    private int [][][] machineWeeklyProducedParts;

    private double score;

    private List<WorkSlot> unassignedWorkSlots;

    private double changeoverTime;
    private double shortage;
    private double shortageGurobi;
    private double priorityGurobi;

    public double getShortage() {
        return shortage;
    }

    public void setShortage(double shortage) {
        this.shortage = shortage;
    }

    public double getShortageGurobi() {
        return shortageGurobi;
    }

    public void setShortageGurobi(double shortageGurobi) {
        this.shortageGurobi = shortageGurobi;
    }

    public double getPriorityGurobi() {
        return priorityGurobi;
    }

    public void setPriorityGurobi(double priorityGurobi) {
        this.priorityGurobi = priorityGurobi;
    }

// Getters. Some of them to automatically export solution data to JSON

    public WorkSlot[][] getSolutionData() {
        return solutionData;
    }

    public int[] getNumberOfMachineWorkSlots() {
        return numberOfMachineWorkSlots;
    }

    public int[][][] getMachineWeeklyProducedParts() {
        return machineWeeklyProducedParts;
    }


    public List<WorkSlot> getUnassignedWorkSlots() {
        return unassignedWorkSlots;
    }

    public double getChangeoverTime() {
        return changeoverTime;
    }

    public void setChangeoverTime(double changeoverTime) {
        this.changeoverTime = changeoverTime;
    }

    /**
     * Initialize solution from instance
     *
     * @param ins Instance of the problem
     */
    public CLSPSolution(CLSPInstance ins) {
        super(ins);
        solutionData = new WorkSlot[ins.getNumMachines()][ins.maximumWorkSlots()];
        numberOfMachineWorkSlots = new int[ins.getNumMachines()];
        for (int i = 0; i < ins.getNumMachines(); i++) {
            numberOfMachineWorkSlots[i] = 0;
        }
        machineWeeklyProducedParts = new int[this.getInstance().getNumMachines()][this.getInstance().getNumPeriods()][this.getInstance().getNumParts()];
        score = Double.MAX_VALUE;
        unassignedWorkSlots = ins.getCopyOfSlotsNoShortage();
        changeoverTime = 0;
        shortage = Double.MAX_VALUE;
        shortageGurobi = Double.MAX_VALUE;
        priorityGurobi = Double.MAX_VALUE;
    }

    /**
     * Clone constructor
     *
     * @param s Solution to clone
     */
    public CLSPSolution(CLSPSolution s) {
        super(s);
        solutionData = new WorkSlot[this.getInstance().getNumMachines()][this.getInstance().maximumWorkSlots()];
        for (int i = 0; i < this.getInstance().getNumMachines(); i++) {
            for (int j = 0; j < s.numberOfMachineWorkSlots[i]; j++) {
                solutionData[i][j] = new WorkSlot(s.solutionData[i][j]);
            }
        }
        numberOfMachineWorkSlots = new int[this.getInstance().getNumMachines()];
        if (this.getInstance().getNumMachines() >= 0)
            System.arraycopy(s.numberOfMachineWorkSlots, 0, numberOfMachineWorkSlots, 0, this.getInstance().getNumMachines());
        machineWeeklyProducedParts = new int[this.getInstance().getNumMachines()][this.getInstance().getNumPeriods()][this.getInstance().getNumParts()];
        for (int i = 0; i < this.getInstance().getNumMachines(); i++) {
            for (int j = 0; j < this.getInstance().getNumPeriods(); j++) {
                if (this.getInstance().getNumParts() >= 0)
                    System.arraycopy(s.machineWeeklyProducedParts[i][j], 0, machineWeeklyProducedParts[i][j], 0, this.getInstance().getNumParts());
            }
        }
        score = s.score;
        unassignedWorkSlots = new ArrayList<>();
        for (WorkSlot ws : s.unassignedWorkSlots) {
            unassignedWorkSlots.add(new WorkSlot(ws));
        }
        changeoverTime = s.changeoverTime;
        shortage = s.shortage;
        shortageGurobi = s.shortageGurobi;
        priorityGurobi = s.priorityGurobi;
    }


    @Override
    public CLSPSolution cloneSolution() {
        // You do not need to modify this method
        // Call clone constructor
        return new CLSPSolution(this);
    }

    @Override
    protected boolean _isBetterThan(CLSPSolution other) {
        return DoubleComparator.isLess(this.getScore(), other.getScore());
    }

    /**
     * Get the current solution score.
     * The difference between this method and recalculateScore is that
     * this result can be a property of the solution, or cached,
     * it does not have to be calculated each time this method is called
     *
     * @return current solution score as double
     */
    @Override
    public double getScore() {
        return this.score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    /**
     * Recalculate solution score from scratch, using the problem objective function.
     * The difference between this method and getScore is that we must recalculate the score from scratch,
     * without using any cache/shortcuts.
     * This method will be used to validate the correct behaviour of the getScore() method, and to help catch
     * bugs or mistakes when changing incremental score calculation.
     * DO NOT UPDATE CACHES IN THIS METHOD / MAKE SURE THIS METHOD DOES NOT HAVE SIDE EFFECTS
     * DO NOT UPDATE CACHES IN THIS METHOD / MAKE SURE THIS METHOD DOES NOT HAVE SIDE EFFECTS
     * and once more
     * DO NOT UPDATE CACHES IN THIS METHOD / MAKE SURE THIS METHOD DOES NOT HAVE SIDE EFFECTS
     *
     * @return current solution score as double
     */
    @Override
    public double recalculateScore() {

        int [] totalProducedParts = new int[this.getInstance().getNumParts()];
        int [] lastInventory = new int[this.getInstance().getNumParts()];
        int [] difference = new int[this.getInstance().getNumParts()];
        int [][] weeklyProducedParts = new int[this.getInstance().getNumPeriods()][this.getInstance().getNumParts()];

        return scoreCalculation(fullChangeOverTimeCalculation(),fullShortageCalculation(totalProducedParts, lastInventory, difference,weeklyProducedParts));

    }

    /**
     * Calculates the score of the solution. This formula is implemented once in the code.
     *
     * @param totalChangeOverTime Previously calculated maximum total time
     * @param shortage Previously calculated shortage
     * @return score of the solution
     */
    public double scoreCalculation(double totalChangeOverTime, double shortage) {
        return totalChangeOverTime + shortage;
    }

    /**
     * Calculates the machine's weekly produced parts from the given workslot sequence.
     * @param instance Current instance
     * @param machineWeeklyProducedParts Matrix to store the weekly produced parts (period, part)
     * @param workslotSequence Workslot sequence
     * @param machineId Machine to calculate the weekly produced parts
     */
    public static void calculateMachineWeeklyProducedParts(CLSPInstance instance, int[][] machineWeeklyProducedParts, WorkSlot[] workslotSequence, int numWorkSlots, int machineId) {
        int period = 0;
        double currentPeriodDuration = 0;
        int machineCapacity = instance.getMachineCapacity(machineId,period);
        for (int j=0; j < numWorkSlots; j++) {
            // If there is a change of period, we account for it
            if ((currentPeriodDuration + workslotSequence[j].getDuration()) >= machineCapacity) {
                double hoursToComplete = machineCapacity - currentPeriodDuration;
                machineWeeklyProducedParts[period][workslotSequence[j].getPartId()] += (int) Math.ceil(hoursToComplete * instance.productionRate[workslotSequence[j].getPartId()][machineId]);
                currentPeriodDuration = currentPeriodDuration + workslotSequence[j].getDuration() - machineCapacity;
                period++;
                // If the sequence of work slots is longer than the number of periods, we skip the rest of the work slots
                if (period >= instance.getNumPeriods()) {
                    break;
                }
                machineCapacity = instance.getMachineCapacity(machineId, period);
                while (currentPeriodDuration >= machineCapacity) {
                    machineWeeklyProducedParts[period][workslotSequence[j].getPartId()] += (machineCapacity * instance.productionRate[workslotSequence[j].getPartId()][machineId]);
                    currentPeriodDuration -= machineCapacity;
                    period++;
                    if (period >= instance.getNumPeriods()) {
                        break;
                    }
                    machineCapacity = instance.getMachineCapacity(machineId, period);
                }
                if (period >= instance.getNumPeriods()) {
                    break;
                }
                machineWeeklyProducedParts[period][workslotSequence[j].getPartId()] += (int) Math.ceil(currentPeriodDuration * instance.productionRate[workslotSequence[j].getPartId()][machineId]);
                machineCapacity = instance.getMachineCapacity(machineId, period);
            } else {
                currentPeriodDuration += workslotSequence[j].getDuration();
                machineWeeklyProducedParts[period][workslotSequence[j].getPartId()] += (int) Math.ceil(workslotSequence[j].getDuration() * instance.productionRate[workslotSequence[j].getPartId()][machineId]);
            }
            // If a changeover is needed, we account for it
            if ((j < (numWorkSlots - 1)) &&
                    (workslotSequence[j].getPartId() != workslotSequence[j + 1].getPartId())) {
                int changeoverTime = instance.changeoverTime[workslotSequence[j].getPartId()][workslotSequence[j + 1].getPartId()];
                currentPeriodDuration += changeoverTime;
                // If changeover time reaches the end of the period, account for it in total time, and reset period
                if (currentPeriodDuration >= machineCapacity) {
                    currentPeriodDuration -= machineCapacity;
                    period++;
                    // If the sequence of work slots is longer than the number of periods, we skip the rest of the work slots
                    if (period >= instance.getNumPeriods()) {
                        break;
                    }
                }
            }
        }

        // Accumulates the obtained values (begins in the second period)
        for (int p = 1; p < instance.getNumPeriods(); p++) {
            for (int part = 0; part < instance.getNumParts(); part++) {
                machineWeeklyProducedParts[p][part] += machineWeeklyProducedParts[p-1][part];
            }
        }

    }

    /**
     * Calculates the shortage of the solution. It fills in the arrays that receive as parameters.
     * @param producedParts Produced parts to be filled in
     * @param lastInventory Last inventory to be filled in
     * @param difference Difference to be filled in
     * @param weeklyProducedParts Weekly produced parts to be filled in
     * @return shortage of the solution
     */
    protected double fullShortageCalculation(int [] producedParts, int [] lastInventory, int [] difference, int [][] weeklyProducedParts) {
        int [] weeklyShortage = new int[this.getInstance().getNumPeriods()];

        var newMachineWeeklyProducedParts = new int[this.getInstance().getNumMachines()][this.getInstance().getNumPeriods()][this.getInstance().getNumParts()];

        for (int machineId=0; machineId<this.getInstance().getNumMachines(); machineId++) {
            calculateMachineWeeklyProducedParts(this.getInstance(), newMachineWeeklyProducedParts[machineId], this.solutionData[machineId], this.numberOfMachineWorkSlots[machineId], machineId);
        }

        for (int period = 0; period < this.getInstance().getNumPeriods(); period++) {
            producedParts[period] = 0;
            for (int part = 0; part < this.getInstance().getNumParts(); part++) {
                weeklyProducedParts[period][part] = 0;
                for (int machineId=0; machineId<this.getInstance().getNumMachines(); machineId++) {
                    weeklyProducedParts[period][part] += newMachineWeeklyProducedParts[machineId][period][part];
                }
                producedParts[period] += weeklyProducedParts[period][part];
            }
        }

        // Shortage is calculated for each week
        for (int period=0; period<this.getInstance().getNumPeriods(); period++) {
            for (int i=0; i<this.getInstance().getNumParts(); i++) {
                lastInventory[i] = this.getInstance().inventory[i][period];
                difference[i] = weeklyProducedParts[period][i] + lastInventory[i];
                if (difference[i] < 0)
                    weeklyShortage[period] += (-1)*difference[i];
            }
        }

        return calculateShortageFunction(weeklyShortage);
    }


    protected double fullChangeOverTimeCalculation() {
        double totalChangeOverTime = 0;

        for (int machineId=0; machineId<this.getInstance().getNumMachines(); machineId++) {
            for (int j=0; j<this.numberOfMachineWorkSlots[machineId]; j++) {
                if ((j < (this.numberOfMachineWorkSlots[machineId] - 1)) &&
                        (this.solutionData[machineId][j].getPartId() != this.solutionData[machineId][j + 1].getPartId())) {
                    totalChangeOverTime += this.getInstance().changeoverTime[this.solutionData[machineId][j].getPartId()][this.solutionData[machineId][j + 1].getPartId()];
                }
            }
        }

        return totalChangeOverTime;
    }

/*
    protected double fullMaxTotalTimeCalculation() {
        double totalTime = 0;
        double currentMachineTime;

        for (int machineId=0; machineId<this.getInstance().getNumMachines(); machineId++) {
            int period = 0;
            double currentPeriodDuration = 0;
            currentMachineTime = 0;
            int machineCapacity = this.getInstance().getMachineCapacity(machineId,period);
            for (int j=0; j<this.numberOfMachineWorkSlots[machineId]; j++) {
                currentMachineTime += this.solutionData[machineId][j].getDuration();
                // If there is a change of period, we account for it
                if ((currentPeriodDuration + this.solutionData[machineId][j].getDuration()) >= machineCapacity) {
                    currentPeriodDuration = currentPeriodDuration + this.solutionData[machineId][j].getDuration() - machineCapacity;
                    period++;
                } else {
                    currentPeriodDuration += this.solutionData[machineId][j].getDuration();
                }
                if ((j < (this.numberOfMachineWorkSlots[machineId] - 1)) &&
                        (this.solutionData[machineId][j].getPartId() != this.solutionData[machineId][j + 1].getPartId())) {
                    int changeoverTime = this.getInstance().getChangeoverTime(this.solutionData[machineId][j].getPartId(), this.solutionData[machineId][j + 1].getPartId());
                    currentMachineTime += changeoverTime;
                    currentPeriodDuration += changeoverTime;
                    // TODO: take into account the changeover time overlapping with the period change
                }
            }
            totalTime = Math.max(totalTime,currentMachineTime);
        }

        return totalTime;
    }
*/

    /**
     * Generate a string representation of this solution. Used when printing progress to console,
     * show as minimal info as possible
     *
     * @return Small string representing the current solution (Example: id + score)
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        double machineTime;
        double maximumTime = 0;
        double accumulatedChangeOverTime = 0;
        double hoursNonPreferredMachine = 0;
        int [] machineProducedParts = new int[this.getInstance().getNumParts()];

        for (int machineId=0; machineId<this.getInstance().getNumMachines(); machineId++) {
            machineTime = 0;
            int period = 0;
            double currentPeriodDuration = 0;
            int machineCapacity = this.getInstance().getMachineCapacity(machineId,period);
            for (int i=0; i<this.getInstance().getNumParts(); i++) {
                machineProducedParts[i] = 0;
            }
            sb.append("Machine ").append(machineId).append(":\n");
            sb.append("\tWsId\tPartId\tDuration\tEnd\tWeek\tInventory\tLOG DURATION\n");
            for (int j=0; j<this.numberOfMachineWorkSlots[machineId]; j++) {
                if (this.getInstance().getPriority(this.solutionData[machineId][j].getPartId(), machineId) > 1)
                    hoursNonPreferredMachine += this.solutionData[machineId][j].getDuration();
                machineTime += this.solutionData[machineId][j].getDuration();
                // If there is a change of period, we account for it
                if ((currentPeriodDuration + this.solutionData[machineId][j].getDuration()) >= machineCapacity) {
                    double hoursToComplete = machineCapacity - currentPeriodDuration;
                    machineProducedParts[this.solutionData[machineId][j].getPartId()] += (int) Math.ceil(hoursToComplete*this.getInstance().productionRate[this.solutionData[machineId][j].getPartId()][machineId]);
                    sb.append("\t").append(j).append("\t").append(this.solutionData[machineId][j].getPartId()).append("\t").append(hoursToComplete).append("\t").append(machineTime - this.solutionData[machineId][j].getDuration() + hoursToComplete).append("\t").append(period + 1).append("\t").append(Arrays.toString(machineProducedParts)).append("\t").append(this.solutionData[machineId][j].getDuration()).append("\n");
                    currentPeriodDuration = currentPeriodDuration + this.solutionData[machineId][j].getDuration() - machineCapacity;
                    machineProducedParts[this.solutionData[machineId][j].getPartId()] += (int) Math.ceil(currentPeriodDuration*this.getInstance().productionRate[this.solutionData[machineId][j].getPartId()][machineId]);
                    period++;
                    machineCapacity = this.getInstance().getMachineCapacity(machineId,period);
                    sb.append("\t").append(j).append("\t").append(this.solutionData[machineId][j].getPartId()).append("\t").append(currentPeriodDuration).append("\t").append(machineTime).append("\t").append(period + 1).append("\t").append(Arrays.toString(machineProducedParts)).append("\t").append(this.solutionData[machineId][j].getDuration()).append("\n");
                } else {
                    currentPeriodDuration += this.solutionData[machineId][j].getDuration();
                    machineProducedParts[this.solutionData[machineId][j].getPartId()] += (int) Math.ceil(this.solutionData[machineId][j].getDuration()*this.getInstance().productionRate[this.solutionData[machineId][j].getPartId()][machineId]);
                    sb.append("\t").append(j).append("\t").append(this.solutionData[machineId][j].getPartId()).append("\t").append(this.solutionData[machineId][j].getDuration()).append("\t").append(machineTime).append("\t").append(period + 1).append("\t").append(Arrays.toString(machineProducedParts)).append("\t").append(this.solutionData[machineId][j].getDuration()).append("\n");
                }
                // If a changeover is needed, we account for it
                if ((j < (this.numberOfMachineWorkSlots[machineId] - 1)) &&
                        (this.solutionData[machineId][j].getPartId() != this.solutionData[machineId][j + 1].getPartId())) {
                    int changeoverTime = this.getInstance().changeoverTime[this.solutionData[machineId][j].getPartId()][this.solutionData[machineId][j + 1].getPartId()];
                    machineTime += changeoverTime;
                    currentPeriodDuration += changeoverTime;
                    accumulatedChangeOverTime += changeoverTime;
                    sb.append("\tChangeover\t").append(changeoverTime).append("\t").append(machineTime).append("\t").append(period + 1).append("\t").append(Arrays.toString(machineProducedParts)).append("\t").append(this.solutionData[machineId][j].getDuration()).append("\n");
                    // If changeover time reaches the end of the period, account for it in total time, and reset period
                    if (currentPeriodDuration >= machineCapacity) {
                        currentPeriodDuration -= machineCapacity;
                        sb.append("\tChangeover overlapped period\n");
                        period++;
                    }
                }
            }
            if (machineTime > maximumTime) {
                maximumTime = machineTime;
            }
        }
        sb.append("\nMaximum time: ").append(maximumTime).append("\n");

        int [] totalProducedParts = new int[this.getInstance().getNumParts()];
        int [] lastInventory = new int[this.getInstance().getNumParts()];
        int [] difference = new int[this.getInstance().getNumParts()];
        int [][] weeklyProducedParts2 = new int[this.getInstance().getNumPeriods()][this.getInstance().getNumParts()];

        double objFunctionShortage = fullShortageCalculation(totalProducedParts, lastInventory, difference,weeklyProducedParts2);
        long shortage = 0;

        // TODO producedParts may generate less parts due to rounding of numbers in the week change

        int[][] allInventory = new int[this.getInstance().getNumPeriods()][this.getInstance().getNumParts()];
        int[][] allShortage = new int[this.getInstance().getNumPeriods()][this.getInstance().getNumParts()];

        sb.append("\n\nWeekly detail:\n\n");
        for (int i=0; i<this.getInstance().getNumPeriods(); i++) {
            int [] inventoryLevel = new int[this.getInstance().getNumParts()];
            for (int j=0; j<this.getInstance().getNumParts(); j++) {
                inventoryLevel[j] = weeklyProducedParts2[i][j] + this.getInstance().getInventoryLevelByPeriod(i)[j];
                if (inventoryLevel[j] < 0) {
                    shortage += (-1) * inventoryLevel[j];
                    allShortage[i][j] = (-1) * inventoryLevel[j];
                }
            }
            sb.append("\tProduction ").append(i + 1).append(": ").append(Arrays.toString(weeklyProducedParts2[i])).append("\n");
            sb.append("\tOrig. Inv. Level ").append(i + 1).append(": ").append(Arrays.toString(this.getInstance().getInventoryLevelByPeriod(i))).append("\n");
            sb.append("\tInv. Level ").append(i + 1).append(": ").append(Arrays.toString(inventoryLevel)).append("\n\n");
            allInventory[i] = inventoryLevel;
        }

        sb.append("\nWeekly produced parts:\n");
        for (int i=0; i<this.getInstance().getNumPeriods(); i++) {
            sb.append("\tPeriod ").append(i + 1).append(": ").append(Arrays.toString(weeklyProducedParts2[i])).append("\n");
        }

        sb.append("\nAll Inventory:\n");
        for (int i=0; i<this.getInstance().getNumPeriods(); i++) {
            sb.append("\tPeriod ").append(i + 1).append(": ").append(Arrays.toString(allInventory[i])).append("\n");
        }

        sb.append("\nAll Shortage:\n");
        for (int i=0; i<this.getInstance().getNumPeriods(); i++) {
            sb.append("\tPeriod ").append(i + 1).append(": ").append(Arrays.toString(allShortage[i])).append("\n");
        }

        sb.append("\n\nTotal production:\t").append("\t").append(Arrays.toString(totalProducedParts));

        sb.append("\nOrig. Inv. Level:\t").append("\t").append(Arrays.toString(lastInventory));

        sb.append("\nInventory Level:\t").append("\t").append(Arrays.toString(difference));

        sb.append("\nShortage f.o.:\t\t\t\t").append(objFunctionShortage);
        sb.append("\nTotal changeover time:\t\t").append(accumulatedChangeOverTime).append("\n");
        sb.append("\n\nSCORE: ").append(this.getScore()).append("\n");
        sb.append("\nRecalculated SCORE: ").append(this.recalculateScore()).append("\n");

        sb.append("\n\n---- Comparison with exact model data ----");
        sb.append("\nShortage:\t\t\t\t\t").append(shortage);
        sb.append("\nChangeover time:\t\t\t").append(accumulatedChangeOverTime);
        sb.append("\nPriority:\t").append(hoursNonPreferredMachine).append("\n");

        return sb.toString();

        // TODO: When all fields are implemented delete this method and use your IDE
        //  to autogenerate it using only the most important fields.
        // This method will be called to print best solutions in console while solving, and by your IDE when debugging
        // WARNING: DO NOT UPDATE CACHES IN THIS METHOD / MAKE SURE THIS METHOD DOES NOT HAVE SIDE EFFECTS
        // Calling toString to a solution should NEVER change or update any of its fields
        //throw new UnsupportedOperationException("CLSPSolution::toString not implemented yet");
    }

//    /**
//     * Optionally provide a way to calculate custom solution properties
//     * @return a map with the property names as keys,
//     * and a function to calculate the property value given the solution as value
//     */
//    @Override
//    public Map<String, Function<CLSPSolution, Object>> customProperties() {
//        var properties = super.customProperties();
//        // . Examples:
//        // properties.put("myCustomPropertyName", s -> s.getScore());
//        // properties.put("myCustomProperty2Name", CLSPSolution::getScore);
//        return properties;
//    }

    /**
     * Adds a work slot to the given machineID as the current last slot
     * @param machineId Id of the machine to add the work slot
     * @param workSlot Work slot to add
     */
    public void addWorkSlot(int machineId, WorkSlot workSlot) {

        if (this.numberOfMachineWorkSlots[machineId] == this.solutionData[machineId].length) {
            throw new ArrayIndexOutOfBoundsException("The machine " + machineId + " has no more work slots available");
        }

        // Calculate initial time and period
        double iniTime = 0;
        int iniPeriod = 0;
        if (this.numberOfMachineWorkSlots[machineId] > 0) {
            // Not first workslot, add changeover time to previous workslot
            double changeover = this.getInstance().changeoverTime[workSlot.getPartId()][this.solutionData[machineId][this.numberOfMachineWorkSlots[machineId]-1].getPartId()];
            iniTime = this.solutionData[machineId][this.numberOfMachineWorkSlots[machineId]-1].getEndTime() + changeover;
            iniPeriod = this.solutionData[machineId][this.numberOfMachineWorkSlots[machineId]-1].getEndPeriod();
            // Add changeover time to total changeover time
            this.changeoverTime += changeover;

            // Check if the changeover time overlaps the period
            double machineCapacity = this.getInstance().getMachineCapacity(machineId,iniPeriod);
            if (iniTime >= machineCapacity) {
                iniTime -= machineCapacity;
                iniPeriod++;
            }
        }
        workSlot.setIniTime(iniTime);
        workSlot.setIniPeriod(iniPeriod);


        // Calculate end time and period, and check if overlaps period
        int endPeriod = iniPeriod;
        double machineCapacity = this.getInstance().getMachineCapacity(machineId, endPeriod);
        double endTime = iniTime + workSlot.getDuration();
        if (endTime >= machineCapacity) {
            if (endPeriod < this.getInstance().getNumPeriods())
                machineWeeklyProducedParts[machineId][endPeriod][workSlot.getPartId()] += (int) Math.ceil((machineCapacity-iniTime) * this.getInstance().productionRate[workSlot.getPartId()][machineId]);
            endTime -= machineCapacity;
            endPeriod++;
            if (endPeriod < this.getInstance().getNumPeriods())
                machineWeeklyProducedParts[machineId][endPeriod][workSlot.getPartId()] += (int) Math.ceil(endTime * this.getInstance().productionRate[workSlot.getPartId()][machineId]);
        } else {
            if (endPeriod < this.getInstance().getNumPeriods())
                machineWeeklyProducedParts[machineId][endPeriod][workSlot.getPartId()] += (int) Math.ceil(workSlot.getDuration() * this.getInstance().productionRate[workSlot.getPartId()][machineId]);
        }
        workSlot.setEndTime(endTime);
        workSlot.setEndPeriod(endPeriod);

        // Adds a work slot to the given machineID as the last slot
        this.solutionData[machineId][this.numberOfMachineWorkSlots[machineId]] = workSlot;
        this.numberOfMachineWorkSlots[machineId]++;
    }

    /**
     * After adding the workslots, the weekly production is only known for each week. This method accumulates the
     * weekly production to obtain the total production for each part on every machine. It also fills in the value
     * of the shortage attribute. This method is called ONLY by constructive methods.
     */
    public void accumulateWeeklyProductionAndCalculatesScore(boolean accumulateWeeklyProduction) {
        if (accumulateWeeklyProduction) {
            // Begins in the second period
            for (int period = 1; period < this.getInstance().getNumPeriods(); period++) {
                for (int machine = 0; machine < this.getInstance().getNumMachines(); machine++) {
                    for (int part = 0; part < this.getInstance().getNumParts(); part++) {
                        this.machineWeeklyProducedParts[machine][period][part] += this.machineWeeklyProducedParts[machine][period - 1][part];
                    }
                }
            }

        }


        // Shortage is calculated for each week
        int[][] weeklyProductShortage = new int[this.getInstance().getNumPeriods()][this.getInstance().getNumParts()];
        int[] weeklyShortage = fullCalculateWeeklyShortage(this.machineWeeklyProducedParts, weeklyProductShortage);
        int[][] weeklyProducedParts = new int[this.getInstance().getNumPeriods()][this.getInstance().getNumParts()];
        calculateWeeklyProducedParts(this.machineWeeklyProducedParts, weeklyProducedParts,this.getInstance());

        this.shortage = calculateShortageFunction(weeklyShortage);

        this.score = scoreCalculation(fullChangeOverTimeCalculation(),shortage);

    }

    /**
     * Shortage is calculated for each week
     *
     * @param machineWeeklyProducedParts Weekly produced parts for each part and machine.
     * @param weeklyProductShortage Weekly product shortage (positive and negative) for each part and
     *                             machine. This structure is filled in by the method.
     * @return Array with the shortage for each week, only with positive values which corresponds to negative shortage.
     */
    public int[] fullCalculateWeeklyShortage(int[][][] machineWeeklyProducedParts, int[][] weeklyProductShortage) {
        int [][] weeklyProducedParts = new int[this.getInstance().getNumPeriods()][this.getInstance().getNumParts()];
        calculateWeeklyProducedParts(machineWeeklyProducedParts, weeklyProducedParts,this.getInstance());
        int [] weeklyShortage = new int[this.getInstance().getNumPeriods()];
        int difference;

        for (int period=0; period<this.getInstance().getNumPeriods(); period++) {
            for (int i=0; i<this.getInstance().getNumParts(); i++) {
                difference = weeklyProducedParts[period][i] + this.getInstance().inventory[i][period];
                weeklyProductShortage[period][i] = difference;
                if (difference < 0)
                    weeklyShortage[period] += (-1)*difference;
            }
        }
        return weeklyShortage;
    }

    /**
     * Receives machineWeeklyProducedParts and calculates the weekly produced parts for each part filling in
     * weeklyProducedParts structure from scratch.
     * @param machineWeeklyProducedParts Weekly produced parts for each part and machine
     * @param weeklyProducedParts Weekly produced parts for each part. This structure is filled in by the method
     *                            and MUST BE EMPTY!
     * @param instance Instance of the problem
     */
    public static void calculateWeeklyProducedParts(int[][][] machineWeeklyProducedParts, int[][] weeklyProducedParts, CLSPInstance instance) {
        for (int period = 0; period < instance.getNumPeriods(); period++) {
            for (int machine = 0; machine < instance.getNumMachines(); machine++) {
                for (int part = 0; part < instance.getNumParts(); part++) {
                    weeklyProducedParts[period][part] += machineWeeklyProducedParts[machine][period][part];
                }
            }
        }
    }

    /* *
     * Calculates the maximum time needed to complete all work slots of the given machine and updates the machineMaxTime
     * variable.
     * @param machine Machine to calculate the maximum time
     * @return The maximum time of the machine.
     * /
    private double calculateMachineMaxTime(int machine) {
        double maxTime = this.solutionData[machine][this.numberOfMachineWorkSlots[machine]-1].getEndTime();
        // The following cannot be a product since the machine capacity is not constant
        for (int i = 0; i<this.solutionData[machine][this.numberOfMachineWorkSlots[machine]-1].getEndPeriod(); i++) {
            maxTime += this.getInstance().getMachineCapacity(machine,i);
        }
        machineMaxTime[machine] = maxTime;
        return maxTime;
    }
*/
    public double calculateShortageFunction(int[] weeklyShortage) {
        double shortage = 0;
        for (int period=0; period<this.getInstance().getNumPeriods(); period++) {
            // TODO: revise weights of each period
            shortage += weeklyShortage[period]; // * Math.pow(10,this.getInstance().getNumPeriods()-period-1);
        }
        return shortage;
    }

    /**
     * Updates the timing of the work slots of the given machine, starting from the given position and ending in the
     * last workslot.
     * @param machineId Id of the machine to update the timing
     * @param initialPosition Initial position to start updating the timing
     */
    private void updateWorkSlotTiming(int machineId, int initialPosition) {
        var wsArray = this.solutionData[machineId];
        for (int i = initialPosition; i<this.numberOfMachineWorkSlots[machineId]; i++) {
            updateIndividualWorkSlotTiming(i,wsArray, machineId, this.getInstance());
        }
    }

    /**
     * Updates the timing of the work slot in position i of the given machine.
     * @param i Position of the work slot to update the timing
     * @param wsArray Array of work slots of the machine
     * @param machineId Id of the machine to update the timing
     * @param instance Instance of the problem
     */
    public static void updateIndividualWorkSlotTiming(int i, WorkSlot[] wsArray, int machineId, CLSPInstance instance) {
        if (i == 0) {
            // Initial position must be 0, so the initial workslot is the first one
            wsArray[i].setIniTime(0);
            wsArray[i].setIniPeriod(0);
        } else {
            // Add changeover time with previous workslot
            wsArray[i].setIniTime(wsArray[i - 1].getEndTime()+instance.changeoverTime[wsArray[i - 1].getPartId()][wsArray[i].getPartId()]);
            wsArray[i].setIniPeriod(wsArray[i - 1].getEndPeriod());
            // If changeover time exceeds the period, update the period
            if (wsArray[i].getIniTime() > instance.getMachineCapacity(machineId,wsArray[i - 1].getEndPeriod())) {
                wsArray[i].setIniTime(wsArray[i].getIniTime() - instance.getMachineCapacity(machineId,wsArray[i - 1].getEndPeriod()));
                wsArray[i].setIniPeriod(wsArray[i - 1].getEndPeriod()+1);
            }
        }
        // Update end time and period
        wsArray[i].setEndTime(wsArray[i].getIniTime()+wsArray[i].getDuration());
        // If end time exceeds the period, update the period
        if (wsArray[i].getEndTime() > instance.getMachineCapacity(machineId,wsArray[i].getIniPeriod())) {
            var endTime = wsArray[i].getEndTime();
            int endPeriod = wsArray[i].getIniPeriod();
            do {
                endTime -= instance.getMachineCapacity(machineId, wsArray[i].getIniPeriod());
                endPeriod++;
                wsArray[i].setEndTime(endTime);
                wsArray[i].setEndPeriod(endPeriod);
            } while (endTime > instance.getMachineCapacity(machineId,wsArray[i].getIniPeriod()));
        } else {
            wsArray[i].setEndPeriod(wsArray[i].getIniPeriod());
        }
        // Assert
        if (wsArray[i].getEndTime() > instance.getMachineCapacity(machineId,wsArray[i].getEndPeriod()))
            throw new RuntimeException("Collapse: workslot with end time larger than allowed - "+wsArray[i]);
    }

    /**
     * Updates the weekly production of the given machine, starting from the given position and ending in the last
     * workslot of the machine.
     * @param givenMachineWeeklyProducedParts Number of produced parts by machine, period and part
     */
    private void updateWeeklyProduction(int [][][] givenMachineWeeklyProducedParts) {
        for (int machineId=0; machineId<this.getInstance().getNumMachines(); machineId++)
            for (int i = 0; i < this.getInstance().getNumPeriods(); i++)
                if (this.getInstance().getNumParts() >= 0)
                    System.arraycopy(givenMachineWeeklyProducedParts[machineId][i], 0, this.machineWeeklyProducedParts[machineId][i], 0, this.getInstance().getNumParts());
    }

    public void updateDataStructures(int initialMachineId, int initialPosition, int finalMachineId, int finalPosition, double changeoverTime, int[][][] machineWeeklyProducedParts, double shortage) {
        // Update time information of the workslots
        updateCommonDataStructures(initialMachineId, initialPosition, finalMachineId, finalPosition, changeoverTime, shortage);

        updateWeeklyProduction(machineWeeklyProducedParts);

    }

    public void removeUnassignedWorkSlot(WorkSlot workSlot) {
        unassignedWorkSlots.remove(workSlot);
    }

    /**
     * Collapses consecutive work slots corresponding to the same product on each machine.
     */
    public void collapse() {

        WorkSlot[][] collapsedSolutionData = new WorkSlot[this.getInstance().getNumMachines()][this.getSolutionData()[0].length];
        for (int machine = 0; machine < solutionData.length; machine++) {
            int i = 0;
            int newWorkslots = 0;
            while (i < this.numberOfMachineWorkSlots[machine]-1) {
                int j = i+1;
                WorkSlot w = new WorkSlot(solutionData[machine][i]);
                WorkSlot next = solutionData[machine][j];
                while (w.getPartId() == next.getPartId()) {
                    w.collapse(next);
                    j++;
                    if (j == this.numberOfMachineWorkSlots[machine]) {
                        break;
                    }
                    next = solutionData[machine][j];
                }
                collapsedSolutionData[machine][newWorkslots] = w;
                newWorkslots++;
                i = j;
            }
            // Last two workslots were different, so we add the last one, only if there are more than one workslot
            if ((this.numberOfMachineWorkSlots[machine] > 1) &&
               (solutionData[machine][this.numberOfMachineWorkSlots[machine]-1].getPartId() != solutionData[machine][this.numberOfMachineWorkSlots[machine]-2].getPartId())) {
                collapsedSolutionData[machine][newWorkslots] = new WorkSlot(solutionData[machine][i]);
                newWorkslots++;
            }
            this.numberOfMachineWorkSlots[machine] = newWorkslots;
        }

        this.solutionData = collapsedSolutionData;

        // Update timing:
        for (int machine = 0; machine < solutionData.length; machine++) {
            updateWorkSlotTiming(machine,0);
        }

    }

    /**
     * Separates work slots corresponding to the same product on each machine according to minimum batch size.
     */
    public void uncollapse() {
        double minBatchSize = this.getInstance().getMinimumWorkload();

        WorkSlot[][] uncollapsedSolutionData = new WorkSlot[this.getInstance().getNumMachines()][this.getInstance().maximumWorkSlots()];
        for (int machine = 0; machine < solutionData.length; machine++) {
            int newWorkslots = 0;
            for (int i = 0; i < this.numberOfMachineWorkSlots[machine]; i++) {
                double duration = solutionData[machine][i].getDuration();
                while (duration >= 2 * minBatchSize) {
                    uncollapsedSolutionData[machine][newWorkslots] = new WorkSlot(solutionData[machine][i].getPartId(), minBatchSize);
                    newWorkslots++;
                    duration -= minBatchSize;
                }
                uncollapsedSolutionData[machine][newWorkslots] = new WorkSlot(solutionData[machine][i].getPartId(), duration);
                newWorkslots++;
            }
            this.numberOfMachineWorkSlots[machine] = newWorkslots;
        }

        this.solutionData = uncollapsedSolutionData;

        // Update timing:
        for (int machine = 0; machine < solutionData.length; machine++) {
            updateWorkSlotTiming(machine,0);
        }

    }



    public boolean isEmpty() {
        for (int i=0; i<this.getInstance().getNumMachines(); i++) {
            if (this.numberOfMachineWorkSlots[i] > 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Map<String, Function<CLSPSolution, Object>> customProperties() {
        return Map.of(
                "Shortage", CLSPSolution::getShortageGurobi,
                "Changeover", CLSPSolution::getChangeoverTime,
                "Priority", CLSPSolution::getPriorityGurobi
        );
    }

    public void updateDataStructuresOptimized(int initialMachineId, int initialPosition, int finalMachineId, int finalPosition, double changeoverTime, double shortage, int[][] newIniMachineWeeklyProducedParts, int[][] newEndMachineWeeklyProducedParts) {
        // Update time information of the workslots
        updateCommonDataStructures(initialMachineId, initialPosition, finalMachineId, finalPosition, changeoverTime, shortage);

        // Updates weekly production of the solution. Assume the array was already copied!!
        machineWeeklyProducedParts[initialMachineId] = newIniMachineWeeklyProducedParts;
        if (initialMachineId != finalMachineId)
            machineWeeklyProducedParts[finalMachineId] = newEndMachineWeeklyProducedParts;

    }

//    private void updateMachineWeeklyProduction(int[][] newMachineWeeklyProducedParts, int machine) {
//            for (int i = 0; i < this.getInstance().getNumPeriods(); i++)
//                for (int j = 0; j < this.getInstance().getNumParts(); j++)
//                    machineWeeklyProducedParts[machine][i][j] = newMachineWeeklyProducedParts[i][j];
//    }

    private void updateCommonDataStructures(int initialMachineId, int initialPosition, int finalMachineId, int finalPosition, double changeoverTime, double shortage) {
        if (initialMachineId == finalMachineId) {
            int ini = Math.min(initialPosition, finalPosition);
            // Updates timing from the initial position to the final position of the machine, since the maximum
            // could have changed.
            // TODO Maybe this process could be optimized
            updateWorkSlotTiming(initialMachineId, ini);
        } else {
            // Updates timing from each position to the final position of the machine, since the maximum
            // could have changed.
            // TODO Maybe this process could be optimized
            updateWorkSlotTiming(initialMachineId, initialPosition);
            updateWorkSlotTiming(finalMachineId, finalPosition);
        }


        // Update changeover time and shortage
        this.changeoverTime = changeoverTime;
        this.shortage = shortage;

    }


/*
    public static void main(String[] args) {
        String filename = "instances/testing/toy-instance.txt";
        CLSPInstance instance = new CLSPInstanceImporter().checkInstance(filename);

        System.out.println("\n---------------- HAND-MADE SOLUTION ------------------");
        CLSPSolution s = new CLSPSolution(instance);

        // Hand-made solution.
        s.addWorkSlot(0, new WorkSlot(3, 10));
        s.addWorkSlot(0, new WorkSlot(4, 23.33333333333333333));
        s.addWorkSlot(0, new WorkSlot(2, 90.7));
        s.addWorkSlot(0, new WorkSlot(0, 22.8));
        s.addWorkSlot(0, new WorkSlot(1, 32.5));
        s.addWorkSlot(0, new WorkSlot(2, 59.3));

        s.accumulateWeeklyProductionAndCalculatesScore();

        System.out.println(s);
        s.getScore();

/*
        // Not working due to MORK  random manager not initialized

        // Random solution
        CLSPRandomConstructive constructive = new CLSPRandomConstructive();
        final int NUM_SOLUTIONS = 100;
        CLSPSolution bestSolution = new CLSPSolution(instance);
        constructive.construct(bestSolution);

        for (int i=0; i<NUM_SOLUTIONS; i++) {
            CLSPSolution s1 = new CLSPSolution(instance);
            constructive.construct(s1);
            if (s1._isBetterThan(bestSolution)) {
                bestSolution = new CLSPSolution(s1);
            }
            System.out.println(s1.getScore());
        }
        System.out.println("\n\n---------------- BEST RANDOM SOLUTION OUT OF " + NUM_SOLUTIONS + "------------------");
        System.out.println(bestSolution);
        bestSolution.getScore();
* /

    }
*/

}
