/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.FlatUiSupport;

class ProjectDialogTest {
	@Test
	void preferredFormRowsUsesOnlyPreferredHeightTracks() {
		assertEquals("p,p,p,p,p", FlatUiSupport.preferredFormRows(5));
		assertThrows(IllegalArgumentException.class, () -> FlatUiSupport.preferredFormRows(0));
	}

	@Test
	void preferredRowsKeepEveryBuilderTargetAtItsPreferredHeight() {
		FormLayout layout = new FormLayout("default, 3dlu, 220dlu:grow", FlatUiSupport.preferredFormRows(5));
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		JLabel firstLabel = new JLabel("プロジェクト名:");
		JTextField firstField = new JTextField();
		JLabel secondLabel = new JLabel("担当者:");
		JTextField secondField = new JTextField();
		builder.append(firstLabel, firstField);
		builder.nextLine(2);
		builder.append(secondLabel, secondField);

		JPanel panel = builder.getPanel();
		panel.setSize(panel.getPreferredSize());
		panel.doLayout();

		assertAtLeastPreferredHeight(firstLabel);
		assertAtLeastPreferredHeight(firstField);
		assertAtLeastPreferredHeight(secondLabel);
		assertAtLeastPreferredHeight(secondField);
	}

	private static void assertAtLeastPreferredHeight(java.awt.Component component) {
		Dimension preferred = component.getPreferredSize();
		assertTrue(component.getHeight() >= preferred.height,
			() -> component.getClass().getSimpleName() + " height=" + component.getHeight()
				+ " preferred=" + preferred.height);
	}

	@Test
	void selectedSharedResourcePoolIsPreservedAndTheBlankChoiceMeansNoPool() {
		ResourcePool pool = ResourcePool.createRourcePool("shared", new DataFactoryUndoController());

		assertSame(pool, ProjectDialog.selectedResourcePool(pool));
		assertNull(ProjectDialog.selectedResourcePool(""));
		assertNull(ProjectDialog.selectedResourcePool(null));
	}
}
