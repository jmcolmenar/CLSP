package es.urjc.etsii.grafo.CLSP.hexaly;

import es.urjc.etsii.grafo.CLSP.model.CLSPInstance;
import es.urjc.etsii.grafo.CLSP.model.CLSPSolution;
import es.urjc.etsii.grafo.algorithms.Algorithm;
import es.urjc.etsii.grafo.experiment.AbstractExperiment;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class HexalyExperiment extends AbstractExperiment<CLSPSolution, CLSPInstance> {
    @Override
    public List<Algorithm<CLSPSolution, CLSPInstance>> getAlgorithms() {
        return List.of(
                new HexalyAlgorithm(1, TimeUnit.HOURS)
        );
    }
}
