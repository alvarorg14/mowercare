package com.mowercare.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.mowercare.common.EmailNormalization;
import com.mowercare.common.persistence.DataIntegrityViolations;
import com.mowercare.config.InviteProperties;
import com.mowercare.exception.InviteTokenInvalidException;
import com.mowercare.exception.LastAdminDeactivationException;
import com.mowercare.exception.LastAdminRemovalException;
import com.mowercare.exception.ResourceNotFoundException;
import com.mowercare.exception.UserDeactivatedManagementException;
import com.mowercare.exception.UserEmailConflictException;
import com.mowercare.model.AccountStatus;
import com.mowercare.model.Organization;
import com.mowercare.model.User;
import com.mowercare.model.UserRole;
import com.mowercare.model.request.CreateEmployeeUserRequest;
import com.mowercare.model.request.UpdateEmployeeUserRoleRequest;
import com.mowercare.model.response.AssignableUserResponse;
import com.mowercare.model.response.CreateEmployeeUserResponse;
import com.mowercare.model.response.EmployeeUserResponse;
import com.mowercare.repository.OrganizationRepository;
import com.mowercare.repository.RefreshTokenRepository;
import com.mowercare.repository.UserRepository;
import com.mowercare.security.RoleAuthorization;
import com.mowercare.security.TenantPathAuthorization;

@Service
public class OrganizationUserService {

	private final OrganizationRepository organizationRepository;
	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final OpaqueTokenService opaqueTokenService;
	private final InviteProperties inviteProperties;
	private final Clock clock;

