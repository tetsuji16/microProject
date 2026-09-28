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
package com.microproject.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.microproject.util.Environment;

class JobQueueUiServicesTest {
	private final boolean originalVisible = Environment.isVisible();

	@AfterEach
	void restoreUiServices() {
		JobQueueUiServices.setProvider(null);
		Environment.setVisible(originalVisible);
	}

	@Test
	void delegatesParentAndProgressMonitorCreationToTheUiProvider() {
		Object parent = new Object();
		RecordingProgressMonitor monitor = new RecordingProgressMonitor();
		RecordingProvider provider = new RecordingProvider(parent, monitor);
		JobQueueUiServices.setProvider(provider);
		Environment.setVisible(true);
		JobQueue queue = new JobQueue("document queue", true);

		assertSame(parent, queue.getComponent());
		assertSame(monitor, queue.getProgressMonitor("Loading", null));
		assertEquals("Loading", provider.monitorName);
		assertSame(parent, provider.monitorParent);
		assertEquals(0, provider.minimum);
		assertEquals(JobQueue.MAX_PROGRESS, provider.maximum);

		Object explicitParent = new Object();
		assertSame(monitor, queue.getProgressMonitor("Saving", explicitParent));
		assertSame(explicitParent, provider.monitorParent);
	}

	@Test
	void hiddenQueuesDoNotResolveAUiComponent() {
		JobQueueUiServices.setProvider(new RecordingProvider(new Object(), new RecordingProgressMonitor()));
		Environment.setVisible(false);

		assertNull(new JobQueue("headless queue", false).getComponent());
	}

	private static final class RecordingProvider implements JobQueueUiProvider {
		private final Object component;
		private final JobProgressMonitor monitor;
		private String monitorName;
		private Object monitorParent;
		private int minimum;
		private int maximum;

		private RecordingProvider(Object component, JobProgressMonitor monitor) {
			this.component = component;
			this.monitor = monitor;
		}

		@Override
		public Object getComponent(boolean documentBased) {
			return component;
		}

		@Override
		public JobProgressMonitor createProgressMonitor(String name, Object parent, int minimum, int maximum) {
			monitorName = name;
			monitorParent = parent;
			this.minimum = minimum;
			this.maximum = maximum;
			return monitor;
		}
	}

	private static final class RecordingProgressMonitor implements JobProgressMonitor {
		@Override public void setProgress(int progress) { }
		@Override public void setNote(String note) { }
		@Override public void close() { }
		@Override public boolean isCanceled() { return false; }
		@Override public boolean isClosed() { return false; }
	}
}
