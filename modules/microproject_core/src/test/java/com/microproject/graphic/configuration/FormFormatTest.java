/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.graphic.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.StringReader;

import org.apache.commons.digester.Digester;
import org.junit.jupiter.api.Test;

class FormFormatTest {
	@Test
	void digesterPopulatesTypedBoxesAndLayoutsInConfiguredOrder() throws Exception {
		Digester digester = new Digester();
		digester.addObjectCreate("bar/format/form", FormFormat.class.getName());
		FormFormat.addDigesterEvents(digester);

		FormFormat form = (FormFormat) digester.parse(new StringReader("""
				<bar><format><form>
					<layout defaultZoom="false" />
					<layout defaultZoom="true" />
					<box id="task-name" />
				</form></format></bar>
				"""));

		assertEquals(1, form.getDefaultZoomIndex());
		assertEquals(1, form.getBoxes().size());
		assertEquals("task-name", form.getBoxes().getFirst().getId());
		assertSame(form.getLayout().get(1), form.getLayout(0));
	}
}
