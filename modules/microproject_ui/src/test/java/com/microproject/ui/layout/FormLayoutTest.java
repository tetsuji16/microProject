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
package com.microproject.ui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Fixed expectations for the layout algorithm. A horizontal dialog unit is 6.25 / 4 pixels
 * and a vertical one 13 / 8 pixels here. The expected values were computed with
 * JGoodies Forms 1.9.0 using the same base units, and match it exactly.
 */
class FormLayoutTest {
	private static final CellConstraints.Alignment D = CellConstraints.DEFAULT;

	@BeforeEach
	void fixDialogUnits() {
		DialogUnits.setBaseUnitsForTesting(new double[] { 6.25, 13.0 });
	}

	@AfterEach
	void restoreDialogUnits() {
		DialogUnits.setBaseUnitsForTesting(null);
	}

	@Test
	void growingColumnTakesFreeSpaceAndSpanningComponentWidensIt() {
		Form form = new Form("right:pref, 3dlu, fill:80dlu:grow", "p, 3dlu, p")
				.add(10, 10, 40, 20, 1, 1, 1, 1)
				.add(30, 12, 100, 24, 3, 1, 1, 1)
				.add(50, 20, 200, 30, 1, 3, 3, 1);
		form.assertSizes("200x59", "170x59");
		form.assertBounds(200, 59, "0,2,40,20 45,0,155,24 0,29,200,30");
		form.assertBounds(300, 109, "0,2,40,20 45,0,255,24 0,29,300,30");
		form.assertBounds(185, 59, "0,2,40,20 45,0,140,24 0,29,185,30");
		form.assertBounds(160, 54, "0,2,40,20 45,0,125,24 0,29,170,30");
	}

	@Test
	void defaultColumnsShrinkTowardsTheirMinimum() {
		Form form = new Form("default, 3dlu, default", "p")
				.add(20, 10, 90, 20, 1, 1, 1, 1)
				.add(40, 10, 120, 20, 3, 1, 1, 1);
		form.assertSizes("215x20", "65x20");
		form.assertBounds(215, 20, "0,0,90,20 95,0,120,20");
		form.assertBounds(315, 70, "0,0,90,20 95,0,120,20");
		form.assertBounds(140, 20, "0,0,55,20 60,0,80,20");
		form.assertBounds(55, 15, "0,0,20,20 25,0,40,20");
	}

	@Test
	void freeSpaceIsSharedByWeightAndBoundedSizesKeepTheirMinimum() {
		Form form = new Form("max(50dlu;pref), 3dlu, p:grow, 3dlu, p:grow(0.5)", "p")
				.add(5, 5, 20, 18, 1, 1, 1, 1)
				.add(5, 5, 33, 18, 3, 1, 1, 1)
				.add(5, 5, 17, 18, 5, 1, 1, 1);
		form.assertSizes("138x18", "138x18");
		form.assertBounds(138, 18, "0,0,78,18 83,0,33,18 121,0,17,18");
		form.assertBounds(238, 68, "0,0,78,18 83,0,100,18 188,0,50,18");
		form.assertBounds(128, 13, "0,0,78,18 83,0,33,18 121,0,17,18");
	}

	@Test
	void rowAlignmentsPlaceComponentsInTheirRows() {
		Form form = new Form("p", "top:p, fill:20dlu:grow, bottom:p, p")
				.add(5, 5, 60, 15, 1, 1, 1, 1)
				.add(5, 5, 60, 15, 1, 2, 1, 1)
				.add(5, 5, 60, 15, 1, 3, 1, 1)
				.add(5, 5, 40, 15, 1, 4, 1, 1, CellConstraints.CENTER, CellConstraints.CENTER);
		form.assertSizes("60x78", "60x78");
		form.assertBounds(60, 78, "0,0,60,15 0,15,60,33 0,48,60,15 10,63,40,15");
		form.assertBounds(160, 128, "0,0,60,15 0,15,60,83 0,98,60,15 10,113,40,15");
	}

	@Test
	void spanningComponentDoesNotWidenColumnsThatCannotGrow() {
		Form fixed = new Form("p, 3dlu, p", "p, p")
				.add(5, 5, 30, 10, 1, 1, 1, 1)
				.add(5, 5, 30, 10, 3, 1, 1, 1)
				.add(40, 8, 150, 12, 1, 2, 3, 1);
		fixed.assertSizes("65x22", "65x22");
		fixed.assertBounds(165, 72, "0,0,30,10 35,0,30,10 0,10,65,12");

		Form growing = new Form("p, 3dlu, p:grow", "p, p")
				.add(5, 5, 30, 10, 1, 1, 1, 1)
				.add(5, 5, 30, 10, 3, 1, 1, 1)
				.add(40, 8, 150, 12, 1, 2, 3, 1);
		growing.assertSizes("150x22", "65x22");
		growing.assertBounds(107, 22, "0,0,30,10 35,0,72,10 0,10,107,12");
	}

