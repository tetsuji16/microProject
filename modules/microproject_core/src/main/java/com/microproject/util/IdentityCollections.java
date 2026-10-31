/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.util;

import java.util.List;

/** Reference-identity membership helpers for model objects that define no usable {@code equals}. */
public final class IdentityCollections {
	private IdentityCollections() {
	}

	/** Returns whether {@code candidate} is the same instance as an element of {@code values}. */
	public static boolean containsIdentity(Iterable<?> values, Object candidate) {
		for (Object value : values) if (value == candidate) return true;
		return false;
	}

	/** Adds a non-null {@code value} unless the same instance is already present. */
	public static <T> void addIdentity(List<T> values, T value) {
		if (value != null && !containsIdentity(values, value)) values.add(value);
	}
}
