/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.microproject.ribbon.CommandId;
import com.microproject.ribbon.RibbonCommandResult;

class HeadlessDocumentCommandTest {
	@Test
	void indentMakesSelectedTaskChildOfPredecessorAndUndoRedoRestoreIt() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("First", "Second")) {
			var first = fx.task(0);
			var second = fx.task(1);
			assertNull(second.getWbsParentTask());

			fx.select(second);
			RibbonCommandResult result = fx.run(CommandId.INDENT);
			assertEquals(RibbonCommandResult.Status.CHANGED, result.status());
			assertSame(first, second.getWbsParentTask());

			fx.undo();
			assertNull(second.getWbsParentTask());

			fx.redo();
			assertSame(first, second.getWbsParentTask());
		}
	}
}
