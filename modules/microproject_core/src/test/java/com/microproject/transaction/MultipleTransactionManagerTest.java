/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.document.Document;

class MultipleTransactionManagerTest {
	private static final Document DOCUMENT = (Document) Proxy.newProxyInstance(
		MultipleTransactionManagerTest.class.getClassLoader(), new Class<?>[] { Document.class },
		(proxy, method, arguments) -> null);

	@Test
	void listenersKeepRegistrationOrderAndRemovalDropsLastDuplicate() {
		MultipleTransactionManager manager = new MultipleTransactionManager();
		List<String> calls = new ArrayList<>();
		MultipleTransaction.Listener first = event -> calls.add("first");
		MultipleTransaction.Listener second = event -> calls.add("second");
		manager.addListener(first);
		manager.addListener(second);
		manager.addListener(first);

		manager.removeListener(first);
		manager.fire(DOCUMENT, 17, true);
		manager.fire(DOCUMENT, 17, false);

		assertEquals(List.of("first", "second", "first", "second"), calls);
	}

	@Test
	void listenerChangesDuringDispatchApplyOnNextEvent() {
		MultipleTransactionManager manager = new MultipleTransactionManager();
		List<String> calls = new ArrayList<>();
		MultipleTransaction.Listener second = event -> calls.add("second");
		manager.addListener(event -> {
			calls.add("first");
			manager.removeListener(second);
		});
		manager.addListener(second);

		manager.fire(DOCUMENT, 18, true);
		manager.fire(DOCUMENT, 18, false);

		assertEquals(List.of("first", "second", "first"), calls);
	}
}
