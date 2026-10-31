/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class CoreHelpersTest {
	@Test
	void identityHelpersCompareReferencesNotEquality() {
		String first = new String("same");
		String second = new String("same");
		List<String> values = new ArrayList<>();

		IdentityCollections.addIdentity(values, first);
		IdentityCollections.addIdentity(values, first);
		IdentityCollections.addIdentity(values, second);
		IdentityCollections.addIdentity(values, null);

		assertEquals(2, values.size());
		assertTrue(IdentityCollections.containsIdentity(values, second));
		assertFalse(IdentityCollections.containsIdentity(List.of(first), second));
	}

	@Test
	void markupEscapeHandlesNullAndAllReservedCharacters() {
		assertEquals("", MarkupEscape.escape(null));
		assertEquals("plain", MarkupEscape.escape("plain"));
		assertEquals("&amp;lt;a href=&quot;x&quot;&gt; &amp; &lt;b&gt;", MarkupEscape.escape("&lt;a href=\"x\"> & <b>"));
	}
}
