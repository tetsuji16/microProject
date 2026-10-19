/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.Test;

import com.microproject.util.FlatUiSupport;

class ComponentFactoryTextStateTest {
	@Test
	void deferredProgrammaticRefreshStaysUnmodifiedButLaterEditUsesAccent() throws Exception {
		JTextField[] field = new JTextField[1];
		SwingUtilities.invokeAndWait(() -> {
			field[0] = new JTextField();
			ComponentFactory.installTextStateTracker(field[0]);
			field[0].setText("loaded from model");
			ComponentFactory.markComponentAsUnmodified(field[0]);
		});
		SwingUtilities.invokeAndWait(() -> assertEquals(FlatUiSupport.infoForeground(), field[0].getForeground(),
			"deferred initial document events must not mark a model refresh as edited"));

		SwingUtilities.invokeAndWait(() -> field[0].setText("edited by user"));
		SwingUtilities.invokeAndWait(() -> assertEquals(FlatUiSupport.accentColor(), field[0].getForeground(),
			"a later committed document change must still be marked as edited"));

		JTextComponent[] verifiedField = new JTextComponent[1];
		ExtDateField[] dateField = new ExtDateField[1];
		SwingUtilities.invokeAndWait(() -> {
			dateField[0] = new ExtDateField();
			verifiedField[0] = dateField[0].getFormattedTextField();
			ComponentFactory.installTextStateTracker(verifiedField[0]);
			verifiedField[0].setText("model refresh");
			ComponentFactory.markComponentAsUnmodified(dateField[0]);
		});
		SwingUtilities.invokeAndWait(() -> assertEquals(FlatUiSupport.infoForeground(), verifiedField[0].getForeground(),
			"refreshing a wrapper must invalidate pending events on its verified text component"));
	}
}
