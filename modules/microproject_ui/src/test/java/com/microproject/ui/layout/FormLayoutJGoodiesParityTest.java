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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

import com.microproject.ui.dialog.util.FieldComponentMap;

/**
 * Compares {@link FormLayout} with JGoodies Forms 1.9.0, which the dialogs used before.
 * The same specification, components, constraints and container size must give the same
 * minimum and preferred sizes and the same component bounds. Dialog units are measured
 * with the real dialog font, as in the application.
 * <p>
 * Remove this class together with the JGoodies dependency (#1388); {@link FormLayoutTest}
 * keeps fixed expectations that were checked against JGoodies.
 */
class FormLayoutJGoodiesParityTest {
	private static final CellConstraints.Alignment[] OWN_H = { CellConstraints.DEFAULT, CellConstraints.FILL,
			CellConstraints.LEFT, CellConstraints.RIGHT, CellConstraints.CENTER };
	private static final CellConstraints.Alignment[] OWN_V = { CellConstraints.DEFAULT, CellConstraints.FILL,
			CellConstraints.TOP, CellConstraints.BOTTOM, CellConstraints.CENTER };
	private static final com.jgoodies.forms.layout.CellConstraints.Alignment[] JG_H = {
			com.jgoodies.forms.layout.CellConstraints.DEFAULT, com.jgoodies.forms.layout.CellConstraints.FILL,
			com.jgoodies.forms.layout.CellConstraints.LEFT, com.jgoodies.forms.layout.CellConstraints.RIGHT,
			com.jgoodies.forms.layout.CellConstraints.CENTER };
	private static final com.jgoodies.forms.layout.CellConstraints.Alignment[] JG_V = {
			com.jgoodies.forms.layout.CellConstraints.DEFAULT, com.jgoodies.forms.layout.CellConstraints.FILL,
			com.jgoodies.forms.layout.CellConstraints.TOP, com.jgoodies.forms.layout.CellConstraints.BOTTOM,
			com.jgoodies.forms.layout.CellConstraints.CENTER };

	@Test
	void repositorySpecificationsLayOutLikeJGoodies() {
		DialogUnits.setBaseUnitsForTesting(null);
		Random random = new Random(20261125L);
		int compared = 0;
		for (String columns : RepositoryFormSpecs.COLUMNS) {
			for (int pick = 0; pick < 3; pick++) {
				String rows = RepositoryFormSpecs.ROWS[random.nextInt(RepositoryFormSpecs.ROWS.length)];
				compared += compareRandomForms(columns, rows, random);
			}
		}
		for (String rows : RepositoryFormSpecs.ROWS) {
			for (int pick = 0; pick < 3; pick++) {
				String columns = RepositoryFormSpecs.COLUMNS[random.nextInt(RepositoryFormSpecs.COLUMNS.length)];
				compared += compareRandomForms(columns, rows, random);
			}
		}
		assertTrue(compared > 1000, "compared " + compared + " bounds");
	}

	@Test
	void bordersConvertDialogUnitsLikeJGoodies() {
		DialogUnits.setBaseUnitsForTesting(null);
		JPanel probe = new JPanel();
		for (String spec : new String[] { "9px,7px,9px,7px", "4dlu,4dlu,4dlu,4dlu", "1dlu, 12dlu, 7dlu, 3dlu" }) {
			Insets expected = com.jgoodies.forms.factories.Borders.createEmptyBorder(spec).getBorderInsets(probe);
			assertEquals(expected, FormBorders.createEmptyBorder(spec).getBorderInsets(probe), spec);
		}
	}

