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
package com.microproject.pm.graphic.spreadsheet.common.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.spreadsheet.SpreadSheet;

class NodeListTransfertActionTest {
	@Test
	void localValuesOverrideDelegateAndMissingValuesFallBack() {
		var delegate = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent event) {
			}
		};
		delegate.putValue("Name", "delegate-name");
		delegate.putValue("Description", "delegate-description");
		var action = new NodeListTransfertAction(delegate, null, null);
		action.putValue("Name", "spreadsheet-name");

		assertEquals("spreadsheet-name", action.getValue("Name"));
		assertEquals("delegate-description", action.getValue("Description"));

		action.putValue("Description", null);
		assertNull(action.getValue("Description"));
	}

	@Test
	void delegatesEnabledStateAndRewritesDelegatedEventSource() throws Exception {
		AtomicReference<ActionEvent> delegatedEvent = new AtomicReference<>();
		var delegate = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent event) {
				delegatedEvent.set(event);
			}
		};
		SwingUtilities.invokeAndWait(() -> {
			var spreadsheet = new SpreadSheet();
			var action = new NodeListTransfertAction(delegate, null, spreadsheet);
			action.setEnabled(false);
			assertFalse(action.isEnabled());
			ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "copy");

			action.actionPerformed(event);

			assertSame(event, delegatedEvent.get());
			assertSame(spreadsheet, delegatedEvent.get().getSource());
		});
	}
}
