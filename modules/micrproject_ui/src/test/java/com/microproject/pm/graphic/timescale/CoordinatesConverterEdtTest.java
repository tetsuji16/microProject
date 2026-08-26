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
package com.microproject.pm.graphic.timescale;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class CoordinatesConverterEdtTest {
	@Test
	void scheduleChangesNotifyUiListenersOnEdt() throws Exception {
		var undo = new DataFactoryUndoController();
		var project = Project.createProject(ResourcePool.createRourcePool("coordinates-test", undo), undo);
		project.initialize(false, false);
		var converter = new RecordingConverter(project);

		converter.scheduleChanged(null);

		assertTrue(converter.notified.await(5, TimeUnit.SECONDS));
		assertTrue(converter.onEdt.get());
		converter.dispose();
	}

	@Test
	void burstIsCoalescedAndDisposeCancelsPendingUpdate() throws Exception {
		var undo = new DataFactoryUndoController();
		var project = Project.createProject(ResourcePool.createRourcePool("coordinates-burst", undo), undo);
		project.initialize(false, false);
		var converter = new RecordingConverter(project);
		var cancelled = new RecordingConverter(project);
		var edtBlocked = new CountDownLatch(1);
		var releaseEdt = new CountDownLatch(1);
		SwingUtilities.invokeLater(() -> {
			edtBlocked.countDown();
			try { releaseEdt.await(5, TimeUnit.SECONDS); }
			catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
		});
		assertTrue(edtBlocked.await(5, TimeUnit.SECONDS));

		for (int index = 0; index < 10_000; index++) {
			converter.scheduleChanged(null);
			cancelled.scheduleChanged(null);
		}
		cancelled.dispose();
		releaseEdt.countDown();
		SwingUtilities.invokeAndWait(() -> { });

		assertTrue(converter.updateCount.get() == 1);
		assertTrue(cancelled.updateCount.get() == 0);
		converter.dispose();
	}

	@Test
	void edtBurstIsAlsoCoalesced() throws Exception {
		var undo = new DataFactoryUndoController();
		var project = Project.createProject(ResourcePool.createRourcePool("coordinates-edt-burst", undo), undo);
		project.initialize(false, false);
		var converter = new RecordingConverter(project);

		SwingUtilities.invokeAndWait(() -> {
			for (int index = 0; index < 10_000; index++) converter.scheduleChanged(null);
		});
		SwingUtilities.invokeAndWait(() -> { });

		assertTrue(converter.updateCount.get() == 1);
		converter.dispose();
	}

	private static final class RecordingConverter extends CoordinatesConverter {
		private static final long serialVersionUID = 1L;
		private CountDownLatch notified;
		private AtomicBoolean onEdt;
		private AtomicInteger updateCount;

		RecordingConverter(Project project) {
			super(project);
			notified = new CountDownLatch(1);
			onEdt = new AtomicBoolean();
			updateCount = new AtomicInteger();
		}

		@Override
		protected void updateLargeInterval(boolean event) {
			if (notified == null) {
				super.updateLargeInterval(event);
				return;
			}
			onEdt.set(SwingUtilities.isEventDispatchThread());
			updateCount.incrementAndGet();
			notified.countDown();
		}
	}
}
