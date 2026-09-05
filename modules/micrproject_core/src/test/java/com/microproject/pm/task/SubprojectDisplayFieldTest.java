/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.configuration.Configuration;
import com.microproject.field.Field;
import com.microproject.field.FieldContext;

class SubprojectDisplayFieldTest {
	@Test
	void masterColumnsAreSafeForBothOrdinaryAndLinkedTasks() {
		Field file = Configuration.getFieldFromId("Field.subprojectFile");
		Field status = Configuration.getFieldFromId("Field.subprojectStatus");
		FieldContext context = FieldContext.DEFAULT_CONTEXT;
		NormalTask ordinary = new NormalTask(null);
		DefaultSubProj linked = new DefaultSubProj();
		linked.setSubprojectFile("C:/plans/child.mpo");
		linked.setLoadStatus(SubProj.LoadStatus.INVALID);

		assertTrue(file.isHidden(ordinary, context));
		assertTrue(status.isHidden(ordinary, context));
		assertNull(file.getValue(ordinary, context));
		assertNull(status.getValue(ordinary, context));
		assertFalse(file.isHidden(linked, context));
		assertFalse(status.isHidden(linked, context));
		assertEquals("C:/plans/child.mpo", file.getValue(linked, context));
		assertEquals("INVALID", status.getValue(linked, context));
	}
}
