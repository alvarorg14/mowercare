package com.mowercare.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mowercare.model.Organization;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
}
