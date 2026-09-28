/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.assignment;

import com.microproject.field.FieldContext;

/** Shared visibility rules for baseline fields rolled up from child values. */
public final class TimeDistributedFieldVisibility {
	private TimeDistributedFieldVisibility() {
	}

	/**
	 * Hides a baseline field when no child supplies time-distributed fields or
	 * every child that does is hidden for the requested range.
	 */
	public static boolean isBaselineFieldHidden(Iterable<?> children, int baseline, FieldContext fieldContext) {
		for (Object child : children) {
			if (!(child instanceof TimeDistributedFields fields))
				continue;
			if (!fields.fieldHideBaselineCost(baseline, fieldContext))
				return false;
		}
		return true;
	}
}
