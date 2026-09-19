/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import java.util.concurrent.atomic.AtomicLong;

/** Monotonic identity used to discard completions belonging to an old document. */
final class DocumentGeneration {
	private final AtomicLong value = new AtomicLong();

	long current() {
		return value.get();
	}

	long advance() {
		return value.incrementAndGet();
	}

	boolean isCurrent(long token) {
		return value.get() == token;
	}
}
