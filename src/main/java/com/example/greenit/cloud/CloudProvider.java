package com.example.greenit.cloud;

import java.util.List;

/**
 * Generic contract for cloud provider integrations.
 */
public interface CloudProvider {
    /**
     * Discover all relevant cloud resources that should be monitored.
     */
    List<CloudResource> listResources();

    /**
     * Retrieve current utilization metrics for a given resource.
     */
    Utilization getUtilization(CloudResource resource);

    /**
     * Perform an autoscaling action on the resource (e.g., resize, stop/start).
     */
    void scaleResource(CloudResource resource, ScalingAction action);
}
