package com.mowercare.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationRecipientRulesTest {

	private static final UUID ACTOR = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID ADMIN_B = UUID.fromString("20000000-0000-0000-0000-000000000002");
	private static final UUID TECH = UUID.fromString("30000000-0000-0000-0000-000000000003");

	@Test
	@DisplayName("actor admin excluded; other admin and eligible assignee remain")
	void actorAdmin_excludesSelf_includesOtherAdminAndAssignee() {
		var out = NotificationRecipientRules.resolve(ACTOR, TECH, List.of(ACTOR, ADMIN_B));
		assertThat(out).containsExactlyInAnyOrder(ADMIN_B, TECH);
	}

	@Test
	@DisplayName("assignee same as actor is not added twice; actor stripped from admins")
	void assigneeEqualsActor_dedupes() {
		var out = NotificationRecipientRules.resolve(ACTOR, ACTOR, List.of(ACTOR, ADMIN_B));
		assertThat(out).containsExactly(ADMIN_B);
	}

	@Test
	@DisplayName("no assignee leaves only non-actor admins")
	void noAssignee_onlyNonActorAdmins() {
		var out = NotificationRecipientRules.resolve(ACTOR, null, List.of(ACTOR, ADMIN_B));
		assertThat(out).containsExactly(ADMIN_B);
	}
}