	@Test
	void builderPlacesComponentsLikeTheJGoodiesBasedBuilder() {
		DialogUnits.setBaseUnitsForTesting(null);
		String columns = "right:pref, 3dlu, fill:80dlu:grow, 3dlu, p";
		String rows = "p,3dlu,p,3dlu,p,3dlu,p,3dlu,p";
		com.jgoodies.forms.builder.DefaultFormBuilder old = new com.jgoodies.forms.builder.DefaultFormBuilder(
				new com.jgoodies.forms.layout.FormLayout(columns, rows));
		DefaultFormBuilder own = new DefaultFormBuilder(new FormLayout(columns, rows));
		List<Component[]> pairs = new ArrayList<>();
		old.setDefaultDialogBorder();
		own.setDefaultDialogBorder();
		pairs.add(new Component[] { old.append("Name:", new JTextField(10)), own.append("Name:", new JTextField(10)) });
		pairs.add(new Component[] { old.append(new JCheckBox("x")), own.append(new JCheckBox("x")) });
		old.nextLine(2);
		own.nextLine(2);
		pairs.add(new Component[] { old.addSeparator("Section").getParent(), own.addSeparator("Section").getParent() });
		old.nextLine(1);
		own.nextLine(1);
		pairs.add(new Component[] { old.addLabel("L"), own.addLabel("L") });
		pairs.add(new Component[] { old.addSpanning(new JTextField(5), old.getColumn(), old.getRow(), 3),
				own.addSpanning(new JTextField(5), own.getColumn(), own.getRow(), 3) });
		old.nextLine(2);
		own.nextLine(2);
		pairs.add(new Component[] { old.append("Wide", new JComboBox<>(new String[] { "aaaa" }), 3),
				own.append("Wide", new JComboBox<>(new String[] { "aaaa" }), 3) });
		assertEquals(old.getRow(), own.getRow());
		assertEquals(old.getColumn(), own.getColumn());
		assertPanelsMatch(old.getPanel(), own.getPanel(), pairs);
	}

	@Test
	void fieldComponentMapBuildsTheSameRowsWithBothBuilders() {
		DialogUnits.setBaseUnitsForTesting(null);
		String columns = "p, 3dlu, p, 3dlu, p, 3dlu, p";
		String rows = "p, 3dlu, p";
		com.jgoodies.forms.builder.DefaultFormBuilder old = new com.jgoodies.forms.builder.DefaultFormBuilder(
				new com.jgoodies.forms.layout.FormLayout(columns, rows));
		DefaultFormBuilder own = new DefaultFormBuilder(new FormLayout(columns, rows));
		FieldComponentMap oldMap = new FixedComponentMap();
		FieldComponentMap ownMap = new FixedComponentMap();
		List<Component[]> pairs = new ArrayList<>();
		pairs.add(new Component[] { oldMap.append(old, "Field.lag"), ownMap.append(own, "Field.lag") });
		old.nextLine(2);
		own.nextLine(2);
		pairs.add(new Component[] { oldMap.append(old, "Field.lag", 3), ownMap.append(own, "Field.lag", 3) });
		assertEquals(old.getRow(), own.getRow());
		assertEquals(old.getColumn(), own.getColumn());
		assertPanelsMatch(old.getPanel(), own.getPanel(), pairs);
	}

	/** Returns plain components so the comparison does not depend on a project. */
	static final class FixedComponentMap extends FieldComponentMap {
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

	private static void assertPanelsMatch(JPanel old, JPanel own, List<Component[]> pairs) {
		assertEquals(old.getComponentCount(), own.getComponentCount(), "component count");
		assertEquals(old.getPreferredSize(), own.getPreferredSize(), "preferred size");
		for (Dimension size : new Dimension[] { old.getPreferredSize(), new Dimension(900, 400), new Dimension(120, 60) }) {
			old.setSize(size);
			own.setSize(size);
			old.doLayout();
			own.doLayout();
			for (int index = 0; index < old.getComponentCount(); index++)
				assertEquals(old.getComponent(index).getBounds(), own.getComponent(index).getBounds(),
						"component " + index + " at " + size);
			for (Component[] pair : pairs)
				assertEquals(pair[0].getBounds(), pair[1].getBounds(), "returned component at " + size);
		}
	}

