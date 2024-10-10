package es.urjc.etsii.grafo.CLSP.hexaly;

import es.urjc.etsii.grafo.CLSP.model.CLSPInstance;
import es.urjc.etsii.grafo.CLSP.model.CLSPSolution;
import es.urjc.etsii.grafo.algorithms.Algorithm;
import localsolver.LocalSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class HexalyAlgorithm extends Algorithm<CLSPSolution, CLSPInstance> {

    private static final Logger log = LoggerFactory.getLogger(HexalyAlgorithm.class);

    private final long timelimit;
    private final TimeUnit unit;

    public HexalyAlgorithm(long timelimit, TimeUnit unit) {
        super("Hexaly");
        this.timelimit = timelimit;
        this.unit = unit;
    }

    @Override
    public CLSPSolution algorithm(CLSPInstance instance) {
        try (var localsolver = new LocalSolver()){
            var model = new CLSPModelFinal(instance, localsolver, true);
            model.solve(timelimit, unit);
            var solution = model.getSolution();
            solution.accumulateWeeklyProductionAndCalculatesScore(true);
            return solution;
        } catch (UnsatisfiedLinkError e){
            log.error("Failed to initialize LocalSolver (Hexaly), check that LocalSolver is properly installed and a valid license has been provided. Details: " + e.getMessage());
            System.exit(-1);
            return null; // Unreachable
        }
    }

}
