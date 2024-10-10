package es.urjc.etsii.grafo.CLSP.model;

import es.urjc.etsii.grafo.io.Instance;
import es.urjc.etsii.grafo.util.ArrayUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.max;

public class CLSPInstance extends Instance {

    private final int numParts;
    private final int numMachines;
    private final int numPeriods;
    public final int[][] productionRate;
    public final int[][] changeoverTime;

    public final int[][] inventory;
    private final int[][] capacity;
    private final int[][] priority;

    private final int maximumShortage;
    private final int minimumWorkload;
    private final int maximumWorkSlots;
    private final List<WorkSlot> slotsMinimumWorkload = new ArrayList<>();
    private final List<WorkSlot> slotsNoShortage = new ArrayList<>();

    public CLSPInstance(String filename, int numParts, int numMachines, int numPeriods, int[][] productionRate, int[][] changeoverTime, int[][] inventory, int[][] capacity, int[][] priority) {
        super(filename);
        this.numParts = numParts;
        this.numMachines = numMachines;
        this.numPeriods = numPeriods;
        this.productionRate = new int[numParts][numMachines];
        for (int i = 0; i < numParts; i++) {
            System.arraycopy(productionRate[i], 0, this.productionRate[i], 0, numMachines);
        }
        this.changeoverTime = new int[numParts][numParts];
        for (int i = 0; i < numParts; i++) {
            System.arraycopy(changeoverTime[i], 0, this.changeoverTime[i], 0, numParts);
        }
        this.inventory = new int[numParts][numPeriods];
        for (int i = 0; i < numParts; i++) {
            System.arraycopy(inventory[i], 0, this.inventory[i], 0, numPeriods);
        }
        this.capacity = new int[numMachines][numPeriods];
        for (int i = 0; i < numMachines; i++) {
            System.arraycopy(capacity[i], 0, this.capacity[i], 0, numPeriods);
        }
        this.priority = new int[numParts][numMachines];
        for (int i = 0; i < numParts; i++) {
            System.arraycopy(priority[i], 0, this.priority[i], 0, numMachines);
        }

        maximumShortage = obtainMaximumShortage();
        minimumWorkload = obtainMinimumWorkload();
        // The maximum number of work slots is the next integer value after -1.0 multiplied by the maximum shortage and divided by the minimum workload
        maximumWorkSlots = (int) Math.ceil( ((-1.0) * maximumShortage) / minimumWorkload);

        // Generate slots considering the inventory level and the PPH of each machine:

        // For each product, obtain the number of slots according to the number of hours needed according to last week inventory and PPH.
        int[] numberOfSlots = new int[numParts];
        double[] hoursNeeded = new double[numParts];
        for (int i = 0; i < numParts; i++) {
             /* Each machine has a different PPH, so we should take that into account.
               We use the minimum PPH of all the machines that can produce the part (value > 0) */
            double minimumRate = getMinimumRate(numMachines, productionRate, i);
            hoursNeeded[i] = (-1.0)*inventory[i][numPeriods-1]/minimumRate;
            numberOfSlots[i] = (int) Math.ceil(hoursNeeded[i]/minimumWorkload);
        }

        // Generate slots
        for (int p = 0; p < numParts; p++) {
            for (int s = 0; s < numberOfSlots[p]; s++) {
                slotsMinimumWorkload.add(new WorkSlot(p,minimumWorkload));
                slotsNoShortage.add(new WorkSlot(p,minimumWorkload));
            }
            // Correct the last slots if needed:
            if ((hoursNeeded[p] > minimumWorkload) && (hoursNeeded[p]%minimumWorkload != 0)) {
                // Remove the last two slots and add a new one with the remaining hours
                slotsNoShortage.remove(slotsNoShortage.size()-1);
                slotsNoShortage.remove(slotsNoShortage.size()-1);
                slotsNoShortage.add(new WorkSlot(p, minimumWorkload + (hoursNeeded[p] % minimumWorkload)));
            }
        }

        // Properties for the instance selection
        this.setProperty("numParts", numParts);
        this.setProperty("numMachines", numMachines);
        this.setProperty("numPeriods", numPeriods);
        this.setProperty("maximumShortage", maximumShortage);
        this.setProperty("avgProductionRate", ArrayUtil.sum(ArrayUtil.flatten(productionRate)) / (numParts * numMachines));
        this.setProperty("avgInventory", ArrayUtil.sum(ArrayUtil.flatten(inventory)) / (numParts * numPeriods));

    }

