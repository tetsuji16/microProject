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
import com.lowagie.text.pdf.DefaultFontMapper;
import com.microproject.reports.render.ReportFontProvider;
import com.microproject.print.ExtendedPageFormat;
import com.microproject.print.GraphPageable;
import com.microproject.print.ViewPrintable;

final class PageablePdfWriter {
	void write(GraphPageable pageable, File file, DoubleConsumer progress) throws Exception {
		pageable.update();
		int pageCount = pageable.getNumberOfPages();
		if (pageCount == 0) return;
		com.microproject.reports.render.ReportPrintable report = pageable.getReportPrintable();
		if (report != null) {
			java.awt.print.PageFormat pageFormat = pageable.getPageFormat(0);
			writePages(file, progress, pageCount, pageFormat.getWidth(), pageFormat.getHeight(),
				(page, graphics) -> report.print(graphics, pageFormat, page), true);
			return;
		}
		ViewPrintable printable = pageable.getSafePrintable();
		ExtendedPageFormat pageFormat = pageable.getSafePageFormat();
		writePages(file, progress, pageCount, pageFormat.getWidth(), pageFormat.getHeight(),
			(page, graphics) -> printable.print(graphics, page), false);
	}

	@FunctionalInterface private interface PagePainter {
		void paint(int page, Graphics2D graphics) throws Exception;
	}

	private void writePages(File file, DoubleConsumer progress, int pageCount, double pageWidth,
			double pageHeight, PagePainter painter, boolean embedJapaneseFont) throws Exception {
		Document document = new Document();
		DefaultFontMapper fontMapper = embedJapaneseFont ? embeddedJapaneseFontMapper() : null;
		try (FileOutputStream output = new FileOutputStream(file)) {
			PdfWriter writer = PdfWriter.getInstance(document, output);
			try {
				if (pageCount == 0) return;
				float width = (float) pageWidth, height = (float) pageHeight;
				float increment = 0.9f / pageCount;
				for (int page = 0; page < pageCount; page++) {
					progress.accept(0.1f + page * increment);
					document.setPageSize(new Rectangle(width, height));
					if (page == 0) document.open(); else document.newPage();
					Graphics2D graphics = fontMapper == null
						? writer.getDirectContent().createGraphics(width, height)
						: writer.getDirectContent().createGraphics(width, height, fontMapper);
					try { painter.paint(page, graphics); } finally { graphics.dispose(); }
				}
			} finally {
				if (document.isOpen()) document.close();
			}
		}
	}

	static DefaultFontMapper embeddedJapaneseFontMapper() {
		DefaultFontMapper mapper = new DefaultFontMapper();
		String[] styles = { "plain", "bold", "italic", "bolditalic" };
		byte[][] faces = { ReportFontProvider.regularBytes(), ReportFontProvider.boldBytes(),
			ReportFontProvider.regularBytes(), ReportFontProvider.boldBytes() };
		for (String family : new String[] { "Dialog", "SansSerif", "Serif", "Monospaced" }) {
			for (int index = 0; index < styles.length; index++)
				mapper.putName(family + "." + styles[index], fontParameters(faces[index], index == 1 || index == 3));
		}
		mapper.putName("Noto Sans JP", fontParameters(faces[0], false));
		mapper.putName("Noto Sans JP Bold", fontParameters(faces[1], true));
		return mapper;
	}

	private static DefaultFontMapper.BaseFontParameters fontParameters(byte[] fontBytes, boolean bold) {
		DefaultFontMapper.BaseFontParameters parameters = new DefaultFontMapper.BaseFontParameters(
			bold ? "NotoSansJP-Bold.otf" : "NotoSansJP-Regular.otf");
		parameters.encoding = com.lowagie.text.pdf.BaseFont.IDENTITY_H;
		parameters.embedded = true;
		parameters.cached = true;
		parameters.ttfAfm = fontBytes;
		return parameters;
	}
}
