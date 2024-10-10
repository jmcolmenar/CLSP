package es.urjc.etsii.grafo.CLSP.model;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "custom")
public class CLSPConfig {
    // Define any configuration property to automatically set from any source,
    // including the command line, the application.yml or the environment
    // See: https://mork-optimization.readthedocs.io/en/latest/features/config/ for more details


    private String gurobiTimeLimit;
    private String gurobiModelFile;

    private String algorithmDescription;

    private String mhTimeLimitInSeconds;

    public String getMhTimeLimitInSeconds() {
        return mhTimeLimitInSeconds;
    }

    public void setMhTimeLimitInSeconds(String mhTimeLimitInSeconds) {
        this.mhTimeLimitInSeconds = mhTimeLimitInSeconds;
    }

    public String getGurobiTimeLimit() {
        return gurobiTimeLimit;
    }

    public void setGurobiTimeLimit(String gurobiTimeLimit) {
        this.gurobiTimeLimit = gurobiTimeLimit;
    }

    public String getAlgorithmDescription() {
        return algorithmDescription;
    }

    public void setAlgorithmDescription(String algorithmDescription) {
        this.algorithmDescription = algorithmDescription;
    }

    public void setGurobiModelFile(String gurobiModelFile) {
        this.gurobiModelFile = gurobiModelFile;
    }

    public String getGurobiModelFile() {
        return gurobiModelFile;
    }
}
