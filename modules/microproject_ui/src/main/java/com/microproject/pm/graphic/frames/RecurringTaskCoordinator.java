/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
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
package com.microproject.pm.graphic.frames;

import java.awt.GraphicsEnvironment;
import java.util.function.BooleanSupplier;

import com.microproject.dialog.RecurringTaskDialog;
import com.microproject.pm.task.RecurringTaskSpec;

final class RecurringTaskCoordinator {
	interface SpecProvider {
		RecurringTaskSpec show(DocumentFrame frame);
	}

	interface InsertionHandler {
		void insert(DocumentFrame frame, RecurringTaskSpec spec);
	}

	private final BooleanSupplier headlessChecker;
	private final SpecProvider specProvider;
	private final InsertionHandler insertionHandler;

	RecurringTaskCoordinator() {
		this(
			GraphicsEnvironment::isHeadless,
			new SpecProvider() {
				@Override
				public RecurringTaskSpec show(DocumentFrame frame) {
					RecurringTaskDialog dialog = RecurringTaskDialog.getInstance(frame.getGraphicManager().getFrame());
					return dialog.doModal() ? dialog.getSpec() : null;
				}
			},
			new InsertionHandler() {
				@Override
				public void insert(DocumentFrame frame, RecurringTaskSpec spec) {
					new RecurringTaskInsertionService().insertRecurringTasks(frame, spec);
				}
			});
	}

	RecurringTaskCoordinator(
		BooleanSupplier headlessChecker,
		SpecProvider specProvider,
		InsertionHandler insertionHandler) {
		this.headlessChecker = headlessChecker;
		this.specProvider = specProvider;
		this.insertionHandler = insertionHandler;
	}

	boolean openDialogAndInsert(DocumentFrame frame) {
		if (headlessChecker.getAsBoolean())
			return false;
		RecurringTaskSpec spec = specProvider.show(frame);
		if (spec == null)
			return false;
		insertionHandler.insert(frame, spec);
		return true;
	}
}
