package com.example.greenit.autoscale;

import com.example.greenit.cloud.CloudProvider;
import com.example.greenit.cloud.CloudResource;
import com.example.greenit.cloud.ScalingAction;
import com.example.greenit.cloud.Utilization;
import com.example.greenit.emission.EmissionService;
import com.example.greenit.metrics.MetricsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;

@Service
public class AutoscaleService {

    private final CloudProvider cloudProvider;
    private final EmissionService emissionService;
    private final MetricsService metricsService;

    // Thresholds – could be externalized to config
    private static final double CPU_UTILIZATION_THRESHOLD = 0.15; // 15%
    private static final Duration UNDERUTILIZATION_DURATION = Duration.ofMinutes(30);

    public AutoscaleService(CloudProvider cloudProvider,
                            EmissionService emissionService,
                            MetricsService metricsService) {
        this.cloudProvider = cloudProvider;
        this.emissionService = emissionService;
        this.metricsService = metricsService;
    }

    // Runs every minute
    @Scheduled(fixedDelayString = "${autoscale.poll-interval:60000}")
    public void pollResources() {
        List<CloudResource> resources = cloudProvider.listResources();
        Flux.fromIterable(resources)
            .parallel()
            .runOn(Schedulers.boundedElastic())
            .flatMap(resource -> {
                Utilization util = cloudProvider.getUtilization(resource);
                return emissionService.getCarbonIntensity(resource.getRegion())
                    .map(intensity -> new ResourceMetrics(resource, util, intensity));
            })
            .doOnNext(rm -> {
                // Record metrics
                metricsService.recordUtilization(rm.resource.getId(), rm.utilization.getCpuUsage(), rm.utilization.getMemoryUsage());
                double carbonKg = rm.utilization.getPowerKw() * rm.intensity; // kWh * kgCO2e/kWh
                metricsService.recordCarbon(rm.resource.getId(), carbonKg);
                // Persist to DB (omitted here – call a repository if needed)
                // Evaluate scaling
                if (rm.utilization.getCpuUsage() < CPU_UTILIZATION_THRESHOLD && rm.utilization.getUnderUtilizedDuration().compareTo(UNDERUTILIZATION_DURATION) >= 0) {
                    cloudProvider.scaleResource(rm.resource, ScalingAction.SCALE_DOWN);
                    metricsService.incrementScaleDown(rm.resource.getId());
                }
            })
            .subscribe();
    }

    private static class ResourceMetrics {
        final CloudResource resource;
        final Utilization utilization;
        final double intensity;
        ResourceMetrics(CloudResource resource, Utilization utilization, double intensity) {
            this.resource = resource;
            this.utilization = utilization;
            this.intensity = intensity;
        }
    }
}
