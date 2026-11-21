/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import java.awt.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.microproject.ui.ribbon.RibbonCustomization;
import com.microproject.ui.ribbon.RibbonCustomizationFile;

/**
 * The Import/Export dropdown shared by the Customize Ribbon and Quick Access Toolbar pages. The file always holds both
 * the ribbon and the toolbar, so either page exports and replaces the working copies of both. An import changes the
 * working copies only when the whole file was accepted; the stored values change when the dialog is applied.
 */
final class CustomizationTransfer {
	/** Chooses the file to read or write; {@code null} means the user cancelled. */
	interface FileChoice {
		Path choose(Component parent, boolean forSave);
	}

	/** The file dialog. Tests replace it so that no native chooser opens. */
	static volatile FileChoice fileChoice = CustomizationTransfer::chooseWithDialog;

	private final Supplier<RibbonCustomization> ribbon;
	private final Supplier<List<String>> quickAccess;
	private final BiConsumer<RibbonCustomization, List<String>> replace;

	CustomizationTransfer(Supplier<RibbonCustomization> ribbon, Supplier<List<String>> quickAccess,
		BiConsumer<RibbonCustomization, List<String>> replace) {
		this.ribbon = ribbon;
		this.quickAccess = quickAccess;
		this.replace = replace;
	}

	/** Builds a dropdown button; {@code namePrefix} makes the component names unique per page. */
	JButton createButton(String namePrefix) {
		JButton button = new JButton(UsabilityStrings.text("ribbonTransfer.button") + " ▾");
		button.setName(namePrefix + "ImportExport");
		button.getAccessibleContext().setAccessibleName(UsabilityStrings.text("ribbonTransfer.button"));
		String mnemonic = UsabilityStrings.text("ribbonTransfer.button.mnemonic", "");
		if (!mnemonic.isEmpty()) button.setMnemonic(mnemonic.charAt(0));
		JMenuItem importItem = item(namePrefix + "Import", "ribbonTransfer.import");
		importItem.addActionListener(event -> importFrom(button));
		JMenuItem exportItem = item(namePrefix + "Export", "ribbonTransfer.export");
		exportItem.addActionListener(event -> exportTo(button));
		JPopupMenu menu = new JPopupMenu();
		menu.setName(namePrefix + "ImportExportMenu");
		menu.add(importItem);
		menu.add(exportItem);
		button.addActionListener(event -> menu.show(button, 0, button.getHeight()));
		return button;
	}

	private static JMenuItem item(String name, String textKey) {
		JMenuItem item = new JMenuItem(UsabilityStrings.text(textKey));
		item.setName(name);
		item.getAccessibleContext().setAccessibleName(item.getText());
		return item;
	}

	void exportTo(Component parent) {
		Path path = fileChoice.choose(parent, true);
		if (path == null) return;
		if (Files.exists(path) && JOptionPane.showConfirmDialog(parent,
			MessageFormat.format(UsabilityStrings.text("ribbonTransfer.confirmOverwrite"), path.getFileName()),
			UsabilityStrings.text("ribbonTransfer.title"), JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) return;
		try {
			RibbonCustomizationFile.write(path, ribbon.get(), quickAccess.get());
		} catch (IOException failure) {
			JOptionPane.showMessageDialog(parent,
				MessageFormat.format(UsabilityStrings.text("ribbonTransfer.exportFailed"), failure.getMessage()),
				UsabilityStrings.text("ribbonTransfer.title"), JOptionPane.ERROR_MESSAGE);
		}
	}

	void importFrom(Component parent) {
		Path path = fileChoice.choose(parent, false);
		if (path == null) return;
		RibbonCustomizationFile.Content content;
		try {
			content = RibbonCustomizationFile.read(path);
		} catch (RibbonCustomizationFile.RejectedException rejected) {
			String reason = UsabilityStrings.text("ribbonTransfer.reject." + rejected.reason().name(),
				rejected.getMessage());
			JOptionPane.showMessageDialog(parent,
				MessageFormat.format(UsabilityStrings.text("ribbonTransfer.importFailed"), reason),
				UsabilityStrings.text("ribbonTransfer.title"), JOptionPane.ERROR_MESSAGE);
			return;
		}
		replace.accept(content.customization(), content.quickAccess());
		if (content.droppedCommandIds() > 0) {
			JOptionPane.showMessageDialog(parent,
				MessageFormat.format(UsabilityStrings.text("ribbonTransfer.dropped"), content.droppedCommandIds()),
				UsabilityStrings.text("ribbonTransfer.title"), JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private static Path chooseWithDialog(Component parent, boolean forSave) {
		JFileChooser chooser = new JFileChooser();
		chooser.setName("ribbonTransferFileChooser");
		chooser.setAcceptAllFileFilterUsed(false);
		chooser.setFileFilter(new FileNameExtensionFilter(UsabilityStrings.text("ribbonTransfer.filter"),
			RibbonCustomizationFile.EXTENSION));
		chooser.setDialogTitle(UsabilityStrings.text(forSave ? "ribbonTransfer.export.title" : "ribbonTransfer.import.title"));
		Component owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
		int result = forSave ? chooser.showSaveDialog(owner) : chooser.showOpenDialog(owner);
		if (result != JFileChooser.APPROVE_OPTION) return null;
		Path path = chooser.getSelectedFile().toPath();
		String name = path.getFileName().toString();
		if (forSave && !name.toLowerCase(java.util.Locale.ROOT).endsWith("." + RibbonCustomizationFile.EXTENSION)) {
			path = path.resolveSibling(name + "." + RibbonCustomizationFile.EXTENSION);
		}
		return path;
	}
}
