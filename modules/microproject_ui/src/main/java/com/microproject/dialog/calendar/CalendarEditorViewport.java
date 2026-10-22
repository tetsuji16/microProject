/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.dialog.calendar;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/** Shared sizing for calendar detail editors hosted in confirmation dialogs. */
final class CalendarEditorViewport {
	private CalendarEditorViewport() { }

	static Dimension boundedSize(Dimension editorSize, Dimension usableSize) {
		int widthLimit = usableSize.width - 80;
		int heightLimit = usableSize.height - 160;
		if (widthLimit < 240) widthLimit = usableSize.width;
		if (heightLimit < 160) heightLimit = usableSize.height;
		return new Dimension(Math.min(editorSize.width, Math.max(1, widthLimit)),
			Math.min(editorSize.height, Math.max(1, heightLimit)));
	}

	static JScrollPane createScrollPane(JPanel editorPanel, Dimension usableSize) {
		JScrollPane pane = new JScrollPane(editorPanel);
		pane.setPreferredSize(boundedSize(editorPanel.getPreferredSize(), usableSize));
		return pane;
	}

	static Dimension usableScreenSize(Component component) {
		Window window = SwingUtilities.getWindowAncestor(component);
		GraphicsConfiguration configuration = window == null ? null : window.getGraphicsConfiguration();
		if (configuration == null)
			return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();
		Rectangle bounds = configuration.getBounds();
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
		return new Dimension(bounds.width - insets.left - insets.right,
			bounds.height - insets.top - insets.bottom);
	}
}
