package com.example.greenit.cloud;

import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import reactor.core.publisher.Flux;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Azure implementation of {@link CloudProvider}. Retrieves VM resources, obtains a simplistic
 * utilization metric (placeholder) and performs right‑size actions using the Azure SDK.
 */
public class AzureCloudProvider implements CloudProvider {
    private final AzureResourceManager azure;

    public AzureCloudProvider() {
        // Use DefaultAzureCredential which reads env vars AZURE_CLIENT_ID, etc.
        AzureProfile profile = new AzureProfile();
        this.azure = AzureResourceManager
                .authenticate(new DefaultAzureCredentialBuilder().build(), profile)
                .withDefaultSubscription();
    }

    @Override
    public Flux<CloudResource> listResources() {
        List<CloudResource> resources = azure.virtualMachines().list().stream()
                .map(vm -> new CloudResource(
                        vm.id(),
                        vm.name(),
                        vm.regionName(),
                        "VM",
                        vm.size().toString()))
                .collect(Collectors.toList());
        return Flux.fromIterable(resources);
    }

    @Override
    public Flux<Utilization> getUtilization(CloudResource resource) {
        // Placeholder: generate a random CPU utilization value. In a real implementation you would
        // query Azure Monitor metrics.
        double cpuUtil = Math.random() * 100.0;
        Utilization util = new Utilization(resource.getId(), cpuUtil);
        return Flux.just(util);
    }

    @Override
    public void rightsize(CloudResource resource, String targetSize) {
        // Simplified right‑size: deallocate (stop) VM if targetSize is "stop"; otherwise resize.
        VirtualMachine vm = azure.virtualMachines().getById(resource.getId());
        if ("stop".equalsIgnoreCase(targetSize)) {
            vm.deallocate();
        } else {
            vm.update().withSize(targetSize).apply();
        }
    }
}
