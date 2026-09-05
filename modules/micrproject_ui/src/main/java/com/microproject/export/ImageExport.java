/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.export;

import java.awt.Component;
import java.io.IOException;
import com.microproject.print.GraphPageable;

public class ImageExport {
	private final ExportFileChooser fileChooser;
	public ImageExport() { this(new SwingExportFileChooser()); }
	public ImageExport(ExportFileChooser fileChooser) { this.fileChooser = fileChooser; }
	public static void export(final GraphPageable pageable, Component parentComponent) throws IOException {
		new ImageExport().exportWithChooser(pageable, parentComponent);
	}
	void exportWithChooser(GraphPageable pageable, Component parentComponent) {
		fileChooser.choose(pageable.getRenderer().getProject().getName(), parentComponent)
				.ifPresent(target -> PageableExportJob.schedule(pageable, parentComponent, target, "Image Export"));
	}
}
