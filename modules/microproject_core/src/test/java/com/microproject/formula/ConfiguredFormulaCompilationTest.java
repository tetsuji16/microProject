/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.formula;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.node.transform.CommonTransformFactory;
import com.microproject.node.transform.TransformList;
import com.microproject.view.configuration.ActionListFactory;
import com.microproject.view.configuration.ActionLists;

/**
 * Every formula written in view.xml has to compile, so a Groovy upgrade cannot silently
 * break a filter, sorter, grouper or action list (issue #1373).
 */
class ConfiguredFormulaCompilationTest {
	/** The list names production code passes to {@link TransformList#getInstance(String)}. */
	private static final List<String> TRANSFORM_LISTS =
			List.of("hidden_filters", "report_filters", "user_filters", "user_sorters");

	@Test
	void everyConfiguredTransformCompiles() {
		List<String> broken = new ArrayList<>();
		List<String> missing = new ArrayList<>();
		int checked = 0;
		for (String name : TRANSFORM_LISTS) {
			TransformList list = TransformList.getInstance(name);
			if (list == null) {
				// view.xml defines no <filters id="Filters.report">; ConverterContext falls back to
				// user_filters, so there is no formula to compile for that name.
				missing.add(name);
				continue;
			}
			for (CommonTransformFactory factory : list.getFactories()) {
				// Entries without a formula text are built from a definition or are the no-op
				// None filter/sorter, so there is no Groovy source to compile.
				if (factory.getFormulaText() == null || factory.getFormulaText().isBlank()) continue;
				checked++;
				if (list.getTransform(factory.getId()) == null) broken.add(name + "/" + factory.getId());
			}
		}
		assertTrue(checked > 0, "no configured formula was found");
		assertTrue(broken.isEmpty(), "formulas that did not compile: " + broken);
		assertTrue(missing.size() < TRANSFORM_LISTS.size(), "no transform list was configured");
	}

	@Test
	void everyConfiguredActionListCompiles() {
		ActionLists lists = ActionLists.getInstance();
		assertNotNull(lists);
		assertTrue(lists.getFactories().size() > 1);
		List<String> broken = new ArrayList<>();
		for (ActionListFactory factory : lists.getFactories()) {
			if (lists.getActionList(factory.getId()) == null) broken.add(factory.getId());
		}
		assertTrue(broken.isEmpty(), "action lists that did not compile: " + broken);
	}
}
