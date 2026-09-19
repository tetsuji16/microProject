/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.export;

import java.awt.Component;
import java.io.IOException;
import com.microproject.print.GraphPageable;
import com.microproject.ui.privacy.PrivacyDisplayMode;

public class PDFExport {
	private final ExportFileChooser fileChooser;
	public PDFExport() { this(new SwingExportFileChooser()); }
	public PDFExport(ExportFileChooser fileChooser) { this.fileChooser = fileChooser; }
	public static void export(final GraphPageable pageable, Component parentComponent) throws IOException {
		new PDFExport().exportWithChooser(pageable, parentComponent);
	}
	void exportWithChooser(GraphPageable pageable, Component parentComponent) {
		ExportJobScheduler.scheduleIfSelected(
				fileChooser.choose(PrivacyDisplayMode.projectName(pageable.getRenderer().getProject()), parentComponent),
				target -> PageableExportJob.schedule(pageable, parentComponent, target, "PDF Export"));
	}
}
