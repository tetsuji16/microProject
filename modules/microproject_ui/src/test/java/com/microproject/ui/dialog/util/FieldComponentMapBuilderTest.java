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
package com.microproject.ui.dialog.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

import com.microproject.ui.layout.DefaultFormBuilder;
import com.microproject.ui.layout.FormLayout;

class FieldComponentMapBuilderTest {
	@Test
	void fieldRowsAdvanceTheBuilderAndSpanTheRequestedColumns() {
		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("p, 3dlu, p, 3dlu, p, 3dlu, p", "p, 3dlu, p"));
		FieldComponentMap map = new FixedComponentMap();

		JComponent single = map.append(builder, "Field.lag");
		assertNotNull(single, "Field.lag must exist in the configuration");
		assertEquals(5, builder.getColumn(), "label and field each take a component column and its gap");
		builder.nextLine(2);
		JComponent spanning = map.append(builder, "Field.lag", 3);
		assertEquals(8, builder.getColumn());
		assertEquals(4, builder.getPanel().getComponentCount());

		JPanel panel = builder.getPanel();
		panel.setSize(panel.getPreferredSize());
		panel.doLayout();
		assertEquals(single.getX(), spanning.getX(), "both fields start in column 3");
		JLabel secondLabel = (JLabel) panel.getComponent(2);
		assertEquals("Lag:", secondLabel.getText());
		assertEquals(0, secondLabel.getX());
	}

	private static final class FixedComponentMap extends FieldComponentMap {
		FixedComponentMap() {
			super(new Object());
		}

		@Override
		public JComponent getComponent(String fieldId, int flag) {
			return new JTextField(8);
		}

		@Override
		public String getLabel(String fieldId) {
			return "Lag";
		}
	}
}
