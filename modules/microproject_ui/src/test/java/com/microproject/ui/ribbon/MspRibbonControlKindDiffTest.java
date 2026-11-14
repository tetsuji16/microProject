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
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.microproject.menu.ExtToolBarFactory;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.menu.MenuRibbonCommandSource;
import com.microproject.menu.testsupport.MenuDefinitionSupport;
import com.microproject.ribbon.RibbonControlKind;
import com.microproject.ribbon.SwingRibbonModel;

/**
 * Ratchet between the ribbon model and the control-type contract in
 * {@code msp-ribbon-control-spec.csv}.
 *
 * <p>The CSV states, per ribbon command, which Office control type Microsoft
 * Project uses for it. A command whose implemented kind differs must be listed
 * in {@link #KNOWN_GAPS} with the kind it has today. Implementing the documented
 * kind makes this test fail until the entry is removed, so the list can only
 * shrink. See {@code docs/msp-ribbon-control-kinds-ja.md}.</p>
 */
class MspRibbonControlKindDiffTest {
	private static final String SPEC = "msp-ribbon-control-spec.csv";
	private static final Set<String> EVIDENCE = Set.of("BASELINE", "PENDING_VERIFY", "DOCUMENTED");

	/**
	 * Commands whose control kind is not yet the documented Microsoft Project kind,
	 * mapped to the kind they have today. Remove an entry in the change that
	 * implements the documented kind; never add one to make a regression pass.
	 */
	private static final Map<String, RibbonControlKind> KNOWN_GAPS = Map.ofEntries(
		Map.entry("RibbonPaste", RibbonControlKind.BUTTON),
		Map.entry("RibbonCopy", RibbonControlKind.BUTTON),
		Map.entry("RibbonInsert", RibbonControlKind.BUTTON),
		Map.entry("RibbonFind", RibbonControlKind.BUTTON),
		Map.entry("RibbonClear", RibbonControlKind.BUTTON),
		Map.entry("RibbonInsertResource", RibbonControlKind.BUTTON),
		Map.entry("RibbonGridlines", RibbonControlKind.BUTTON),
		Map.entry("RibbonSaveBaseline", RibbonControlKind.SPLIT_BUTTON),
		Map.entry("RibbonChooseSort", RibbonControlKind.SPLIT_BUTTON),
		Map.entry("RibbonChooseFilter", RibbonControlKind.SPLIT_BUTTON),
		Map.entry("RibbonChooseGroup", RibbonControlKind.SPLIT_BUTTON),
		Map.entry("RibbonGantt", RibbonControlKind.TOGGLE_BUTTON),
		Map.entry("RibbonNetwork", RibbonControlKind.TOGGLE_BUTTON),
		Map.entry("RibbonResources", RibbonControlKind.TOGGLE_BUTTON),
		Map.entry("RibbonTaskUsageDetail", RibbonControlKind.TOGGLE_BUTTON),
		Map.entry("RibbonResourceUsageDetail", RibbonControlKind.TOGGLE_BUTTON),
		Map.entry("RibbonCalendarView", RibbonControlKind.BUTTON),
		Map.entry("RibbonTimeline", RibbonControlKind.BUTTON),
		Map.entry("RibbonDetails", RibbonControlKind.TOGGLE_BUTTON),
		Map.entry("RibbonTaskModeManual", RibbonControlKind.BUTTON),
		Map.entry("RibbonTaskModeAutomatic", RibbonControlKind.BUTTON),
		Map.entry("RibbonInactivateTask", RibbonControlKind.BUTTON),
		Map.entry("RibbonPercentComplete0", RibbonControlKind.BUTTON),
		Map.entry("RibbonPercentComplete25", RibbonControlKind.BUTTON),
		Map.entry("RibbonPercentComplete50", RibbonControlKind.BUTTON),
		Map.entry("RibbonPercentComplete75", RibbonControlKind.BUTTON),
		Map.entry("RibbonPercentComplete100", RibbonControlKind.BUTTON),
		Map.entry("RibbonTopBarUndo", RibbonControlKind.BUTTON),
		Map.entry("RibbonTopBarRedo", RibbonControlKind.BUTTON));

	private record SpecRow(String ribbonId, RibbonControlKind mspKind, String evidence) {
	}

	private static Map<String, SpecRow> spec;
	private static Map<String, SwingRibbonModel.RibbonButton> buttons;
	private static Map<String, RibbonControlKind> implemented;

