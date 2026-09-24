/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.configuration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CalculationPreferenceTest {
	@Test
	void projectPoliciesAreConstructorInitializedAndActiveInstanceIsStable() {
		CalculationPreference defaults = new CalculationPreference();

		assertFalse(defaults.isAssignmentDurationExcludesNonWorkPeriods());
		assertFalse(defaults.isNonWorkContourPeriodsStayFixedLength());
		assertTrue(CalculationPreference.MS_PROJECT.isAssignmentDurationExcludesNonWorkPeriods());
		assertTrue(CalculationPreference.MS_PROJECT.isNonWorkContourPeriodsStayFixedLength());
		assertSame(CalculationPreference.MS_PROJECT, CalculationPreference.getActive());
	}
}
