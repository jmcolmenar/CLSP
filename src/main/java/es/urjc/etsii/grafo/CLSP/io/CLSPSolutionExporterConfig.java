package es.urjc.etsii.grafo.CLSP.io;

import es.urjc.etsii.grafo.annotation.SerializerSource;
import es.urjc.etsii.grafo.io.serializers.AbstractSolutionSerializerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the solution serializer. Properties are loaded from the serializers.yml file, using the
 * node "serializers.solution-custom" by default.
 * First, loads the packaged serializers.yml inside the JAR.
 * Secondly, if there exists a serializers.yml file outside the JAR, override the properties defined in it.
 *
 * Several config properties are inherited, such as "frequency", "enabled", ...
 */
@SerializerSource
@ConfigurationProperties(prefix = "serializers.solution-custom")
public class CLSPSolutionExporterConfig extends AbstractSolutionSerializerConfig {

}
