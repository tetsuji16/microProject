/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.microproject.menu.ExtToolBarFactory;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.menu.MenuRibbonCommandSource;
import com.microproject.menu.testsupport.MenuDefinitionSupport;
import com.microproject.ribbon.SwingRibbonModel;

/**
 * Diffs the real standard ribbon against the Microsoft Project desktop command specification in
 * {@code msp-ribbon-spec.csv} (#769 / #453). The structure tests lock what exists; this test
 * locks what must exist, so a shortfall can only be tolerated through an explicit, reasoned
 * {@link #KNOWN_GAPS} entry that fails the build as soon as the command is implemented.
 */
class MspRibbonSpecDiffTest {
	private enum Status { REQUIRED, EXTENSION, EXCLUDED }

	private record Row(String tab, String group, String command, String ribbonId, Status status, String source) { }

	/** REQUIRED commands that the ribbon does not yet expose, each with the reason it is open. */
	private static final Map<String, String> KNOWN_GAPS = Map.ofEntries(
		Map.entry("RibbonTaskViewGantt", "Task > View dropdown absent; views are only on the View tab"),
		Map.entry("RibbonFont", "Task > Font group absent; Text Styles on the Format tab covers the dialog, Ctrl+Shift+F exists"),
		Map.entry("RibbonAddToTimeline", "no Add to Timeline command or Action exists yet"),
		Map.entry("RibbonFill", "FillDown Action and Ctrl+D exist but no ribbon button is wired"),
		Map.entry("RibbonLinksBetweenProjects", "no Links Between Projects dialog exists"),
		Map.entry("RibbonDefineWbs", "WBS code definition is deliberately not exposed until implemented (RibbonStructureTest)"),
		Map.entry("RibbonSpelling", "no spell checker (F7) exists"),
		Map.entry("RibbonNewWindow", "no New Window / Switch Windows commands exist"),
		Map.entry("RibbonOutline", "View > Data > Outline levels not wired to a ribbon command"),
		Map.entry("RibbonTables", "View > Data > Tables not wired to a ribbon command"),
		Map.entry("RibbonHighlight", "View > Data > Highlight filter not wired to a ribbon command"));

	@Test
	void everyRequiredCommandIsPresentUnlessAnExplicitKnownGapSaysOtherwise() throws IOException {
		Set<String> present = ribbonIds();
		List<String> missing = new ArrayList<>();
		for (Row row : spec()) {
			if (row.status() != Status.REQUIRED || present.contains(row.ribbonId()))
				continue;
			if (!KNOWN_GAPS.containsKey(row.ribbonId()))
				missing.add(row.tab() + " > " + row.group() + " > " + row.command() + " (" + row.ribbonId() + ")");
		}
		assertTrue(missing.isEmpty(), () -> "REQUIRED Microsoft commands absent from the ribbon: " + missing);
	}

	@Test
	void knownGapsAreRequiredRowsThatAreStillAbsent() throws IOException {
		Set<String> present = ribbonIds();
		Map<String, Row> required = new LinkedHashMap<>();
		for (Row row : spec())
			if (row.status() == Status.REQUIRED)
				required.put(row.ribbonId(), row);
		for (String gap : KNOWN_GAPS.keySet()) {
			assertTrue(required.containsKey(gap), () -> "KNOWN_GAPS entry is not a REQUIRED spec row: " + gap);
			assertTrue(!present.contains(gap), () -> "KNOWN_GAPS entry is implemented; remove it: " + gap);
		}
	}

	@Test
	void excludedCommandsAreAbsent() throws IOException {
		Set<String> present = ribbonIds();
		for (Row row : spec())
			if (row.status() == Status.EXCLUDED)
				assertTrue(!present.contains(row.ribbonId()), () -> "EXCLUDED command is advertised: " + row.ribbonId());
	}

	@Test
	void everyRibbonCommandIsClassifiedInTheSpecAndStatusesAreNotContradictory() throws IOException {
		Set<String> classified = new LinkedHashSet<>();
		for (Row row : spec()) {
			assertTrue(classified.add(row.ribbonId()), () -> "duplicate spec row: " + row.ribbonId());
			assertTrue(!row.source().isBlank(), () -> "spec row needs a source or reason: " + row.ribbonId());
		}
		Set<String> unclassified = new TreeSet<>(ribbonIds());
		unclassified.removeAll(classified);
		assertTrue(unclassified.isEmpty(), () -> "ribbon commands missing from msp-ribbon-spec.csv: " + unclassified);
	}

	@Test
	void extensionRowsStillExistInTheRibbon() throws IOException {
		Set<String> present = ribbonIds();
		for (Row row : spec())
			if (row.status() == Status.EXTENSION)
				assertTrue(present.contains(row.ribbonId()), () -> "stale EXTENSION row: " + row.ribbonId());
	}

	@Test
	void requiredRowsCiteAnInventorySourceId() throws IOException {
		Set<String> sources = new TreeSet<>();
		for (Row row : spec())
			if (row.status() == Status.REQUIRED)
				sources.add(row.source());
		for (String source : sources)
			assertTrue(source.matches("S\\d+[a-c]?"), () -> "REQUIRED rows must cite an inventory source id: " + source);
	}

	private static Set<String> ribbonIds() {
		ExtToolBarFactory buttons = new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
		SwingRibbonModel model = new SwingRibbonFactory(new MenuRibbonCommandSource(buttons),
			MenuDefinitionSupport.ribbonBundles(Locale.ROOT)).createModel(MenuManager.STANDARD_RIBBON);
		Set<String> ids = new LinkedHashSet<>(model.getTaskBarButtons());
		for (SwingRibbonModel.RibbonTab tab : model.getTabs())
			for (SwingRibbonModel.RibbonBand band : tab.getBands())
				for (SwingRibbonModel.RibbonButton button : band.getButtons())
					ids.add(button.getId());
		return ids;
	}

	private static List<Row> spec() throws IOException {
		List<Row> rows = new ArrayList<>();
		try (InputStream stream = MspRibbonSpecDiffTest.class.getResourceAsStream("msp-ribbon-spec.csv")) {
			assertTrue(stream != null, "msp-ribbon-spec.csv must be on the test classpath");
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
			String header = reader.readLine();
			assertEquals("tab,group,command,ribbonId,status,source", header);
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				if (line.isBlank())
					continue;
				String[] cells = line.split(",", 6);
				assertEquals(6, cells.length, "malformed spec row: " + line);
				rows.add(new Row(cells[0], cells[1], cells[2], cells[3], Status.valueOf(cells[4]), cells[5]));
			}
		}
		return rows;
	}
}
