package es.urjc.etsii.grafo.CLSP.exact;

import es.urjc.etsii.grafo.CLSP.model.CLSPConfig;
import es.urjc.etsii.grafo.CLSP.model.CLSPInstance;
import es.urjc.etsii.grafo.CLSP.model.CLSPSolution;
import es.urjc.etsii.grafo.CLSP.model.WorkSlot;
import es.urjc.etsii.grafo.create.Constructive;
import es.urjc.etsii.grafo.util.IOUtil;

import java.io.*;

public class CLSPExactModel extends Constructive<CLSPSolution, CLSPInstance> {


    private final CLSPConfig config;

    public CLSPExactModel(CLSPConfig config) {
        this.config = config;
    }

    @Override
    public CLSPSolution construct(CLSPSolution solution) {
        // IN --> Empty solution from solution(instance) constructor
        // OUT --> Feasible solution with an assigned score

        String modelFile = this.config.getGurobiModelFile();

        // Instance names
        String instanceName = solution.getInstance().getPath();
        String instanceNameNoDirectory = solution.getInstance().getPath().substring(solution.getInstance().getPath().lastIndexOf('/') + 1);
        String outputFileName = "solutions/" + instanceNameNoDirectory + "_exact_output.txt";
        File outputFile = new File(outputFileName);

        var pb = new ProcessBuilder().inheritIO();
        pb.redirectErrorStream(true);
        pb.redirectOutput(outputFile);

        String timeLimit = config.getGurobiTimeLimit();

        // Launch python script
        try {
            IOUtil.extractResource(modelFile, modelFile, IOUtil.isJAR(this.getClass()), true);
            pb.command("python3", "-m", "pip", "install", "gurobipy").start().waitFor();
            pb.command("python3", modelFile, instanceName,timeLimit).start().waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        processOutput(outputFile, solution);

        solution.setScore(solution.scoreCalculation(solution.getChangeoverTime(),solution.getShortageGurobi()));

        // Remember to call solution.notifyUpdate() if the solution is modified without using moves!!
        solution.notifyUpdate();
        return solution;
    }


    private void processOutput(File outputFile, CLSPSolution solution) {
        // Open file for reading and process output until reaching the solution
        try  {
            BufferedReader reader = new BufferedReader(new FileReader(outputFile));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(">>> Solution found")) {
                    // Process objective values
                    String[] parts = reader.readLine().split(":");
                    solution.setShortageGurobi(Double.parseDouble(parts[1]));
                    parts = reader.readLine().split(":");
                    solution.setChangeoverTime(Double.parseDouble(parts[1]));
                    parts = reader.readLine().split(":");
                    solution.setPriorityGurobi(Double.parseDouble(parts[1]));
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void processOutputToWorkSlots(File outputFile, CLSPSolution solution) {
        // TODO: translate to workslots. Some "fake" workslots could be needed to cover the holes.
        // Open file for reading and process output until reaching the solution
        try  {
            BufferedReader reader = new BufferedReader(new FileReader(outputFile));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(">>> Solution Schedule")) {
                    // Skip header
                    reader.readLine();
                    // Process solution: each line is "Line	Part	Priority	Hours	Produced_Parts	Period"
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split("\t");
                        WorkSlot ws = new WorkSlot(Integer.parseInt(parts[1]), Double.parseDouble(parts[3]));
                        solution.addWorkSlot(Integer.parseInt(parts[0]), ws);
                    }
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
