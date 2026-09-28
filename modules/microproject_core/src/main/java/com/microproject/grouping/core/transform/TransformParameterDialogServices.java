/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.grouping.core.transform;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Optional UI provider for collecting parameters before applying a transform. */
public final class TransformParameterDialogServices {
	private static volatile Supplier<Consumer<CommonTransform>> provider = () -> null;

	private TransformParameterDialogServices() { }

	public static Consumer<CommonTransform> createDialog() {
		return provider.get();
	}

	public static void setProvider(Supplier<Consumer<CommonTransform>> dialogProvider) {
		provider = dialogProvider == null ? () -> null : dialogProvider;
	}
}