	@Test
	void columnAlignmentsAndInvisibleComponents() {
		Form form = new Form("left:min, 3dlu, center:pref, 3dlu, right:80px", "p")
				.add(25, 12, 70, 20, 1, 1, 1, 1)
				.add(5, 5, 30, 20, 3, 1, 1, 1, CellConstraints.LEFT, CellConstraints.TOP)
				.add(5, 5, 30, 20, 5, 1, 1, 1)
				.invisible(5, 5, 500, 200, 5, 1);
		form.assertSizes("145x20", "145x20");
		form.assertBounds(145, 20, "0,0,25,20 30,0,30,20 115,0,30,20 65,-90,80,200");
	}

	@Test
	void constraintsAreCopiedWhenAComponentIsAdded() {
		CellConstraints shared = new CellConstraints();
		JPanel panel = new JPanel(new FormLayout("10px, 20px", "10px"));
		JPanel first = sized(1, 1, 5, 5);
		panel.add(first, shared.xy(1, 1));
		panel.add(sized(1, 1, 5, 5), shared.xy(2, 1));
		panel.setSize(30, 10);
		panel.doLayout();
		assertEquals(0, first.getX());
	}

	@Test
	void containerInsetsOffsetTheGrid() {
		JPanel panel = new JPanel(new FormLayout("p", "p"));
		panel.setBorder(FormBorders.createEmptyBorder("4dlu, 4dlu, 2px, 3px"));
		JPanel child = sized(1, 1, 30, 10);
		panel.add(child, new CellConstraints().xy(1, 1));
		Insets insets = panel.getInsets();
		assertEquals(new Insets(7, 6, 2, 3), insets);
		assertEquals(new Dimension(39, 19), panel.getPreferredSize());
		panel.setSize(panel.getPreferredSize());
		panel.doLayout();
		assertEquals(new java.awt.Rectangle(6, 7, 30, 10), child.getBounds());
	}

	private static JPanel sized(int minWidth, int minHeight, int prefWidth, int prefHeight) {
		JPanel panel = new JPanel(null);
		panel.setMinimumSize(new Dimension(minWidth, minHeight));
		panel.setPreferredSize(new Dimension(prefWidth, prefHeight));
		return panel;
	}

	private static final class Form {
		private final JPanel panel;
		private final List<Component> components = new ArrayList<>();

		Form(String columns, String rows) {
			panel = new JPanel(new FormLayout(columns, rows));
		}

		Form add(int minWidth, int minHeight, int prefWidth, int prefHeight, int col, int row, int colSpan, int rowSpan) {
			return add(minWidth, minHeight, prefWidth, prefHeight, col, row, colSpan, rowSpan, D, D);
		}

		Form add(int minWidth, int minHeight, int prefWidth, int prefHeight, int col, int row, int colSpan, int rowSpan,
				CellConstraints.Alignment colAlign, CellConstraints.Alignment rowAlign) {
			JPanel component = sized(minWidth, minHeight, prefWidth, prefHeight);
			panel.add(component, new CellConstraints().xywh(col, row, colSpan, rowSpan, colAlign, rowAlign));
			components.add(component);
			return this;
		}

		Form invisible(int minWidth, int minHeight, int prefWidth, int prefHeight, int col, int row) {
			add(minWidth, minHeight, prefWidth, prefHeight, col, row, 1, 1);
			components.get(components.size() - 1).setVisible(false);
			return this;
		}

		void assertSizes(String preferred, String minimum) {
			assertEquals(preferred, text(panel.getPreferredSize()), "preferred size");
			assertEquals(minimum, text(panel.getMinimumSize()), "minimum size");
		}

		void assertBounds(int width, int height, String expected) {
			panel.setSize(width, height);
			panel.doLayout();
			StringBuilder actual = new StringBuilder();
			for (Component component : components) {
				if (actual.length() > 0)
					actual.append(' ');
				actual.append(component.getX()).append(',').append(component.getY()).append(',')
						.append(component.getWidth()).append(',').append(component.getHeight());
			}
			assertEquals(expected, actual.toString(), "bounds at " + width + "x" + height);
		}

		private static String text(Dimension size) {
			return size.width + "x" + size.height;
		}
	}
}
