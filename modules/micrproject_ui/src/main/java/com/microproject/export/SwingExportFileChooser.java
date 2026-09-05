/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.export;

import java.awt.Component;
import java.io.File;
import java.util.Locale;
import java.util.Optional;
import com.formdev.flatlaf.util.SystemFileChooser;
import com.formdev.flatlaf.util.SystemFileChooser.FileNameExtensionFilter;
import com.microproject.util.PdfExportUtil;

public final class SwingExportFileChooser implements ExportFileChooser {
	private final SystemFileChooser chooser;
	private final FileNameExtensionFilter pdfFilter;
	private final FileNameExtensionFilter pngFilter;

	public SwingExportFileChooser() { this(new SystemFileChooser()); }
	SwingExportFileChooser(SystemFileChooser chooser) {
		this.chooser = chooser;
		chooser.setFileSelectionMode(SystemFileChooser.FILES_ONLY);
		chooser.setAcceptAllFileFilterUsed(true);
		pdfFilter = new FileNameExtensionFilter("PDF (*.pdf)", "pdf");
		pngFilter = new FileNameExtensionFilter("PNG (*.png)", "png");
		chooser.addChoosableFileFilter(pdfFilter);
		chooser.addChoosableFileFilter(pngFilter);
	}
	@Override public Optional<ExportTarget> choose(String projectName, Component parentComponent) {
		String baseName = projectName == null || projectName.isEmpty() ? "project" : projectName;
		chooser.setSelectedFile(new File(baseName + ".pdf"));
		chooser.setFileFilter(pdfFilter);
		if (chooser.showSaveDialog(parentComponent) != SystemFileChooser.APPROVE_OPTION) return Optional.empty();
		File selected = chooser.getSelectedFile();
		if (selected == null) return Optional.empty();
		ExportFormat format = selected.getName().toLowerCase(Locale.ROOT).endsWith(".png")
				? ExportFormat.PNG : chooser.getFileFilter() == pngFilter ? ExportFormat.PNG : ExportFormat.PDF;
		if (format == ExportFormat.PDF) {
			selected = PdfExportUtil.appendPdfExtensionIfMissing(selected);
		} else if (!selected.getName().toLowerCase(Locale.ROOT).endsWith(".png")) {
			File parent = selected.getParentFile();
			selected = parent == null ? new File(selected.getName() + ".png") : new File(parent, selected.getName() + ".png");
		}
		return Optional.of(new ExportTarget(selected, format));
	}
}
