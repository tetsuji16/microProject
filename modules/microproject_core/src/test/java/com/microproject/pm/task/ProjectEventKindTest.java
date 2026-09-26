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
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ProjectEventKindTest {
	@Test
	void typedKindsKeepStableCodesAndLegacyUnknownValues() {
		ProjectEvent nameChanged = new ProjectEvent(this, ProjectEvent.Kind.NAME_CHANGED, null, "old");
		assertEquals(1, nameChanged.getType());
		assertEquals(ProjectEvent.Kind.NAME_CHANGED, nameChanged.getKind());

		ProjectEvent legacy = new ProjectEvent(this, 99, null, null);
		assertEquals(99, legacy.getType());
		assertThrows(IllegalArgumentException.class, legacy::getKind);

		assertEquals(ProjectEvent.Kind.GROUP_DIRTY_CHANGED, ProjectEvent.Kind.fromCode(2));
	}
}