	@BeforeAll
	static void load() throws IOException {
		spec = readSpec();
		ExtToolBarFactory factory = new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonModel model = new SwingRibbonFactory(new MenuRibbonCommandSource(factory),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT)).createModel(MenuManager.STANDARD_RIBBON);
		buttons = new LinkedHashMap<>();
		implemented = new LinkedHashMap<>();
		for (SwingRibbonModel.RibbonTab tab : model.getTabs()) {
			for (SwingRibbonModel.RibbonBand band : tab.getBands()) {
				for (SwingRibbonModel.RibbonButton button : band.getButtons()) {
					buttons.putIfAbsent(button.getId(), button);
					implemented.putIfAbsent(button.getId(), button.getControlKind());
				}
			}
		}
		// The quick access bar is modelled as plain command ids, so its entries are buttons.
		for (String id : model.getTaskBarButtons()) {
			implemented.putIfAbsent(id, RibbonControlKind.BUTTON);
		}
	}

	@Test
	void everyRibbonCommandHasAControlKindRow() {
		Set<String> missing = new TreeSet<>(implemented.keySet());
		missing.removeAll(spec.keySet());
		Set<String> stale = new TreeSet<>(spec.keySet());
		stale.removeAll(implemented.keySet());
		assertTrue(missing.isEmpty(), "Add a row to " + SPEC + " for: " + missing);
		assertTrue(stale.isEmpty(), "Remove rows of commands that left the ribbon from " + SPEC + ": " + stale);
	}

	@Test
	void implementedKindMatchesTheContractExceptKnownGaps() {
		List<String> problems = new ArrayList<>();
		for (SpecRow row : spec.values()) {
			RibbonControlKind actual = implemented.get(row.ribbonId());
			if (actual == null) {
				continue;
			}
			RibbonControlKind gap = KNOWN_GAPS.get(row.ribbonId());
			if (actual == row.mspKind()) {
				if (gap != null) {
					problems.add(row.ribbonId() + " now is " + actual + ": remove it from KNOWN_GAPS");
				}
			} else if (gap == null) {
				problems.add(row.ribbonId() + " is " + actual + " but the contract says " + row.mspKind());
			} else if (gap != actual) {
				problems.add(row.ribbonId() + " is " + actual + ", KNOWN_GAPS records " + gap
					+ " and the contract says " + row.mspKind());
			}
		}
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	@Test
	void knownGapsNameExistingContractRows() {
		Set<String> unknown = new TreeSet<>(KNOWN_GAPS.keySet());
		unknown.removeAll(spec.keySet());
		assertTrue(unknown.isEmpty(), "KNOWN_GAPS entries without a contract row: " + unknown);
	}

	@Test
	void conformingPopupControlsDeclareTheirItems() {
		List<String> empty = new ArrayList<>();
		for (SpecRow row : spec.values()) {
			SwingRibbonModel.RibbonButton button = buttons.get(row.ribbonId());
			if (button == null || KNOWN_GAPS.containsKey(row.ribbonId())) {
				continue;
			}
			if (button.getControlKind().hasPopup() && button.getItemIds().isEmpty()) {
				empty.add(row.ribbonId());
			}
		}
		assertTrue(empty.isEmpty(), "Popup controls without <id>.items: " + empty);
	}

	@Test
	void unverifiedRowsAreMarked() {
		for (SpecRow row : spec.values()) {
			assertTrue(EVIDENCE.contains(row.evidence()), row.ribbonId() + " has unknown evidence " + row.evidence());
			if (KNOWN_GAPS.containsKey(row.ribbonId())) {
				assertTrue(!"BASELINE".equals(row.evidence()),
					row.ribbonId() + ": a gap needs a Microsoft source (PENDING_VERIFY or DOCUMENTED), not BASELINE");
			}
		}
	}

	private static Map<String, SpecRow> readSpec() throws IOException {
		Map<String, SpecRow> rows = new LinkedHashMap<>();
		try (InputStream stream = MspRibbonControlKindDiffTest.class.getResourceAsStream(SPEC)) {
			assertNotNull(stream, SPEC + " is missing");
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
			String header = reader.readLine();
			assertEquals("ribbonId,mspKind,mspActivation,evidence,location,popupItems", header);
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank()) {
					continue;
				}
				String[] cells = line.split(",", 6);
				assertEquals(6, cells.length, "Malformed row: " + line);
				SpecRow previous = rows.put(cells[0], new SpecRow(cells[0], RibbonControlKind.parse(cells[1]), cells[3]));
				assertTrue(previous == null, "Duplicate row: " + cells[0]);
			}
		}
		return rows;
	}
}
