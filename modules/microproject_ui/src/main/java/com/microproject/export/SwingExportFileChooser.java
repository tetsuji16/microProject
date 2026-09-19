/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.export;

import java.awt.Component;
import java.io.File;
import java.util.Optional;
import com.formdev.flatlaf.util.SystemFileChooser;
import com.formdev.flatlaf.util.SystemFileChooser.FileNameExtensionFilter;

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
		pngFilter = new FileNameExtensionFilter("PNG (first page only, *.png)", "png");
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
		ExportFormat selectedFilterFormat = chooser.getFileFilter() == pngFilter ? ExportFormat.PNG
				: chooser.getFileFilter() == pdfFilter ? ExportFormat.PDF : ExportFormat.fromFileName(selected);
		return Optional.of(resolveTarget(selected, selectedFilterFormat));
	}

	static ExportTarget resolveTarget(File selected, ExportFormat selectedFilterFormat) {
		if (selected == null) return null;
		ExportFormat format = selectedFilterFormat == null ? ExportFormat.fromFileName(selected) : selectedFilterFormat;
		if (format == null) format = ExportFormat.PDF;
		return new ExportTarget(format.appendExtensionIfMissing(selected), format);
	}
}
