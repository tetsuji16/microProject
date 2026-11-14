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
import com.microproject.ribbon.RibbonActivation;
import com.microproject.ribbon.RibbonControlKind;
import com.microproject.ribbon.SwingRibbonModel;

/**
 * Ratchet between what a click on each ribbon command does today and the
 * {@code mspActivation} column of {@code msp-ribbon-control-spec.csv}.
 *
 * <p>The column states, per ribbon command, what a click on the control body
 * does in Microsoft Project: run at once, open a dialog, open a popup, keep a
 * pressed state, switch the view, enter a mode or open a Backstage page. A
 * command that behaves differently must be listed in
 * {@link #KNOWN_ACTIVATION_GAPS} with the behavior it has today, so the list can
 * only shrink. See {@code docs/msp-ribbon-control-kinds-ja.md}.</p>
 */
class MspRibbonActivationDiffTest {
	private static final String SPEC = "msp-ribbon-control-spec.csv";

	/**
	 * Commands whose click behavior is not yet the documented Microsoft Project
	 * behavior, mapped to the behavior they have today. Remove an entry in the
	 * change that implements the documented behavior; never add one to make a
	 * regression pass.
	 */
	private static final Map<String, RibbonActivation> KNOWN_ACTIVATION_GAPS = Map.ofEntries(
		Map.entry("RibbonTaskModeManual", RibbonActivation.COMMAND),
		Map.entry("RibbonTaskModeAutomatic", RibbonActivation.COMMAND),
		Map.entry("RibbonInactivateTask", RibbonActivation.COMMAND),
		Map.entry("RibbonPercentComplete0", RibbonActivation.COMMAND),
		Map.entry("RibbonPercentComplete25", RibbonActivation.COMMAND),
		Map.entry("RibbonPercentComplete50", RibbonActivation.COMMAND),
		Map.entry("RibbonPercentComplete75", RibbonActivation.COMMAND),
		Map.entry("RibbonPercentComplete100", RibbonActivation.COMMAND),
		Map.entry("RibbonChooseSort", RibbonActivation.DIALOG),
		Map.entry("RibbonChooseFilter", RibbonActivation.DIALOG),
		Map.entry("RibbonChooseGroup", RibbonActivation.DIALOG),
		Map.entry("RibbonTimeline", RibbonActivation.DIALOG),
		Map.entry("RibbonCalendarView", RibbonActivation.DIALOG));

	private record SpecRow(String ribbonId, RibbonControlKind mspKind, RibbonActivation mspActivation, String evidence) {
	}

	private static Map<String, SpecRow> spec;
	private static Map<String, RibbonActivation> implemented;

	@BeforeAll
	static void load() throws IOException {
		spec = readSpec();
		ExtToolBarFactory factory = new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonModel model = new SwingRibbonFactory(new MenuRibbonCommandSource(factory),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT)).createModel(MenuManager.STANDARD_RIBBON);
		implemented = new LinkedHashMap<>();
		for (SwingRibbonModel.RibbonTab tab : model.getTabs()) {
			for (SwingRibbonModel.RibbonBand band : tab.getBands()) {
				for (SwingRibbonModel.RibbonButton button : band.getButtons()) {
					implemented.putIfAbsent(button.getId(), button.getActivation());
				}
			}
		}
		// The quick access bar is modelled as plain command ids, so its entries run at once.
		for (String id : model.getTaskBarButtons()) {
			implemented.putIfAbsent(id, RibbonActivation.COMMAND);
		}
	}

	@Test
	void implementedActivationMatchesContractExceptKnownGaps() {
		List<String> problems = new ArrayList<>();
		for (SpecRow row : spec.values()) {
			RibbonActivation actual = implemented.get(row.ribbonId());
			if (actual == null) {
				continue;
			}
			RibbonActivation gap = KNOWN_ACTIVATION_GAPS.get(row.ribbonId());
			if (actual == row.mspActivation()) {
				if (gap != null) {
					problems.add(row.ribbonId() + " now is " + actual + ": remove it from KNOWN_ACTIVATION_GAPS");
				}
			} else if (gap == null) {
				problems.add(row.ribbonId() + " is " + actual + " but the contract says " + row.mspActivation());
			} else if (gap != actual) {
				problems.add(row.ribbonId() + " is " + actual + ", KNOWN_ACTIVATION_GAPS records " + gap
					+ ": update the entry or finish the change");
			}
		}
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	@Test
	void knownGapsNameExistingContractRows() {
		Set<String> unknown = new TreeSet<>(KNOWN_ACTIVATION_GAPS.keySet());
		unknown.removeAll(spec.keySet());
		assertTrue(unknown.isEmpty(), "KNOWN_ACTIVATION_GAPS entries without a contract row: " + unknown);
	}

	@Test
	void gapsAreNotBaselineRows() {
		for (String id : KNOWN_ACTIVATION_GAPS.keySet()) {
			SpecRow row = spec.get(id);
			assertTrue(row != null && !"BASELINE".equals(row.evidence()),
				id + ": a gap needs a Microsoft source (PENDING_VERIFY or DOCUMENTED), not BASELINE");
		}
	}

	/** The documented activation must be one the documented control kind can have. */
	@Test
	void contractActivationIsConsistentWithContractKind() {
		List<String> problems = new ArrayList<>();
		for (SpecRow row : spec.values()) {
			RibbonControlKind kind = row.mspKind();
			RibbonActivation activation = row.mspActivation();
			boolean popupOnly = kind.hasPopup() && !kind.invokesOnClick();
			if ((activation == RibbonActivation.POPUP) != popupOnly) {
				problems.add(row.ribbonId() + ": " + kind + " cannot be " + activation);
			}
			boolean pressable = kind.hasPressedState() || kind == RibbonControlKind.SPLIT_BUTTON;
			if (activation.keepsPressedState() && !pressable) {
				problems.add(row.ribbonId() + ": " + kind + " has no pressed state but the contract says " + activation);
			}
			if (kind.hasPressedState() && !activation.keepsPressedState()) {
				problems.add(row.ribbonId() + ": " + kind + " keeps a pressed state but the contract says " + activation);
			}
		}
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	private static Map<String, SpecRow> readSpec() throws IOException {
		Map<String, SpecRow> rows = new LinkedHashMap<>();
		try (InputStream stream = MspRibbonActivationDiffTest.class.getResourceAsStream(SPEC)) {
			assertNotNull(stream, SPEC + " is missing");
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
			assertEquals("ribbonId,mspKind,mspActivation,evidence,location,popupItems", reader.readLine());
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank()) {
					continue;
				}
				String[] cells = line.split(",", 6);
				assertEquals(6, cells.length, "Malformed row: " + line);
				rows.put(cells[0], new SpecRow(cells[0], RibbonControlKind.parse(cells[1]),
					RibbonActivation.parse(cells[2]), cells[3]));
			}
		}
		return rows;
	}
}
