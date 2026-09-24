/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.association;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.microproject.pm.dependency.HasDependencies;
import com.microproject.pm.task.NormalTask;

class AssociationFormatParametersTest {
	@Test
	void typedAssociationObjectAccessorPreservesLegacyAccessorIdentity() {
		HasDependencies task = new NormalTask();
		AssociationFormatParameters parameters = AssociationFormatParameters.getInstance(task, true, null, false, true);

		assertSame(task, parameters.getAssociationObject());
		assertSame(task, parameters.getThisObject());
	}
}
