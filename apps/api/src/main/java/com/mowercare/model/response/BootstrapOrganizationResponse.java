package com.mowercare.model.response;

import java.util.UUID;

public record BootstrapOrganizationResponse(UUID organizationId, UUID userId) {
}
