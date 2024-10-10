package es.urjc.etsii.grafo.CLSP.experiments;

import es.urjc.etsii.grafo.CLSP.constructives.CLSPRandomConstructive;
import es.urjc.etsii.grafo.CLSP.constructives.grasp.CLSPListManagerEfficient;
import es.urjc.etsii.grafo.CLSP.improvers.LocalSearchBestImprovementCollapsing;
import es.urjc.etsii.grafo.CLSP.model.CLSPInstance;
import es.urjc.etsii.grafo.CLSP.model.CLSPSolution;
import es.urjc.etsii.grafo.CLSP.model.neighborhoods.EfficientExchangeNeighborhood;
import es.urjc.etsii.grafo.CLSP.model.neighborhoods.EfficientInsertNeighborhood;
import es.urjc.etsii.grafo.algorithms.Algorithm;
import es.urjc.etsii.grafo.algorithms.FMode;
import es.urjc.etsii.grafo.algorithms.SimpleAlgorithm;
import es.urjc.etsii.grafo.algorithms.VNS;
import es.urjc.etsii.grafo.autoconfig.irace.AlgorithmBuilder;
import es.urjc.etsii.grafo.autoconfig.irace.AlgorithmConfiguration;
import es.urjc.etsii.grafo.create.Constructive;
import es.urjc.etsii.grafo.create.grasp.GraspBuilder;
import es.urjc.etsii.grafo.improve.Improver;
import es.urjc.etsii.grafo.improve.VND;
import es.urjc.etsii.grafo.improve.ls.LocalSearchBestImprovement;
import es.urjc.etsii.grafo.shake.RandomMoveShake;
import es.urjc.etsii.grafo.solver.Mork;

import static es.urjc.etsii.grafo.algorithms.VNS.KMapper.STOPNOW;

public class CLSPAlgorithmBuilder extends AlgorithmBuilder<CLSPSolution, CLSPInstance> {

    @Override
    public Algorithm<CLSPSolution, CLSPInstance> buildFromConfig(AlgorithmConfiguration config) {

        // Always use a default value for each parameter

        // If no constructive is given, throw an exception
        String constructiveParam = config.getValue("constructive").orElseThrow();
        String alpha = config.getValue("alpha").orElse("None");
        Double alphaValue = Double.valueOf(config.getValue("alphaValue").orElse("-1"));

        // Constructive method
        Constructive<CLSPSolution, CLSPInstance> constructiveMethod;

        if (constructiveParam.equals("random")) {
            constructiveMethod = new CLSPRandomConstructive();
        } else {
            var graspListManager = new CLSPListManagerEfficient();
            // Add GRASP constructive methods to experiment
            // if the alpha parameter is not given --> random alpha in range [0,1] for each construction
            var graspBuilder = new GraspBuilder<CLSPListManagerEfficient.CLSPGRASPMove, CLSPSolution, CLSPInstance>()
                    .withMode(Mork.getFMode())   // Change FMode to either MAXIMIZE or MINIMIZE, can be different from problem f.o, for example when using a custom greedy function
                    .withListManager(graspListManager);

            if (constructiveParam.equals("graspgr")) {
                graspBuilder.withStrategyGreedyRandom();
            } else {
                if (constructiveParam.equals("grasprg")) {
                    graspBuilder.withStrategyRandomGreedy();
                } else {
                    throw new IllegalArgumentException("Unknown constructive method: " + constructiveParam);
                }
            }

            if (alpha.equals("random")) {
                constructiveMethod = graspBuilder.withAlphaRandom().build();
            } else if (alpha.equals("value")) {
                constructiveMethod = graspBuilder.withAlphaValue(alphaValue).build();
            } else {
                throw new IllegalArgumentException("Unknown alpha value: " + alpha);
            }

        }

        // Algorithm type:
        String algorithmType = config.getValue("algorithm").orElseThrow();

        // Improver type: collapseFirst, collapseSecond
        String improverType = config.getValue("lsOrder").orElseThrow();

        // Part of the algorithm's name
        String name = algorithmType + "_" + constructiveParam.substring(constructiveParam.length()-3) + "_" + alphaValue + "_" + improverType;

                switch (algorithmType) {
            case "grasp":
                var grasp = new SimpleAlgorithm<>(name,
                    constructiveMethod,
                    getImprover(improverType,false));
                return grasp;
            case "vns":
            case "gvns":
                // maxK depends on instance size: lambda expression
                Double maxKpct = Double.valueOf(config.getValue("maxKpct").orElseThrow());
                VNS.KMapper<CLSPSolution, CLSPInstance> maxK = (solution, k) -> k >= (maxKpct * solution.getInstance().getNumParts())? STOPNOW : k;
                var shake = new RandomMoveShake<>(1, new EfficientExchangeNeighborhood());
                if (algorithmType.equals("vns")) {
                    VNS<CLSPSolution, CLSPInstance> vns = new VNS<>(name,
                            maxK,
                            constructiveMethod,
                            shake,
                            getImprover(improverType,false)
                    );

                    return vns;
                } else {
                    VNS<CLSPSolution, CLSPInstance> gvns = new VNS<>(name,
                            maxK,
                            constructiveMethod,
                            shake,
                            getImprover(improverType,true)
                    );
                    return gvns;
                }
            default:
                throw new IllegalArgumentException("Unknown algorithm type: " + algorithmType);
        }

    }

    private Improver<CLSPSolution, CLSPInstance> getImprover(String improverType, boolean isVND) {
        switch (improverType) {
            case "collapseFirst":
                if (isVND) {
                    return VND.serial(FMode.MINIMIZE, new LocalSearchBestImprovementCollapsing(FMode.MINIMIZE, new EfficientInsertNeighborhood()),
                            new LocalSearchBestImprovement<>(FMode.MINIMIZE, new EfficientInsertNeighborhood()));
                } else {
                    return Improver.serial(FMode.MINIMIZE, new LocalSearchBestImprovementCollapsing(FMode.MINIMIZE, new EfficientInsertNeighborhood()),
                            new LocalSearchBestImprovement<>(FMode.MINIMIZE, new EfficientInsertNeighborhood()));
                }
            case "collapseSecond":
                if (isVND) {
                    return VND.serial(FMode.MINIMIZE, new LocalSearchBestImprovement<>(FMode.MINIMIZE, new EfficientInsertNeighborhood()),
                            new LocalSearchBestImprovementCollapsing(FMode.MINIMIZE, new EfficientInsertNeighborhood()));
                } else {
                    return Improver.serial(FMode.MINIMIZE, new LocalSearchBestImprovement<>(FMode.MINIMIZE, new EfficientInsertNeighborhood()),
                            new LocalSearchBestImprovementCollapsing(FMode.MINIMIZE, new EfficientInsertNeighborhood()));
                }
            default:
                throw new IllegalArgumentException("Unknown improver type: " + improverType);
        }
    }


}
