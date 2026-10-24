/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.server.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProjectDataOfflineLockTest {
	@Test
	void serverLockMetadataDoesNotMakeAnOfflineProjectReadOnly() {
		ProjectData data = new ProjectData();
		data.setLockedById(42L);
		data.setIdleTime(0L);
		data.setAllowedIdleTime(60_000L);

		assertTrue(data.canBeUsed());
		assertEquals(42L, data.getLockedById());
		assertEquals(0L, data.getIdleTime());
		assertEquals(60_000L, data.getAllowedIdleTime());
	}
}
