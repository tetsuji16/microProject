package org.projectlibre.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;

import org.junit.jupiter.api.Test;

import com.projectlibre1.util.PdfExportUtil;

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
