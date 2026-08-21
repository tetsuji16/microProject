/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.resource;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.microproject.configuration.Configuration;
import com.microproject.strings.Messages;

class ResourceTypeConfigurationTest {
	@Test
	void resourceSheetOffersEverySupportedProjectResourceType() {
		Object[] options = Configuration.getFieldFromId("Field.resourceType").getOptions(null);
		assertTrue(Arrays.asList(options).contains(Messages.getString("ResourceType.Work")));
		assertTrue(Arrays.asList(options).contains(Messages.getString("ResourceType.Material")));
		assertTrue(Arrays.asList(options).contains(Messages.getString("ResourceType.Cost")));
	}
}
