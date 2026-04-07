package com.mowercare.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class DataIntegrityViolationsTest {

	@Test
	@DisplayName("given constraint name matches uq users org email when check then true")
	void givenConstraintNameMatchesUqUsersOrgEmail_whenIsDuplicateOrgEmail_thenTrue() {
		ConstraintViolationException cve = mock(ConstraintViolationException.class);
		when(cve.getConstraintName()).thenReturn("uq_users_organization_id_email");
		DataIntegrityViolationException ex = new DataIntegrityViolationException("dup", cve);
		assertThat(DataIntegrityViolations.isDuplicateOrgEmail(ex)).isTrue();
	}

	@Test
	@DisplayName("given message contains constraint name when cause has no name then true")
	void givenMessageContainsConstraintName_whenCauseHasNoName_thenTrue() {
		ConstraintViolationException cve = mock(ConstraintViolationException.class);
		when(cve.getConstraintName()).thenReturn(null);
		when(cve.getMessage()).thenReturn("violates uq_users_organization_id_email");
		DataIntegrityViolationException ex = new DataIntegrityViolationException("dup", cve);
		assertThat(DataIntegrityViolations.isDuplicateOrgEmail(ex)).isTrue();
	}

	@Test
	@DisplayName("given constraint name differs only by case when check then true")
	void givenConstraintNameDiffersOnlyByCase_whenIsDuplicateOrgEmail_thenTrue() {
		ConstraintViolationException cve = mock(ConstraintViolationException.class);
		when(cve.getConstraintName()).thenReturn("UQ_USERS_ORGANIZATION_ID_EMAIL");
		DataIntegrityViolationException ex = new DataIntegrityViolationException("dup", cve);
		assertThat(DataIntegrityViolations.isDuplicateOrgEmail(ex)).isTrue();
	}

	@Test
	@DisplayName("given unrelated constraint when check then false")
	void givenUnrelatedConstraint_whenIsDuplicateOrgEmail_thenFalse() {
		ConstraintViolationException cve = mock(ConstraintViolationException.class);
		when(cve.getConstraintName()).thenReturn("other_constraint");
		when(cve.getMessage()).thenReturn("other");
		DataIntegrityViolationException ex = new DataIntegrityViolationException("dup", cve);
		assertThat(DataIntegrityViolations.isDuplicateOrgEmail(ex)).isFalse();
	}
}
