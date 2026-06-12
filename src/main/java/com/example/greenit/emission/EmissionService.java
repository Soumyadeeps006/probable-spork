package com.example.greenit.emission;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service that obtains carbon intensity (kg CO₂e per kWh) for a given Azure region.
 * It queries the public API at https://cloud-carbon-footprint.org and caches the result for
 * 24 hours to avoid excessive network calls.
 */
@Service
public class EmissionService {

    private static final String API_URL = "https://api.cloud-carbon-footprint.org/v1/emissions";
    private final WebClient webClient;
    private final Map<String, CachedFactor> cache = new ConcurrentHashMap<>();

    public EmissionService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(API_URL).build();
    }

    /**
     * Returns carbon intensity for the specified region (kg CO₂e per kWh).
     */
    public Mono<Double> getCarbonIntensity(String region) {
        CachedFactor cached = cache.get(region);
        if (cached != null && !cached.isExpired()) {
            return Mono.just(cached.factor);
        }
        // Query the API: GET /v1/emissions?region={region}
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.queryParam("region", region).build())
                .retrieve()
                .bodyToMono(Response.class)
                .map(Response::getCarbonIntensity)
                .doOnNext(factor -> cache.put(region, new CachedFactor(factor)))
                .defaultIfEmpty(0.0);
    }

    // Simple POJO matching the JSON response (assumes field name "carbonIntensity").
    private static class Response {
        private double carbonIntensity;
        public double getCarbonIntensity() { return carbonIntensity; }
        public void setCarbonIntensity(double carbonIntensity) { this.carbonIntensity = carbonIntensity; }
    }

    // Wrapper to hold factor with timestamp.
    private static class CachedFactor {
        final double factor;
        final long timestamp;
        static final long TTL_MS = Duration.ofHours(24).toMillis();
        CachedFactor(double factor) { this.factor = factor; this.timestamp = System.currentTimeMillis(); }
        boolean isExpired() { return System.currentTimeMillis() - timestamp > TTL_MS; }
    }
}
