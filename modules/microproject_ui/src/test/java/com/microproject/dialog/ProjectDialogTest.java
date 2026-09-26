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
		assertEquals("p,3dlu,p,3dlu,p,3dlu,p,3dlu,p", FlatUiSupport.preferredFormRows(5));
		assertThrows(IllegalArgumentException.class, () -> FlatUiSupport.preferredFormRows(0));
	}

	@Test
	void preferredRowsKeepEveryBuilderTargetAtItsPreferredHeight() {
		FormLayout layout = new FormLayout("default, 3dlu, 220dlu:grow", FlatUiSupport.preferredFormRows(3));
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

	@Test
	void nextLineTwoSkipsTheSpacerAndPlacesTheNextControlOnAPreferredTrack() {
		FormLayout layout = new FormLayout("default, 3dlu, 220dlu:grow", "p,3dlu,p");
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		JLabel first = new JLabel("開始日");
		JTextField second = new JTextField("2026-09-23");
		builder.append(first);
		builder.nextLine(2);
		builder.append(second);

		JPanel panel = builder.getPanel();
		panel.setSize(panel.getPreferredSize());
		panel.doLayout();

		assertEquals(1, layout.getConstraints(first).gridY);
		assertEquals(3, layout.getConstraints(second).gridY);
		assertAtLeastPreferredHeight(first);
		assertAtLeastPreferredHeight(second);
	}

	@Test
	void nextLineUsesTheRequestedGridRowCountForAllSupportedGaps() {
		FormLayout layout = new FormLayout("default",
			"p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p");
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		JLabel first = new JLabel("first");
		JLabel second = new JLabel("second");
		JLabel fourth = new JLabel("fourth");
		JLabel eighth = new JLabel("eighth");
		builder.append(first);
		builder.nextLine(2);
		builder.append(second);
		builder.nextLine(2);
		builder.nextLine(4);
		builder.append(fourth);
		builder.nextLine(8);
		builder.append(eighth);

		assertEquals(1, layout.getConstraints(first).gridY);
		assertEquals(3, layout.getConstraints(second).gridY);
		assertEquals(9, layout.getConstraints(fourth).gridY);
		assertEquals(17, layout.getConstraints(eighth).gridY);
	}

	@Test
	void nextLineZeroStillAdvancesOneGridRow() {
		FormLayout layout = new FormLayout("default", "p,p");
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		JLabel first = new JLabel("first");
		JLabel second = new JLabel("second");
		builder.append(first);
		builder.nextLine(0);
		builder.append(second);

		assertEquals(1, layout.getConstraints(first).gridY);
		assertEquals(2, layout.getConstraints(second).gridY);
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
