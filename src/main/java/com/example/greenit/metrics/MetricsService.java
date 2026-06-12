package com.example.greenit.metrics;

import com.example.greenit.cloud.CloudResource;
import com.example.greenit.cloud.Utilization;
import com.example.greenit.emission.EmissionService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Service that registers and updates Micrometer metrics for each cloud resource.
 * Metrics:
 *  - cpu_utilization (percentage)
 *  - memory_utilization (percentage)
 *  - carbon_intensity (kg CO₂e per kWh)
 *  - carbon_emission (kg CO₂e) computed as utilization * intensity
 *  - autoscale_actions_total (counter)
 */
@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;
    private final EmissionService emissionService;
    private final Map<String, Utilization> latestUtilization = new ConcurrentHashMap<>();

    public MetricsService(MeterRegistry meterRegistry, EmissionService emissionService) {
        this.meterRegistry = meterRegistry;
        this.emissionService = emissionService;
        // Register dynamic gauges that pull from latestUtilization map
        Gauge.builder("greenit.cpu.utilization", latestUtilization, map -> map.values().stream()
                .mapToDouble(u -> u.getCpuPercent())
                .average().orElse(0.0))
                .description("Average CPU utilization across monitored resources")
                .register(meterRegistry);
        Gauge.builder("greenit.memory.utilization", latestUtilization, map -> map.values().stream()
                .mapToDouble(u -> u.getMemoryPercent())
                .average().orElse(0.0))
                .description("Average memory utilization across monitored resources")
                .register(meterRegistry);
        Gauge.builder("greenit.carbon.emission", this, service -> service.calculateTotalCarbon())
                .description("Total carbon emission (kg CO₂e) across all resources")
                .register(meterRegistry);
    }

    /**
     * Update utilization for a specific resource and refresh related metrics.
     */
    public void updateUtilization(CloudResource resource, Utilization utilization) {
        latestUtilization.put(resource.getId(), utilization);
    }

    /**
     * Calculate total carbon emission based on the latest utilization and cached carbon intensity per region.
     */
    private double calculateTotalCarbon() {
        return latestUtilization.entrySet().stream()
                .map(entry -> {
                    Utilization u = entry.getValue();
                    String region = entry.getKey(); // assume resource ID encodes region for demo
                    // synchronous block for simplicity; in production use reactive composition
                    Double intensity = emissionService.getCarbonIntensity(region).blockOptional().orElse(0.0);
                    // Assume utilization provides power usage in kWh (placeholder value)
                    double kwh = u.getPowerKwh();
                    return kwh * intensity;
                })
                .mapToDouble(Double::doubleValue)
                .sum();
    }
}
