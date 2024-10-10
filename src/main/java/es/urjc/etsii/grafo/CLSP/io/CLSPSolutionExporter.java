package es.urjc.etsii.grafo.CLSP.io;

import es.urjc.etsii.grafo.CLSP.model.CLSPInstance;
import es.urjc.etsii.grafo.CLSP.model.CLSPSolution;
import es.urjc.etsii.grafo.executors.WorkUnitResult;
import es.urjc.etsii.grafo.io.serializers.SolutionSerializer;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Provides a custom implementation for exporting solutions to disk.
 */
public class CLSPSolutionExporter extends SolutionSerializer<CLSPSolution, CLSPInstance> {

    /**
     * Create a new solution serializer with the given config
     *
     * @param config Common solution serializer configuration
     */
    protected CLSPSolutionExporter(CLSPSolutionExporterConfig config) {
        super(config);
    }

    @Override
    public void export(BufferedWriter writer, WorkUnitResult<CLSPSolution, CLSPInstance> result) throws IOException {
        writer.write(result.solution().toString());
    }
}
