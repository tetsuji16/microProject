package org.pushingpixels.flamingo.internal.compat;

import org.pushingpixels.flamingo.api.common.icon.ResizableIcon;

public final class NeonIconCompat {
	private NeonIconCompat() {
	}

	public static ResizableIcon wrap(
			org.pushingpixels.neon.icon.ResizableIcon icon) {
		return wrap((org.pushingpixels.neon.api.icon.NeonIcon) icon);
	}

	public static ResizableIcon wrap(
			org.pushingpixels.neon.api.icon.NeonIcon icon) {
		if (icon == null) {
			return null;
		}
		if (icon instanceof ResizableIcon) {
			return (ResizableIcon) icon;
		}
		return new NeonResizableIconAdapter(icon);
	}
}
