package es.urjc.etsii.grafo.CLSP.model;

public class WorkSlot {
    private int partId;
    private double duration;
    private double iniTime;
    private int iniPeriod;
    private double endTime;
    private int endPeriod;
    public WorkSlot(int partId, double duration) {
        this.partId = partId;
        this.duration = duration;
        this.iniTime = -1;
        this.iniPeriod = -1;
        this.endTime = -1;
        this.endPeriod = -1;
    }

    public WorkSlot(WorkSlot ws) {
        this.partId = ws.partId;
        this.duration = ws.duration;
        this.iniTime = ws.iniTime;
        this.iniPeriod = ws.iniPeriod;
        this.endTime = ws.endTime;
        this.endPeriod = ws.endPeriod;
    }

    public int getPartId() {
        return partId;
    }

    public void setPartId(int partId) {
        this.partId = partId;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public double getIniTime() {
        return iniTime;
    }

    public void setIniTime(double iniTime) {
        this.iniTime = iniTime;
    }

    public int getIniPeriod() {
        return iniPeriod;
    }

    public void setIniPeriod(int iniPeriod) {
        this.iniPeriod = iniPeriod;
    }

    public double getEndTime() {
        return endTime;
    }

    public void setEndTime(double endTime) {
        this.endTime = endTime;
    }

    public int getEndPeriod() {
        return endPeriod;
    }

    public void setEndPeriod(int endPeriod) {
        this.endPeriod = endPeriod;
    }

    @Override
    public String toString() {
        return "WorkSlot{" +
                "partId=" + partId +
                ", duration=" + duration +
                ", iniTime=" + iniTime +
                ", iniPeriod=" + iniPeriod +
                ", endTime=" + endTime +
                ", endPeriod=" + endPeriod +
                '}';
    }

    public boolean collapse(WorkSlot next) {
        if (this.partId == next.partId) {
            this.duration += next.duration;
            this.endTime = next.endTime;
            this.endPeriod = next.endPeriod;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkSlot workSlot = (WorkSlot) o;

        if (partId != workSlot.partId) return false;
        if (Double.compare(duration, workSlot.duration) != 0) return false;
        if (Double.compare(iniTime, workSlot.iniTime) != 0) return false;
        if (iniPeriod != workSlot.iniPeriod) return false;
        if (Double.compare(endTime, workSlot.endTime) != 0) return false;
        return endPeriod == workSlot.endPeriod;
    }

}
