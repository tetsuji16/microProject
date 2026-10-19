/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.export;

import java.awt.Component;
import java.io.IOException;
import com.microproject.print.GraphPageable;
import com.microproject.ui.privacy.PrivacyDisplayMode;

public class ImageExport {
	private final ExportFileChooser fileChooser;
	public ImageExport() { this(new SwingExportFileChooser()); }
	public ImageExport(ExportFileChooser fileChooser) { this.fileChooser = fileChooser; }
	public static void export(final GraphPageable pageable, Component parentComponent) throws IOException {
		new ImageExport().exportWithChooser(pageable, parentComponent);
	}
	void exportWithChooser(GraphPageable pageable, Component parentComponent) {
		String projectName = pageable.getReportPrintable() == null
			? PrivacyDisplayMode.projectName(pageable.getRenderer().getProject())
			: pageable.getReportPrintable().model().projectName();
		ExportJobScheduler.scheduleIfSelected(
				fileChooser.choose(projectName, parentComponent),
				target -> PageableExportJob.schedule(pageable, parentComponent, target, "Image Export"), pageable::cleanUp);
	}
}
