package es.urjc.etsii.grafo.CLSP.hexaly;


import es.urjc.etsii.grafo.CLSP.model.CLSPInstance;
import es.urjc.etsii.grafo.CLSP.model.CLSPSolution;
import localsolver.*;
import org.apache.commons.collections4.IterableUtils;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.toIntExact;

/**
 * Local Solver Model v2, uses a single work sequence for each machine,
 * using an external function to compute the number of items produced per period
 * Variable name prefixes:
 * e_: generic (e)xpressions, calculated by LocalSolver
 * l_: (l)ambda expressions
 * d_: expressions created from raw instance (d)ata
 * o_: (o)bjective function expression
 * c_: (c)onstraint expression
 * a_: (a)rray expression
 */
public class CLSPModelFinal extends CLSPModel {

    private final boolean combineObjectives;


    double biggestWorkslot;


    /**
     * this.workslots[i].partId == at(this.d_partIds, i)
     */
    LSExpression d_partIds;

    /**
     * this.workslots[i].duration == at(this.d_durations, i)
     */
    LSExpression d_durations;

    /**
     * Changeover times, used to compute the totalChangeOvertime by LocalSolver
     * changeoverTimes[i][j] = time to change from part i to part j in ANY machine
     * All machines have the same changeover time between parts
     */
    int[][] changeoverTimes;
    LSExpression d_changeoverTimes;

    /**
     * Production rate, used to compute how many parts are produced of each type per workslot and machine
     * productionRate[i][j] = how many parts of type i are produced per time unit in machine j
     */
    double[][] productionRate;
    LSExpression d_productionRate;

    /**
     * Minimum inventory to generate for each part id, up tp the given period, real instance is a matrix because it is per period
     * <p>
     * Minimum inventory required up until that period
     * and check if the last period is contains the total inventory required or only the inventory of the last period
     */
    int[][] inventoryForecast;

    /**
     * Maximum machine capacity per period
     */
    int[][] machineCapacity;

    /**
     * Machine capacity used in LocalSolver, allows an extra work slot assignment
     */
    int[] totalMachineCapacity;


    /**
     * Priority of each part in each machine. Int as boolean, 0 means part is assigned to a preferred machine, 1 means the assignment is not preferred
     */
    int[][] isNonPreferredMachine;

    /**
     * e_workSlotSeqs[i] --> Sequence of work to be done by machine i in all periods
     */
    LSExpression[] e_workSlotSeqs;

    /**
     * Same as e_workSlotSeqs, but wrapped as a LocalSolver array type
     * Required for calling our external function
     */
    LSExpression e_a_workSlotSeqs;


    /**
     * Number of work slots assigned to each machine
     */
    LSExpression[] e_workSlotCount;

    /**
     * Main objective, minimize shortage
     */
    LSExpression o_totalShortage;

    /**
     * Second objective, minimize total changeover time
     */
    LSExpression o_totalChangeover;

    /**
     * Used capacity for each machine and period
     */
    LSExpression[] e_machineUsedTime;

    /**
     * Total changeover time per machine
     */
    LSExpression[] e_changeoverPerMachine;

    /**
     * How many parts have we generated per product id in each period.
     * Warning: this structure is both a java and a localsolver array:
     * model.at(e_producedParts[i], j)) returns the number of produced parts of type i in period j
     */
    LSExpression[] e_producedParts;

    /**
     * How many parts have we generated per product id up to the given period
     */
    LSExpression[][] e_accumulatedProducedParts;

    /**
     * How many parts we are missing for each product ID up to each period
     */
    LSExpression[][] e_shortageParts;

