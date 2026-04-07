package com.mowercare.organization.response;

import java.util.UUID;

public record BootstrapOrganizationResponse(UUID organizationId, UUID userId) {
}
