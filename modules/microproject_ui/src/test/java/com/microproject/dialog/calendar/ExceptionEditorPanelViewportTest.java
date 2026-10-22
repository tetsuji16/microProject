/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.dialog.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class ExceptionEditorPanelViewportTest {
	@Test
	void detailsViewportFitsUsableScreenAndKeepsAllFieldsScrollable() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JPanel editor = new JPanel(null);
			editor.setPreferredSize(new Dimension(700, 1_200));
			JCheckBox finalField = new JCheckBox("Last recurring field");
			finalField.setBounds(10, 1_150, 200, 30);
			editor.add(finalField);

			JScrollPane pane = CalendarEditorViewport.createScrollPane(editor, new Dimension(900, 800));
			assertEquals(new Dimension(700, 640), pane.getPreferredSize());
			assertEquals(new Dimension(603, 320), CalendarEditorViewport.boundedSize(
				new Dimension(1_000, 1_200), new Dimension(683, 480)),
				"the 150% runner work area must bound both editor axes and leave room for the JOptionPane title and command row");
			assertSame(editor, pane.getViewport().getView());
			assertEquals(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, pane.getVerticalScrollBarPolicy());

			pane.setSize(500, 300);
			pane.doLayout();
			pane.getViewport().doLayout();
			assertTrue(editor.getHeight() >= 1_200);
			assertTrue(pane.getVerticalScrollBar().getMaximum()
				> pane.getVerticalScrollBar().getVisibleAmount(),
				"fields below the viewport must remain reachable by scrolling");

			JPanel wideEditor = new JPanel(null);
			wideEditor.setPreferredSize(new Dimension(1_000, 1_200));
			JScrollPane boundedPane = CalendarEditorViewport.createScrollPane(wideEditor, new Dimension(683, 480));
			assertEquals(new Dimension(603, 320), boundedPane.getPreferredSize());
			boundedPane.setSize(boundedPane.getPreferredSize());
			boundedPane.doLayout();
			boundedPane.getViewport().doLayout();
			assertTrue(boundedPane.getHorizontalScrollBar().getMaximum()
				> boundedPane.getHorizontalScrollBar().getVisibleAmount(),
				"content wider than the bounded viewport must remain horizontally reachable");
			assertTrue(boundedPane.getVerticalScrollBar().getMaximum()
				> boundedPane.getVerticalScrollBar().getVisibleAmount(),
				"content below the bounded viewport must remain vertically reachable");
		});
	}
}
