package com.microproject.util;

import java.io.File;
import java.util.Locale;

public final class PdfExportUtil {
	private PdfExportUtil() {
	}

	public static File appendPdfExtensionIfMissing(File file) {
		if (file == null || file.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
			return file;
		}
		File parent = file.getParentFile();
		return parent == null
			? new File(file.getName() + ".pdf")
			: new File(parent, file.getName() + ".pdf");
	}
}
