/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.export;

import java.awt.Graphics2D;
import java.io.File;
import java.io.FileOutputStream;
import java.util.function.DoubleConsumer;
import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;
import com.microproject.print.ExtendedPageFormat;
import com.microproject.print.GraphPageable;
import com.microproject.print.ViewPrintable;

final class PageablePdfWriter {
	void write(GraphPageable pageable, File file, DoubleConsumer progress) throws Exception {
		Document document = new Document();
		try (FileOutputStream output = new FileOutputStream(file)) {
			PdfWriter writer = PdfWriter.getInstance(document, output);
			try {
				pageable.update();
				int pageCount = pageable.getNumberOfPages();
				if (pageCount == 0) return;
				ViewPrintable printable = pageable.getSafePrintable();
				ExtendedPageFormat pageFormat = pageable.getSafePageFormat();
				float width = (float) pageFormat.getWidth(), height = (float) pageFormat.getHeight();
				float increment = 0.9f / pageCount;
				for (int page = 0; page < pageCount; page++) {
					progress.accept(0.1f + page * increment);
					document.setPageSize(new Rectangle(width, height));
					if (page == 0) document.open(); else document.newPage();
					Graphics2D graphics = writer.getDirectContent().createGraphics(width, height);
					try { printable.print(graphics, page); } finally { graphics.dispose(); }
				}
			} finally {
				if (document.isOpen()) document.close();
			}
		}
	}
}
