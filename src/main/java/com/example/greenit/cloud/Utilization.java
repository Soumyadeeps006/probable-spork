package com.example.greenit.cloud;

/**
 * Represents utilization metrics for a cloud resource.
 */
public class Utilization {
    private final String resourceId;
    private final double cpuUtilization; // percentage 0-100

    public Utilization(String resourceId, double cpuUtilization) {
        this.resourceId = resourceId;
        this.cpuUtilization = cpuUtilization;
    }

    public String getResourceId() { return resourceId; }
    public double getCpuUtilization() { return cpuUtilization; }
}
