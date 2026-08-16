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
package com.microproject.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;

import org.junit.jupiter.api.Test;

import com.microproject.util.PdfExportUtil;

public class ImageExportTest {
	@Test
	public void keepsExistingPdfExtension() {
		File input = new File("C:\\temp\\report.pdf");

		File output = PdfExportUtil.appendPdfExtensionIfMissing(input);

		assertEquals(input, output);
	}

	@Test
	public void keepsExistingPdfExtensionRegardlessOfCase() {
		File input = new File("C:\\temp\\report.PDF");

		File output = PdfExportUtil.appendPdfExtensionIfMissing(input);

		assertEquals(input, output);
	}

	@Test
	public void appendsPdfExtensionWithoutDroppingParentDirectory() {
		File input = new File("C:\\temp\\report");

		File output = PdfExportUtil.appendPdfExtensionIfMissing(input);

		assertEquals(new File("C:\\temp\\report.pdf"), output);
	}

	@Test
	public void returnsNullWhenFileIsNull() {
		assertNull(PdfExportUtil.appendPdfExtensionIfMissing(null));
	}
}
