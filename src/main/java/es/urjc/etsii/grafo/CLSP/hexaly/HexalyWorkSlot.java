package es.urjc.etsii.grafo.CLSP.hexaly;

import es.urjc.etsii.grafo.CLSP.model.WorkSlot;

public record HexalyWorkSlot(int partId, double duration) {
    public WorkSlot transform() {
        return new WorkSlot(partId, duration);
    }
}
