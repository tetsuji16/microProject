/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.export;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.function.DoubleConsumer;
import javax.imageio.ImageIO;
import com.microproject.print.ExtendedPageFormat;
import com.microproject.print.GraphPageable;
import com.microproject.print.ViewPrintable;

final class PageablePngWriter {
	void write(GraphPageable pageable, File file, DoubleConsumer progress) throws Exception {
		pageable.update();
		if (pageable.getNumberOfPages() == 0) return;
		ViewPrintable printable = pageable.getSafePrintable();
		ExtendedPageFormat pageFormat = pageable.getSafePageFormat();
		BufferedImage image = new BufferedImage((int) pageFormat.getWidth(), (int) pageFormat.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try { graphics.setBackground(Color.WHITE); printable.print(graphics, 0); } finally { graphics.dispose(); }
		try (FileOutputStream output = new FileOutputStream(file)) {
			if (!ImageIO.write(image, "png", output)) throw new java.io.IOException("No PNG writer available");
		}
		progress.accept(0.9f);
	}
}
