package com.microproject.field;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FieldConverterTest {
	@Test
	void convertsUsingTypedRuntimeClassMetadata() throws FieldParseException {
		assertEquals(42, FieldConverter.convert("42", Integer.class, null));
		assertEquals(42, FieldConverter.fromString("42", Integer.class));
	}
}