    public CLSPModelFinal(CLSPInstance instance, LocalSolver localsolver, boolean combineObjectives) {
        this.localSolver = localsolver;
        this.instance = instance;
        this.nParts = instance.getNumParts();
        this.nMachines = instance.getNumMachines();
        this.nPeriods = instance.getNumPeriods();
        this.combineObjectives = combineObjectives;

        var list = new ArrayList<HexalyWorkSlot>();
        this.biggestWorkslot = 0;
        for (var w : instance.getCopyOfSlotsNoShortage()) {
            list.add(new HexalyWorkSlot(w.getPartId(), w.getDuration()));
            this.biggestWorkslot = Math.max(this.biggestWorkslot, w.getDuration());
        }
        this.workslots = list.toArray(new HexalyWorkSlot[0]);

        this.partIds = new int[workslots.length];
        this.durations = new double[workslots.length];
        for (int i = 0; i < workslots.length; i++) {
            var w = workslots[i];
            partIds[i] = w.partId();
            durations[i] = w.duration();
        }

        this.changeoverTimes = new int[nParts][nParts];
        for (int i = 0; i < nParts; i++) {
            System.arraycopy(instance.changeoverTime[i], 0, this.changeoverTimes[i], 0, nParts);
        }
        this.productionRate = new double[nParts][nMachines];
        for (int i = 0; i < nParts; i++) {
            for (int j = 0; j < nMachines; j++) {
                productionRate[i][j] = instance.productionRate[i][j];
            }
        }
        this.inventoryForecast = new int[nParts][nPeriods];
        for (int i = 0; i < nParts; i++) {
            System.arraycopy(instance.inventory[i], 0, this.inventoryForecast[i], 0, nPeriods);
        }

        this.machineCapacity = new int[nMachines][nPeriods];
        this.totalMachineCapacity = new int[nMachines];
        for (int i = 0; i < nMachines; i++) {
            int total = 0;
            for (int j = 0; j < nPeriods; j++) {
                var capacity = instance.getMachineCapacity(i, j);
                this.machineCapacity[i][j] = capacity;
                total += capacity;
            }
            this.totalMachineCapacity[i] = total + (int) Math.round(biggestWorkslot * 1.2); // A bit of slack so we take into account hypothetical fragments.
        }

        this.isNonPreferredMachine = new int[nParts][nMachines];
        for (int i = 0; i < nParts; i++) {
            for (int j = 0; j < nMachines; j++) {
                this.isNonPreferredMachine[i][j] = (instance.getPriority(i, j) > 1) ? 1 : 0; // int as boolean: 1 means true, 0 false
            }
        }

        // Number of produced parts in each period
        e_producedParts = new LSExpression[nParts];

        // Accumulated number of produced parts by id up to the given period
        e_accumulatedProducedParts = new LSExpression[nParts][nPeriods];

        // Changeover per machine and period, does not include inter period changeover
        e_changeoverPerMachine = new LSExpression[nMachines];

        // How many parts we are missing
        e_shortageParts = new LSExpression[nParts][nPeriods];
    }


    public void solve(long timelimit, TimeUnit unit) {
        LSModel model = localSolver.getModel();

        // Initialize model
        initModel(model);

        // Close model and solve
        model.close();

        localSolver.getParam().setTimeLimit((int) unit.toSeconds(timelimit));
        localSolver.solve();
    }

    public void initModel(LSModel model) {
        // LocalSolver needs its own datatypes, cannot use int[] double[] etc as is
        d_changeoverTimes = model.array(changeoverTimes);
        d_productionRate = model.array(productionRate);
        d_partIds = model.array(partIds);
        d_durations = model.array(durations);

        // Objectives
        o_totalShortage = model.sum();
        o_totalChangeover = model.sum();

        if (combineObjectives) {
            model.minimize(model.sum(o_totalShortage, o_totalChangeover));
        } else {
            model.minimize(o_totalShortage);
            model.minimize(o_totalChangeover);
        }

        e_workSlotSeqs = new LSExpression[nMachines];
        e_workSlotCount = new LSExpression[nMachines];
        e_machineUsedTime = new LSExpression[nMachines];
        for (int i = 0; i < nMachines; i++) {
            var workSequence = model.listVar(this.workslots.length);
            e_workSlotSeqs[i] = workSequence;
            e_workSlotCount[i] = model.count(workSequence);
            e_machineUsedTime[i] = model.sum();

            LSExpression l_changeover = model.lambdaFunction(index -> {
                // Function to access changeover time, equivalent to
                // d_changeoverTimes[workSequence[i-1].partId][workSequence[i].partId]
                var e_currentWorkSlot = model.at(workSequence, index);
                var e_currentItemId = model.at(d_partIds, e_currentWorkSlot);
                var e_prevWorkSlot = model.at(workSequence, model.sub(index, 1));
                var e_prevItemId = model.at(d_partIds, e_prevWorkSlot);
                return model.at(d_changeoverTimes, e_prevItemId, e_currentItemId);
            });

            e_changeoverPerMachine[i] = model.sum(model.range(1, e_workSlotCount[i]), l_changeover);
            o_totalChangeover.addOperand(e_changeoverPerMachine[i]);
        }
        e_a_workSlotSeqs = model.array(e_workSlotSeqs);

        var producedPartsExpression = model.createDoubleArrayExternalFunction(new ProducedPartsFormula());

        // For each part type and machine, calculate how many parts are produced of each type in each period
        for (int partId = 0; partId < nParts; partId++) {
            e_producedParts[partId] = model.call(producedPartsExpression, model.createConstant(partId), e_a_workSlotSeqs);
        }

        // Accumulate production for all periods
        for (int partId = 0; partId < nParts; partId++) {
            for (int periodId = 0; periodId < nPeriods; periodId++) {
                if (periodId == 0) {
                    e_accumulatedProducedParts[partId][periodId] = model.at(e_producedParts[partId], 0);
                } else {
                    e_accumulatedProducedParts[partId][periodId] = model.sum(e_accumulatedProducedParts[partId][periodId - 1], model.at(e_producedParts[partId], periodId));
                }
                var e_difference = model.sum(e_accumulatedProducedParts[partId][periodId], inventoryForecast[partId][periodId]);
                e_shortageParts[partId][periodId] = model.max(0, model.sub(0, e_difference));
                // Accumulate shortage for part i in period j to total shortage
                o_totalShortage.addOperand(e_shortageParts[partId][periodId]);
            }
        }

        // Capacity limit per machine
        for (int i = 0; i < this.nMachines; i++) {
            var workSeq = this.e_workSlotSeqs[i];
            var workSeqSize = this.e_workSlotCount[i];
            var l_workslotDuration = model.lambdaFunction(id -> {
                var e_workSlotId = model.at(workSeq, id);
                return model.at(d_durations, e_workSlotId);
            });
            var e_totalWorkSeqDuration = model.sum(model.range(0, workSeqSize), l_workslotDuration);
            e_machineUsedTime[i].addOperand(e_totalWorkSeqDuration);
            e_machineUsedTime[i].addOperand(this.e_changeoverPerMachine[i]);
            model.constraint(model.leq(e_machineUsedTime[i], totalMachineCapacity[i]));
        }
    }

