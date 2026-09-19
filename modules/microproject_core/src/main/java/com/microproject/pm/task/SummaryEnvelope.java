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
package com.microproject.pm.task;

import java.io.Serializable;

public final class SummaryEnvelope implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;

	private Long manualStart;
	private Long manualFinish;
	private Long manualDuration;

	public boolean hasAnyManualValue() {
		return manualStart != null || manualFinish != null || manualDuration != null;
	}

	public boolean hasManualStart() {
		return manualStart != null;
	}

	public Long getManualStart() {
		return manualStart;
	}

	public void setManualStart(long manualStart) {
		this.manualStart = Long.valueOf(manualStart);
	}

	public boolean hasManualFinish() {
		return manualFinish != null;
	}

	public Long getManualFinish() {
		return manualFinish;
	}

	public void setManualFinish(long manualFinish) {
		this.manualFinish = Long.valueOf(manualFinish);
	}

	public boolean hasManualDuration() {
		return manualDuration != null;
	}

	public Long getManualDuration() {
		return manualDuration;
	}

	public void setManualDuration(long manualDuration) {
		this.manualDuration = Long.valueOf(manualDuration);
	}

	public void clearPart(SummaryEnvelopePart part) {
		switch (part) {
		case START:
			manualStart = null;
			break;
		case FINISH:
			manualFinish = null;
			break;
		case DURATION:
			manualDuration = null;
			break;
		default:
			break;
		}
	}

	@Override
	public SummaryEnvelope clone() {
		try {
			return (SummaryEnvelope) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
	}
}
