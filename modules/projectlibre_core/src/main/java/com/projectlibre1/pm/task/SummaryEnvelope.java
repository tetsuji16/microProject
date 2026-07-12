package com.projectlibre1.pm.task;

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
