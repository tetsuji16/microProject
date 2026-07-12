package org.pushingpixels.neon.icon;

import java.awt.Dimension;

import org.pushingpixels.neon.api.icon.NeonIcon;

/**
 * Backward-compatible alias for the old Flamingo / Neon resizable icon API.
 *
 * <p>The new Radiance Neon API moved the concrete icon contract to
 * {@link org.pushingpixels.neon.api.icon.NeonIcon}. This shim keeps the older
 * package name alive for the forked Flamingo sources while delegating the
 * optional color-filter contract to no-op defaults.
 */
public interface ResizableIcon extends NeonIcon {
	@Override
	void setDimension(Dimension newDimension);

	@Override
	default void setColorFilter(ColorFilter colorFilter) {
		// Old Flamingo code never relied on Neon color filters. Keep the shim
		// source-compatible and let wrapped icons opt in later.
	}

	@Override
	default boolean supportsColorFilter() {
		return false;
	}
}
