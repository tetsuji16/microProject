/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.server.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CommonDataObjectEqualityTest {
	@Test
	void comparesUniqueIdsOnlyForOtherDataObjects() {
		CommonDataObject object = new CommonDataObject();
		object.setUniqueId(42L);
		CommonDataObject sameId = new CommonDataObject();
		sameId.setUniqueId(42L);
		CommonDataObject differentId = new CommonDataObject();
		differentId.setUniqueId(43L);

		assertTrue(object.equals(sameId));
		assertFalse(object.equals(differentId));
		assertFalse(object.equals("42"));
		assertFalse(object.equals(null));
	}
}
