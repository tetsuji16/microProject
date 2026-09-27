/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.util;

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
	void preservesOrderingForLegacyUpdateParts() {
		assertEquals(1, ClassLoaderUtils.compareJavaVersion("1.8.0_202", "1.8"));
		assertEquals(-1, ClassLoaderUtils.compareJavaVersion("1.8", "1.8.0_202"));
	}

	@Test
	void rejectsNullVersions() {
		assertThrows(NullPointerException.class, () -> ClassLoaderUtils.compareJavaVersion(null, "25"));
		assertThrows(NullPointerException.class, () -> ClassLoaderUtils.compareJavaVersion("25", null));
	}
}
