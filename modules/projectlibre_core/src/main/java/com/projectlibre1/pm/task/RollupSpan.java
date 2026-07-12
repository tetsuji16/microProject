package com.projectlibre1.pm.task;

import java.io.Serializable;

public final class RollupSpan implements Serializable {
	private static final long serialVersionUID = 1L;

	private final long start;
	private final long finish;
	private final long duration;

	public RollupSpan(long start, long finish, long duration) {
		this.start = start;
		this.finish = finish;
		this.duration = duration;
	}

	public long getStart() {
		return start;
	}

	public long getFinish() {
		return finish;
	}

	public long getDuration() {
		return duration;
	}
}
