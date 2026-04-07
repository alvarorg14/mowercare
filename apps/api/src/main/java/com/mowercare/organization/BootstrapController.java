package com.mowercare.organization;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mowercare.organization.request.BootstrapOrganizationRequest;
import com.mowercare.organization.response.BootstrapOrganizationResponse;
import com.mowercare.organization.BootstrapService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/bootstrap")
public class BootstrapController {

	private final BootstrapService bootstrapService;

	public BootstrapController(BootstrapService bootstrapService) {
		this.bootstrapService = bootstrapService;
	}

	@PostMapping("/organization")
	public ResponseEntity<BootstrapOrganizationResponse> bootstrapOrganization(
			@RequestHeader(value = "X-Bootstrap-Token", required = false) String bootstrapToken,
			@Valid @RequestBody BootstrapOrganizationRequest body) {
		BootstrapOrganizationResponse response = bootstrapService.bootstrapOrganization(
				bootstrapToken,
				body.organizationName(),
				body.adminEmail(),
				body.adminPassword());
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
