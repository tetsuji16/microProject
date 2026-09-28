/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class ListenerRegistryTest {
	@Test
	void preservesOrderAndRemovesLastEqualRegistration() {
		ListenerRegistry<String> registry = new ListenerRegistry<>();
		registry.add("first");
		registry.add("second");
		registry.add(new String("first"));

		registry.remove(new String("first"));

		assertEquals(List.of("first", "second"), registry.snapshot());
	}

	@Test
	void snapshotIsImmutableAndIgnoresNullListeners() {
		ListenerRegistry<String> registry = new ListenerRegistry<>();
		registry.add(null);
		registry.add("first");
		List<String> snapshot = registry.snapshot();

		registry.add("second");
		registry.remove(null);

		assertEquals(List.of("first"), snapshot);
		assertEquals(List.of("first", "second"), registry.snapshot());
		assertThrows(UnsupportedOperationException.class, () -> snapshot.add("third"));
	}
}
