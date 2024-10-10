package es.urjc.etsii.grafo.CLSP.experiments;

import es.urjc.etsii.grafo.CLSP.model.CLSPConfig;
import es.urjc.etsii.grafo.CLSP.model.CLSPInstance;
import es.urjc.etsii.grafo.CLSP.model.CLSPSolution;
import es.urjc.etsii.grafo.algorithms.Algorithm;
import es.urjc.etsii.grafo.experiment.AbstractExperiment;

import java.util.ArrayList;
import java.util.List;

public class CustomAlgorithmExperiment extends AbstractExperiment<CLSPSolution, CLSPInstance> {


    private final CLSPConfig config;

    public CustomAlgorithmExperiment(CLSPConfig config) {
        // Any config class can be requested via the constructor
        this.config = config;
    }


    @Override
    public List<Algorithm<CLSPSolution, CLSPInstance>> getAlgorithms() {
        var algorithms = new ArrayList<Algorithm<CLSPSolution, CLSPInstance>>();

        String algorithmDescription = config.getAlgorithmDescription();

        CLSPAlgorithmBuilder builder = new CLSPAlgorithmBuilder();
        algorithms.add(builder.buildFromStringParams(algorithmDescription));

        return algorithms;
    }
}
