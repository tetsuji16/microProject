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

/** Application-wide provider for optional job queue UI. */
public final class JobQueueUiServices {
	private static final JobQueueUiProvider NO_OP_PROVIDER = new JobQueueUiProvider() {
		@Override
		public Object getComponent(boolean documentBased) {
			return null;
		}

		@Override
		public JobProgressMonitor createProgressMonitor(String name, Object parent, int minimum, int maximum) {
			return null;
		}
	};

	private static volatile JobQueueUiProvider provider = NO_OP_PROVIDER;

	private JobQueueUiServices() {
	}

	public static JobQueueUiProvider getProvider() {
		return provider;
	}

	public static void setProvider(JobQueueUiProvider uiProvider) {
		provider = uiProvider == null ? NO_OP_PROVIDER : uiProvider;
	}
}