    public CLSPSolution getSolution() {
        var solution = new CLSPSolution(instance);
        for (int i = 0; i < this.nMachines; i++) {
            var sequence = IterableUtils.toList(this.e_workSlotSeqs[i].getCollectionValue());
            for (var workslotId : sequence) {
                var workslot = this.workslots[toIntExact(workslotId)];
                solution.addWorkSlot(i, workslot.transform());
            }
        }
        solution.notifyUpdate();

        return solution;
    }

    public class ProducedPartsFormula implements LSDoubleArrayExternalFunction {

        @Override
        public double[] call(LSExternalArgumentValues lsExternalArgumentValues) {
            var itemId = (int) lsExternalArgumentValues.getIntValue(0);
            var sequences = lsExternalArgumentValues.getArrayValue(1);
            assert sequences.count() == nMachines;

            var partsProdPerPeriod = new double[nParts][nPeriods];
            for (int i = 0; i < nMachines; i++) {
                var workSeq = sequences.getArrayValue(i);
                var workSeqSize = workSeq.count();
                int currentPeriod = 0;
                double currentTime = 0;
                double capacity = machineCapacity[i][currentPeriod];
                int lastPartId = -1;
                for (int j = 0; j < workSeqSize; j++) {
                    var workSlotId = workSeq.getIntValue(j);
                    var workSlot = workslots[(int) workSlotId];
                    var duration = workSlot.duration();
                    var currentPartId = workSlot.partId();
                    var prodRate = productionRate[currentPartId][i];
                    var changeover = j == 0? 0: changeoverTimes[lastPartId][currentPartId];
                    currentTime += changeover;
                    if(currentTime >= capacity) {
                        var overtime = currentTime - capacity;
                        currentPeriod++;
                        currentTime = overtime;
                        if (currentPeriod >= nPeriods) {
                            break;
                        }
                    }
                    if (duration + currentTime > capacity) {
                        var remainingTime = capacity - currentTime;
                        var overtime = duration - remainingTime;
                        partsProdPerPeriod[currentPartId][currentPeriod] += prodRate * remainingTime;
                        currentPeriod++;
                        currentTime = overtime;
                        if (currentPeriod >= nPeriods) {
                            break;
                        }
                        partsProdPerPeriod[currentPartId][currentPeriod] += prodRate * overtime;
                    } else {
                        currentTime += duration;
                        partsProdPerPeriod[currentPartId][currentPeriod] += prodRate * duration;
                    }
                    lastPartId = currentPartId;
                }
            }

            // TODO future? find way to return complete matrix for performance, seems it is not supported right now
            return partsProdPerPeriod[itemId];
        }
    }
}
