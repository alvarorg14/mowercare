package com.mowercare.organization;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mowercare.organization.Organization;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
}
