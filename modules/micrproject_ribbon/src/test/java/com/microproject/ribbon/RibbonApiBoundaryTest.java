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
package com.microproject.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class RibbonApiBoundaryTest {
	@Test
	void modelIsImmutableAndRequiresNoApplicationCommandTypes() {
		SwingRibbonModel.RibbonButton button = new SwingRibbonModel.RibbonButton(
			"save", SwingRibbonModel.ButtonPriority.TOP);
		SwingRibbonModel.RibbonTab tab = new SwingRibbonModel.RibbonTab(
			"file", "File", List.of(new SwingRibbonModel.RibbonBand("document", "Document", List.of(button))));
		SwingRibbonModel model = new SwingRibbonModel("desktop", List.of(tab), List.of("save"));

		assertEquals("save", model.getTabs().getFirst().getBands().getFirst().getButtons().getFirst().getId());
		assertThrows(UnsupportedOperationException.class, () -> model.getTabs().add(tab));
	}

	@Test
	void invocationPreservesTheStableCommandAndPhysicalOrigin() {
		Object source = new Object();
		RibbonCommandInvocation invocation = new RibbonCommandInvocation("save",
			RibbonCommandInvocation.Origin.OVERFLOW_POPUP, source);

		assertEquals("save", invocation.commandId());
		assertEquals(RibbonCommandInvocation.Origin.OVERFLOW_POPUP, invocation.origin());
		assertEquals(source, invocation.source());
	}

	@Test
	void semanticResultKeepsAffectedTaskIdsImmutable() {
		var ids = new java.util.ArrayList<Long>();
		ids.add(42L);
		RibbonCommandResult result = RibbonCommandResult.changed("HideSelectedTasks", ids);
		ids.add(99L);
		assertEquals(List.of(42L), result.affectedTaskIds());
		assertThrows(UnsupportedOperationException.class, () -> result.affectedTaskIds().add(7L));
	}
}
