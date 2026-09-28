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
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.microproject.pm.dependency.DependencyWarningNotifications;

class DependencyWarningPresenterTest {
	@AfterEach
	void resetWarningHandler() {
		DependencyWarningNotifications.setCircularLinkWarningHandler(null);
	}

	@Test
	void warningIsDeferredAndSuppressedWhenPopupsAreNotAllowed() {
		List<Runnable> dispatchQueue = new ArrayList<>();
		AtomicReference<String> displayed = new AtomicReference<>();
		DependencyWarningPresenter.install(() -> true, dispatchQueue::add, displayed::set);

		DependencyWarningNotifications.warnCircularLink("Circular dependency disabled");

		assertNull(displayed.get(), "the warning must wait until the UI dispatch callback runs");
		assertEquals(1, dispatchQueue.size());
		dispatchQueue.remove(0).run();
		assertEquals("Circular dependency disabled", displayed.get());

		DependencyWarningPresenter.install(() -> false, dispatchQueue::add, displayed::set);
		DependencyWarningNotifications.warnCircularLink("batch warning");
		assertEquals(0, dispatchQueue.size(), "batch mode must not queue a popup");
		assertEquals("Circular dependency disabled", displayed.get());
	}
}
