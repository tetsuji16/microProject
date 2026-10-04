/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.task;

import java.io.Serializable;

/** A task-owned nonworking interval, measured as working-time offsets from task start. */
public record TaskSplitInterval(long startOffset, long endOffset) implements Serializable {
	private static final long serialVersionUID = 1L;

	public TaskSplitInterval {
		if (endOffset <= startOffset) throw new IllegalArgumentException("A task split interval must have positive length");
	}
}
