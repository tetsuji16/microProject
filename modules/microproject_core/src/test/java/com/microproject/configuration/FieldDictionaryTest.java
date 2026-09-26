/*
 * MIT License
 *
 * Copyright (c) 2026 microProject
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 */
package com.microproject.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

import com.microproject.field.Field;

class FieldDictionaryTest {
	@Test
	void appliesAliasesForKnownFieldsAndIgnoresUnknownFields() {
		Field field = FieldDictionary.getInstance().getFieldFromId("Field.remainingDuration");
		assertNotNull(field);
		String originalAlias = field.getAlias();
		try {
			HashMap<String, String> aliases = new HashMap<>();
			aliases.put(field.getId(), "Remaining duration test alias");
			aliases.put("Field.unknown", "ignored");

			FieldDictionary.setAliasMap(aliases);

			assertEquals("Remaining duration test alias", field.getAlias());
		} finally {
			HashMap<String, String> aliases = new HashMap<>();
			aliases.put(field.getId(), originalAlias);
			FieldDictionary.setAliasMap(aliases);
		}
	}
}
