package com.projectlibre1.contrib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ClassLoaderUtilsTest {
	@Test
	void comparesNumericVersionParts() {
		assertEquals(0, ClassLoaderUtils.compareJavaVersion("25", "25.0"));
		assertEquals(-1, ClassLoaderUtils.compareJavaVersion("1.8", "9"));
		assertEquals(1, ClassLoaderUtils.compareJavaVersion("17.0.10", "17.0.2"));
	}

	@Test
	void preservesLegacyOrderingForNonNumericParts() {
		assertEquals(1, ClassLoaderUtils.compareJavaVersion("1.8.0_202", "1.8"));
		assertEquals(-1, ClassLoaderUtils.compareJavaVersion("1.8", "1.8.0_202"));
	}

	@Test
	void rejectsNullVersionsExplicitly() {
		assertThrows(NullPointerException.class, () -> ClassLoaderUtils.compareJavaVersion(null, "25"));
		assertThrows(NullPointerException.class, () -> ClassLoaderUtils.compareJavaVersion("25", null));
	}
}
