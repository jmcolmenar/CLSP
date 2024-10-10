package es.urjc.etsii.grafo.CLSP.model;

import es.urjc.etsii.grafo.CLSP.hexaly.HexalyAlgorithm;
import es.urjc.etsii.grafo.algorithms.Algorithm;
import es.urjc.etsii.grafo.services.TimeLimitCalculator;
import es.urjc.etsii.grafo.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class TimeLimiter extends TimeLimitCalculator<CLSPSolution, CLSPInstance> {

    // Default time limit in milliseconds: 1 hour per iteration
    private static final int DEFAULT_TIME_LIMIT = 3600_000;
    private final int timeLimitInMilliseconds;

    private static final Logger log = LoggerFactory.getLogger(TimeLimiter.class);

    public TimeLimiter(CLSPConfig config) {
        super();
        // Gets time limit from configuration file
        if (config.getMhTimeLimitInSeconds() == null) {
            this.timeLimitInMilliseconds = DEFAULT_TIME_LIMIT;
            log.info("Default time limit per iteration for metaheuristics is {} seconds. Use custom.mh-time-limit-in-seconds property to customize it.", timeLimitInMilliseconds/1000);
        } else {
            // Converts seconds to milliseconds (Integer.parseInt() returns an int
            this.timeLimitInMilliseconds = Integer.parseInt(config.getMhTimeLimitInSeconds()) * 1000;
            log.info("Time limit per iteration for metaheuristics is set to {} seconds with custom.mh-time-limit-in-seconds property.", timeLimitInMilliseconds/1000);
        }

    }

    @Override
    public long timeLimitInMillis(CLSPInstance instance, Algorithm<CLSPSolution, CLSPInstance> algorithm) {
        if(algorithm instanceof HexalyAlgorithm){
            // LocalSolver has its own time limit configured inside the algorithm, defaults to 1 hour, return a huge value
            return TimeUtil.convert(1, TimeUnit.DAYS, TimeUnit.MILLISECONDS);
        }
        return timeLimitInMilliseconds;
    }
}
