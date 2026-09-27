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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.microproject.field.Field;

class FieldDictionaryTest {
	@TempDir
	Path temporaryDirectory;

	@Test
	void generatedResourceFieldTableContainsResourceFields() throws IOException {
		FieldDictionary dictionary = FieldDictionary.getInstance();
		Set<String> projectFieldIds = dictionary.getProjectFields().stream()
				.map(Field::getIdWithoutPrefix)
				.collect(Collectors.toSet());
		String resourceOnlyFieldId = dictionary.getResourceFields().stream()
				.map(Field::getIdWithoutPrefix)
				.filter(fieldId -> !projectFieldIds.contains(fieldId))
				.findFirst()
				.orElseThrow();
		Path output = temporaryDirectory.resolve("fields.html");

		FieldDictionary.generateFieldDoc(output.toString());

		String html = Files.readString(output);
		int resourceSectionStart = html.indexOf("<b>Resource Fields</b>");
		int resourceSectionEnd = html.indexOf("</table>", resourceSectionStart);
		assertTrue(resourceSectionStart >= 0);
		assertTrue(resourceSectionEnd > resourceSectionStart);
		assertTrue(html.substring(resourceSectionStart, resourceSectionEnd).contains(resourceOnlyFieldId));
	}

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
			assertEquals("Remaining duration test alias", FieldDictionary.getAliasMap().get(field.getId()));
		} finally {
			HashMap<String, String> aliases = new HashMap<>();
			aliases.put(field.getId(), originalAlias);
			FieldDictionary.setAliasMap(aliases);
		}
	}
}
