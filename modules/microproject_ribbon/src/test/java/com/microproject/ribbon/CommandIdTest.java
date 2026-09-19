/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CommandIdTest {
	@Test
	void legacyActionIdsRoundTripThroughTypedIdentifiers() {
		for (CommandId command : CommandId.values())
			assertEquals(command, CommandId.fromActionId(command.actionId()));
	}

	@Test
	void unknownOrNullLegacyIdsAreRejected() {
		assertThrows(IllegalArgumentException.class, () -> CommandId.fromActionId("NotACommand"));
		assertThrows(NullPointerException.class, () -> CommandId.fromActionId(null));
	}
}
