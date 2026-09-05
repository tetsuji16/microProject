/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
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

import org.junit.jupiter.api.Test;

class VersionUtilsTest {
	@Test
	void comparesReleaseVersionsByNumericComponentsRegardlessOfComponentCount() {
		assertEquals(1, VersionUtils.compareVersions("v0.0.23.395", "0.0.23"));
		assertEquals(1, VersionUtils.compareVersions("0.0.10", "0.0.9"));
		assertEquals(0, VersionUtils.compareVersions("0.0.23", "v0.0.23.0"));
	}

	@Test
	void comparesLargeBuildNumbersWithoutIntegerOverflow() {
		assertEquals(1, VersionUtils.compareVersions("0.0.23.2147483648", "0.0.23.999"));
	}

	@Test
	void rejectsMalformedVersions() {
		assertEquals(0, VersionUtils.compareVersions("snapshot", "0.0.23"));
		assertEquals(0, VersionUtils.compareVersions("0.0.23.", "0.0.23"));
	}

	@Test
	void reportsTheGradleReleaseVersionFromTheProcessedResource() {
		assertEquals(System.getProperty("projectlibre.test.releaseVersion"), VersionUtils.getVersion());
	}
}