	private static int compareRandomForms(String columns, String rows, Random random) {
		int compared = 0;
		for (int round = 0; round < 2; round++) {
			com.jgoodies.forms.layout.FormLayout jgoodies = new com.jgoodies.forms.layout.FormLayout(columns, rows);
			FormLayout own = new FormLayout(columns, rows);
			assertEquals(jgoodies.getColumnCount(), own.getColumnCount(), columns);
			assertEquals(jgoodies.getRowCount(), own.getRowCount(), rows);
			int columnCount = own.getColumnCount();
			int rowCount = own.getRowCount();
			JPanel oldPanel = new JPanel(jgoodies);
			JPanel ownPanel = new JPanel(own);
			if (random.nextBoolean()) {
				oldPanel.setBorder(BorderFactory.createEmptyBorder(3, 5, 7, 9));
				ownPanel.setBorder(BorderFactory.createEmptyBorder(3, 5, 7, 9));
			}
			List<Component[]> pairs = new ArrayList<>();
			int count = 1 + random.nextInt(Math.min(12, columnCount * rowCount));
			for (int index = 0; index < count; index++) {
				int kind = random.nextInt(6);
				long seed = random.nextLong();
				JComponent oldComponent = component(kind, new Random(seed));
				JComponent ownComponent = component(kind, new Random(seed));
				int x = 1 + random.nextInt(columnCount);
				int y = 1 + random.nextInt(rowCount);
				int w = 1 + (random.nextInt(4) == 0 ? random.nextInt(columnCount - x + 1) : 0);
				int h = 1 + (random.nextInt(6) == 0 ? random.nextInt(rowCount - y + 1) : 0);
				int horizontal = random.nextInt(5);
				int vertical = random.nextInt(5);
				oldPanel.add(oldComponent, new com.jgoodies.forms.layout.CellConstraints().xywh(x, y, w, h,
						JG_H[horizontal], JG_V[vertical]));
				ownPanel.add(ownComponent, new CellConstraints().xywh(x, y, w, h, OWN_H[horizontal], OWN_V[vertical]));
				pairs.add(new Component[] { oldComponent, ownComponent });
			}
			String form = "\"" + columns + "\" / \"" + rows + "\"";
			Dimension preferred = oldPanel.getPreferredSize();
			Dimension minimum = oldPanel.getMinimumSize();
			assertEquals(preferred, ownPanel.getPreferredSize(), "preferred size of " + form);
			assertEquals(minimum, ownPanel.getMinimumSize(), "minimum size of " + form);
			Dimension[] sizes = { preferred, new Dimension(preferred.width + 137, preferred.height + 41),
					new Dimension((minimum.width + preferred.width) / 2, (minimum.height + preferred.height) / 2),
					new Dimension(Math.max(0, minimum.width - 13), Math.max(0, minimum.height - 7)) };
			for (Dimension size : sizes) {
				oldPanel.setSize(size);
				ownPanel.setSize(size);
				oldPanel.doLayout();
				ownPanel.doLayout();
				for (Component[] pair : pairs) {
					assertEquals(pair[0].getBounds(), pair[1].getBounds(), form + " at " + size);
					compared++;
				}
			}
		}
		return compared;
	}

	private static JComponent component(int kind, Random random) {
		switch (kind) {
			case 0:
				return sized(random, 80, 30, 200, 60, true);
			case 1:
				return new JLabel("Label " + random.nextInt(1000));
			case 2:
				return new JTextField(random.nextInt(20));
			case 3:
				return new JComboBox<>(new String[] { "alpha", "beta gamma" });
			case 4:
				return new JCheckBox("Check me");
			default:
				return sized(random, 300, 100, 400, 100, random.nextInt(6) != 0);
		}
	}

	private static JComponent sized(Random random, int minWidth, int minHeight, int prefWidth, int prefHeight,
			boolean visible) {
		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(random.nextInt(prefWidth), random.nextInt(prefHeight)));
		panel.setMinimumSize(new Dimension(random.nextInt(minWidth), random.nextInt(minHeight)));
		panel.setVisible(visible);
		return panel;
	}
}