    private static double getMinimumRate(int numMachines, int[][] productionRate, int part) {
        double minimumRate = Double.MAX_VALUE;
        for (int j = 0; j < numMachines; j++) {
            if (productionRate[part][j] > 0) {
                minimumRate = Math.min(minimumRate,productionRate[part][j]);
            }
        }
        if (minimumRate == Double.MAX_VALUE) throw new IllegalArgumentException("No machine can produce part " + part);
        return minimumRate;
    }


    // Minimum workload is the maximum of the changeover time matrix
    private int obtainMinimumWorkload() {
        var max = 0;
        for (int i = 0; i < numParts; i++) {
            for (int j = 0; j < numParts; j++) {
                if (changeoverTime[i][j] > max) {
                    max = changeoverTime[i][j];
                }
            }
        }
        return max;
    }

    private int obtainMaximumShortage() {
        var sum = 0;
        for (int i = 0; i < numParts; i++) {
            sum += inventory[i][numPeriods-1];
        }
        return sum;
    }

    @Override
    public String toString() {
        return "CLSPInstance{" +
                "\nnumParts=" + numParts +
                "\nnumMachines=" + numMachines +
                "\nnumPeriods=" + numPeriods +
                "\nproductionRate=" + Arrays.deepToString(productionRate) +
                "\nchangeoverTime=" + Arrays.deepToString(changeoverTime) +
                "\ninventory=" + Arrays.deepToString(inventory) +
                "\ncapacity=" + Arrays.deepToString(capacity) +
                "\npriority=" + Arrays.deepToString(priority) +
                "\nmaximumShortage=" + maximumShortage +
                "\nminimumWorkload=" + minimumWorkload +
                "\nmaximumWorkSlots=" + maximumWorkSlots +
                "\n}";
    }

    public int getNumMachines() {
        return numMachines;
    }

    public int maximumWorkSlots() {
        return maximumWorkSlots;
    }

    public int getNumParts() {
        return numParts;
    }

    public int getMachineCapacity(int machineId, int period) {
        // If period is out of bounds, return last period
        if (period >= numPeriods) {
            period = numPeriods - 1;
        }
        return capacity[machineId][period];
    }

    public int getNumPeriods() {
        return numPeriods;
    }

    public double getMinimumWorkload() {
        return minimumWorkload;
    }

    public int[] getInventoryLevelByPeriod(int period) {
        int [] inventoryLevel = new int[numParts];
        for (int i = 0; i < numParts; i++) {
            inventoryLevel[i] = inventory[i][period];
        }
        return inventoryLevel;
    }

//    public List<WorkSlot> getCopyOfSlotsMinimumWorkload() {
//        return new ArrayList<>(slotsMinimumWorkload);
//    }

    public List<WorkSlot> getCopyOfSlotsNoShortage() {
        return new ArrayList<>(slotsNoShortage);
    }

    public double getPriority(int partId, int machineId) {
        return priority[partId][machineId];
    }

    //    /**
//     * How should instances be ordered, when listing and solving them.
//     * If not implemented, defaults to lexicographic sort by instance name
//     * @param other the other instance to be compared against this one
//     * @return comparison result
//     */
//    @Override
//    public int compareTo(Instance other) {
//        var otherInstance = (CLSPInstance) other;
//        return Integer.compare(this.size, otherInstance.size);
//    }

//    /**
//     * Define custom properties for the instance
//     * @return Map of properties, with each entry containing the property name and its value
//     */
//    @Override
//    public Map<String, Object> customProperties() {
//        var properties =  super.customProperties();
//        properties.put("MyInstancePropertyName", 7);
//        properties.put("MyInstanceProperty2Name", "Hello world");
//        return properties;
//    }
}
