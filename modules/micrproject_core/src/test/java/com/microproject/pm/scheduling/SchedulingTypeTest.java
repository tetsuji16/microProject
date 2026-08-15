package com.microproject.pm.scheduling;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SchedulingTypeTest {
	@Test
	void knownSchedulingTypesResolveToRules() {
		assertNotNull(SchedulingType.getSchedulingRuleInstance(SchedulingType.FIXED_UNITS));
		assertNotNull(SchedulingType.getSchedulingRuleInstance(SchedulingType.FIXED_DURATION));
		assertNotNull(SchedulingType.getSchedulingRuleInstance(SchedulingType.FIXED_WORK));
	}

	@Test
	void unknownSchedulingTypeFailsExplicitly() {
		assertThrows(IllegalArgumentException.class, () -> SchedulingType.getSchedulingRuleInstance(99));
	}
}
