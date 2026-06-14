package com.projectlibre1.util;

import java.io.File;

public final class PdfExportUtil {
	private PdfExportUtil() {
	}

	public static File appendPdfExtensionIfMissing(File file) {
		if (file == null || file.getName().endsWith(".pdf")) {
			return file;
		}
		File parent = file.getParentFile();
		return parent == null
			? new File(file.getName() + ".pdf")
			: new File(parent, file.getName() + ".pdf");
	}
}
