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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FormSpecParsingTest {
	@Test
	void columnSpecificationsHaveAlignmentSizeAndResize() {
		List<FormSpec> specs = FormSpec.parseList("right:pref, 3dlu, fill:80dlu:grow, l:min, c:d:g(0.5), 20px:none, 2pt", true);
		assertEquals(7, specs.size());
		assertSpec(specs.get(0), FormSpec.Align.RIGHT, FormSpec.ComponentSize.PREFERRED, FormSpec.NO_GROW);
		assertSpec(specs.get(1), FormSpec.Align.FILL, new FormSpec.ConstantSize(3, FormSpec.Unit.DIALOG_UNITS_X), FormSpec.NO_GROW);
		assertSpec(specs.get(2), FormSpec.Align.FILL, new FormSpec.ConstantSize(80, FormSpec.Unit.DIALOG_UNITS_X), FormSpec.DEFAULT_GROW);
		assertSpec(specs.get(3), FormSpec.Align.LEFT, FormSpec.ComponentSize.MINIMUM, FormSpec.NO_GROW);
		assertSpec(specs.get(4), FormSpec.Align.CENTER, FormSpec.ComponentSize.DEFAULT, 0.5);
		assertSpec(specs.get(5), FormSpec.Align.FILL, new FormSpec.ConstantSize(20, FormSpec.Unit.PIXEL), FormSpec.NO_GROW);
		assertSpec(specs.get(6), FormSpec.Align.FILL, new FormSpec.ConstantSize(2, FormSpec.Unit.POINT), FormSpec.NO_GROW);
	}

	@Test
	void rowSpecificationsDefaultToCenterAndUseVerticalDialogUnits() {
		List<FormSpec> specs = FormSpec.parseList("p, top:3dlu, bottom:p, fill:150dlu:grow", false);
		assertSpec(specs.get(0), FormSpec.Align.CENTER, FormSpec.ComponentSize.PREFERRED, FormSpec.NO_GROW);
		assertSpec(specs.get(1), FormSpec.Align.TOP, new FormSpec.ConstantSize(3, FormSpec.Unit.DIALOG_UNITS_Y), FormSpec.NO_GROW);
		assertSpec(specs.get(2), FormSpec.Align.BOTTOM, FormSpec.ComponentSize.PREFERRED, FormSpec.NO_GROW);
		assertSpec(specs.get(3), FormSpec.Align.FILL, new FormSpec.ConstantSize(150, FormSpec.Unit.DIALOG_UNITS_Y), FormSpec.DEFAULT_GROW);
	}

	@Test
	void maxAndMinBoundAComponentSizeByAConstant() {
		FormSpec.BoundedSize lower = assertInstanceOf(FormSpec.BoundedSize.class, FormSpec.parse("max(50dlu;pref)", true).size);
		assertSame(FormSpec.ComponentSize.PREFERRED, lower.basis());
		assertEquals(new FormSpec.ConstantSize(50, FormSpec.Unit.DIALOG_UNITS_X), lower.lower());
		assertNull(lower.upper());
		FormSpec.BoundedSize upper = assertInstanceOf(FormSpec.BoundedSize.class, FormSpec.parse("min(pref;90dlu)", true).size);
		assertSame(FormSpec.ComponentSize.PREFERRED, upper.basis());
		assertNull(upper.lower());
		assertEquals(new FormSpec.ConstantSize(90, FormSpec.Unit.DIALOG_UNITS_X), upper.upper());
		FormSpec.BoundedSize bracket = assertInstanceOf(FormSpec.BoundedSize.class, FormSpec.parse("[20dlu,pref,80dlu]", true).size);
		assertSame(FormSpec.ComponentSize.PREFERRED, bracket.basis());
		assertEquals(1.0, FormSpec.parse("max(160dlu;pref):grow", true).resizeWeight);
	}

	@Test
	void blanksAndATrailingCommaAreIgnored() {
		assertEquals(3, new FormLayout(" p ,3dlu , default ", "p, 3dlu, ").getColumnCount());
		assertEquals(2, new FormLayout("p", "p, 3dlu, ").getRowCount());
		assertEquals(2, new FormLayout("p", "p,3dlu,").getRowCount());
		assertEquals(0, new FormLayout("p").getRowCount());
	}

	@ParameterizedTest
	@ValueSource(strings = { "p,,p", "fill", "10furlongs", "1.5dlu", "max(10dlu;20dlu)", "max(pref;min)", "p:grow:grow",
			"p:sometimes", "max(10dlu,pref", "p)", "[pref,min]" })
	void invalidSpecificationsAreRejected(String spec) {
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> new FormLayout(spec));
		assertTrue(error.getMessage().contains(spec), error.getMessage());
	}

	@Test
	void everySpecificationInTheRepositoryParses() {
		for (String columns : RepositoryFormSpecs.COLUMNS)
			assertTrue(new FormLayout(columns).getColumnCount() > 0, columns);
		for (String rows : RepositoryFormSpecs.ROWS)
			assertTrue(new FormLayout("p", rows).getRowCount() > 0, rows);
	}

	@Test
	void componentsOutsideTheGridAreRejectedWhenAdded() {
		JPanel panel = new JPanel(new FormLayout("p, 3dlu, p", "p"));
		assertThrows(IndexOutOfBoundsException.class, () -> panel.add(new JLabel(), new CellConstraints().xy(4, 1)));
		assertThrows(IndexOutOfBoundsException.class, () -> panel.add(new JLabel(), new CellConstraints().xyw(2, 1, 3)));
		assertThrows(IndexOutOfBoundsException.class, () -> panel.add(new JLabel(), new CellConstraints().xy(1, 2)));
		assertThrows(IndexOutOfBoundsException.class, () -> panel.add(new JLabel(), new CellConstraints().xy(0, 1)));
		assertThrows(IllegalArgumentException.class, () -> panel.add(new JLabel(), "1, 1"));
	}

	@Test
	void alignmentsMustMatchTheirAxis() {
		CellConstraints constraints = new CellConstraints();
		assertThrows(IllegalArgumentException.class, () -> constraints.xy(1, 1, CellConstraints.TOP, CellConstraints.DEFAULT));
		assertThrows(IllegalArgumentException.class, () -> constraints.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.LEFT));
	}

	private static void assertSpec(FormSpec spec, FormSpec.Align alignment, FormSpec.Size size, double weight) {
		assertEquals(alignment, spec.defaultAlignment);
		assertEquals(size, spec.size);
		assertEquals(weight, spec.resizeWeight);
	}
}
