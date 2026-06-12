package com.example.greenit.controller;

import com.example.greenit.metrics.MetricsService;
import com.example.greenit.emission.EmissionService;
import com.example.greenit.cloud.CloudProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

@Controller
public class DashboardController {
    private final MetricsService metricsService;
    private final EmissionService emissionService;
    private final CloudProvider cloudProvider;

    public DashboardController(MetricsService metricsService, EmissionService emissionService, CloudProvider cloudProvider) {
        this.metricsService = metricsService;
        this.emissionService = emissionService;
        this.cloudProvider = cloudProvider;
    }

    @GetMapping("/dashboard")
    public Mono<String> dashboard(Model model) {
        // Gather utilization and carbon data for each resource
        return Mono.just(cloudProvider.listResources())
                .flatMapMany(resources -> Mono.justOrEmpty(resources))
                .flatMap(resource ->
                    Mono.zip(
                        Mono.just(resource),
                        Mono.justOrEmpty(metricsService.getCpuUtilization(resource.getId())),
                        emissionService.getCarbonIntensity(resource.getRegion())
                    )
                )
                .collectList()
                .map(dataList -> {
                    model.addAttribute("resources", dataList);
                    return "index"; // resolves to src/main/resources/templates/index.html
                });
    }
}
