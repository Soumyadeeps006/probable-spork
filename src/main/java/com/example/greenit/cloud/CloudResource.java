package com.example.greenit.cloud;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple POJO representing a cloud resource (e.g., VM).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CloudResource {
    private String id;
    private String name;
    private String region;
    private String type; // e.g., "VM"
    private String size; // e.g., "Standard_B2s"
}