	public OrganizationUserService(
			OrganizationRepository organizationRepository,
			UserRepository userRepository,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			OpaqueTokenService opaqueTokenService,
			InviteProperties inviteProperties,
			Clock clock) {
		this.organizationRepository = organizationRepository;
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.opaqueTokenService = opaqueTokenService;
		this.inviteProperties = inviteProperties;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<EmployeeUserResponse> listUsers(UUID organizationId, Jwt jwt) {
		TenantPathAuthorization.requireJwtOrganizationMatchesPath(organizationId, jwt);
		RoleAuthorization.requireAdmin(jwt);
		return userRepository.findByOrganization_IdOrderByEmailAsc(organizationId).stream()
				.map(OrganizationUserService::toListResponse)
				.toList();
	}

	/**
	 * Active employees only, email ascending. Excludes {@link AccountStatus#PENDING_INVITE} (cannot log in yet) and
	 * {@link AccountStatus#DEACTIVATED}. Admin and Technician may call (issue assignment).
	 */
	@Transactional(readOnly = true)
	public List<AssignableUserResponse> listAssignableUsers(UUID organizationId, Jwt jwt) {
		TenantPathAuthorization.requireJwtOrganizationMatchesPath(organizationId, jwt);
		RoleAuthorization.requireEmployee(jwt);
		return userRepository.findByOrganization_IdOrderByEmailAsc(organizationId).stream()
				.filter(u -> u.getAccountStatus() == AccountStatus.ACTIVE)
				.map(OrganizationUserService::toAssignableResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<EmployeeUserResponse> getUser(UUID organizationId, UUID userId, Jwt jwt) {
		TenantPathAuthorization.requireJwtOrganizationMatchesPath(organizationId, jwt);
		RoleAuthorization.requireAdmin(jwt);
		return userRepository.findByOrganization_IdAndId(organizationId, userId).map(OrganizationUserService::toListResponse);
	}

	@Transactional
	public EmployeeUserResponse updateUserRole(
			UUID organizationId, UUID userId, Jwt jwt, UpdateEmployeeUserRoleRequest request) {
		TenantPathAuthorization.requireJwtOrganizationMatchesPath(organizationId, jwt);
		RoleAuthorization.requireAdmin(jwt);
		User user = userRepository
				.findByOrganization_IdAndId(organizationId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found in this organization"));
		if (user.getAccountStatus() == AccountStatus.DEACTIVATED) {
			throw new UserDeactivatedManagementException();
		}
		UserRole newRole = request.role();
		if (user.getRole() == newRole) {
			return toListResponse(user);
		}
		if (user.getRole() == UserRole.ADMIN && newRole == UserRole.TECHNICIAN) {
			userRepository.lockByOrganizationIdAndRole(organizationId, UserRole.ADMIN);
			if (userRepository.countByOrganization_IdAndRoleAndAccountStatus(organizationId, UserRole.ADMIN, AccountStatus.ACTIVE)
					== 1) {
				throw new LastAdminRemovalException();
			}
		}
		user.updateRole(newRole);
		userRepository.save(user);
		return toListResponse(user);
	}

	@Transactional
	public EmployeeUserResponse deactivateUser(UUID organizationId, UUID userId, Jwt jwt) {
		TenantPathAuthorization.requireJwtOrganizationMatchesPath(organizationId, jwt);
		RoleAuthorization.requireAdmin(jwt);
		UUID actorId = UUID.fromString(jwt.getSubject());
		User user = userRepository
				.findByOrganization_IdAndId(organizationId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found in this organization"));
		if (user.getAccountStatus() == AccountStatus.DEACTIVATED) {
			return toListResponse(user);
		}
		if (user.getRole() == UserRole.ADMIN) {
			userRepository.lockByOrganizationIdAndRole(organizationId, UserRole.ADMIN);
			if (userRepository.countByOrganization_IdAndRoleAndAccountStatus(organizationId, UserRole.ADMIN, AccountStatus.ACTIVE)
					== 1) {
				throw new LastAdminDeactivationException();
			}
		}
		user.deactivate(actorId, clock.instant());
		refreshTokenRepository.deleteByUser_Id(user.getId());
		userRepository.save(user);
		return toListResponse(user);
	}

	@Transactional
	public CreateEmployeeUserResponse createUser(UUID organizationId, Jwt jwt, CreateEmployeeUserRequest request) {
		TenantPathAuthorization.requireJwtOrganizationMatchesPath(organizationId, jwt);
		RoleAuthorization.requireAdmin(jwt);
		Organization org = organizationRepository
				.findById(organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
		String email = EmailNormalization.normalize(request.email());
		if (userRepository.findByOrganization_IdAndEmail(organizationId, email).isPresent()) {
			throw new UserEmailConflictException();
		}
		boolean hasPassword = StringUtils.hasText(request.initialPassword());
		try {
			if (hasPassword) {
				User user = new User(
						org,
						email,
						passwordEncoder.encode(request.initialPassword()),
						request.role(),
						AccountStatus.ACTIVE,
						null,
						null);
				user = userRepository.save(user);
				return new CreateEmployeeUserResponse(
						user.getId(), user.getEmail(), user.getRole(), user.getAccountStatus(), null);
			}
			String placeholderHash = passwordEncoder.encode(opaqueTokenService.generateRawToken());
			String rawInvite = opaqueTokenService.generateRawToken();
			String inviteHash = opaqueTokenService.hashRawToken(rawInvite);
			Instant expires = clock.instant().plus(inviteProperties.tokenTtl());
			User user = new User(
					org,
					email,
					placeholderHash,
					request.role(),
					AccountStatus.PENDING_INVITE,
					inviteHash,
					expires);
			user = userRepository.save(user);
			return new CreateEmployeeUserResponse(
					user.getId(), user.getEmail(), user.getRole(), user.getAccountStatus(), rawInvite);
		} catch (DataIntegrityViolationException ex) {
			if (DataIntegrityViolations.isDuplicateOrgEmail(ex)) {
				throw new UserEmailConflictException();
			}
			throw ex;
		}
	}

	@Transactional
	public void acceptInvite(String rawToken, String newPassword) {
		String hash = opaqueTokenService.hashRawToken(rawToken);
		User user = userRepository.findByInviteTokenHash(hash).orElseThrow(InviteTokenInvalidException::new);
		Instant now = clock.instant();
		if (user.getAccountStatus() != AccountStatus.PENDING_INVITE) {
			throw new InviteTokenInvalidException();
		}
		if (user.getInviteExpiresAt() != null && now.isAfter(user.getInviteExpiresAt())) {
			throw new InviteTokenInvalidException();
		}
		user.activateFromInvite(passwordEncoder.encode(newPassword));
		userRepository.save(user);
	}

	private static EmployeeUserResponse toListResponse(User user) {
		return new EmployeeUserResponse(
				user.getId(), user.getEmail(), user.getRole(), user.getAccountStatus(), user.getCreatedAt());
	}

	private static AssignableUserResponse toAssignableResponse(User user) {
		return new AssignableUserResponse(user.getId(), user.getEmail(), user.getRole(), user.getAccountStatus());
	}

}
