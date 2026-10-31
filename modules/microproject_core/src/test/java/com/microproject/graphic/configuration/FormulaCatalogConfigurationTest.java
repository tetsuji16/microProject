/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.graphic.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * ActionLists and CellStyles share FormulaCatalog.  The XML digester binds id/name/add* by
 * property name, so the real configuration must still populate both catalogs (issue #984 a).
 */
class FormulaCatalogConfigurationTest {
	@Test
	void realConfigurationPopulatesTheActionListCatalog() {
		ActionLists lists = ActionLists.getInstance();
		assertNotNull(lists);
		assertEquals("ActionLists.default", lists.getId());
		assertNotNull(lists.getName());
		assertTrue(lists.getFactories().size() > 1);
		ActionListFactory factory = lists.getFactory("ActionList.availability");
		assertNotNull(factory, "digester must bind addActionList");
		assertEquals("ActionList.availability", factory.getId());
		assertNotNull(lists.getActionList("ActionList.availability"));
		assertSame(lists.getActionList("ActionList.availability"), lists.getActionList("ActionList.availability"), "cached");
		assertNotNull(lists.getDefaultActionList());
	}

	@Test
	void realConfigurationPopulatesTheCellStyleCatalog() {
		CellStyles styles = CellStyles.getInstance();
		assertNotNull(styles);
		assertEquals("CellStyles.default", styles.getId());
		assertNotNull(styles.getName());
		assertNotNull(styles.getFactory("CellStyle.resources"), "digester must bind addStyle");
		assertNotNull(styles.getFactory("CellStyle.resources").getFormulaClass(), "digester must bind formulaClass");
		// the style classes live in microproject_ui, so only the factory binding is checked here
		assertTrue(styles.getFactories().size() > 1);
	}
}
