package org.pushingpixels.flamingo.internal.compat;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

import org.pushingpixels.neon.api.icon.NeonIcon.ColorFilter;
import org.pushingpixels.flamingo.api.common.icon.ResizableIcon;

public final class NeonResizableIconAdapter implements ResizableIcon {
	private final org.pushingpixels.neon.api.icon.NeonIcon delegate;

	public NeonResizableIconAdapter(
			org.pushingpixels.neon.api.icon.NeonIcon delegate) {
		this.delegate = delegate;
	}

	@Override
	public void setDimension(Dimension newDimension) {
		this.delegate.setDimension(newDimension);
	}

	@Override
	public void setColorFilter(ColorFilter colorFilter) {
		this.delegate.setColorFilter(colorFilter);
	}

	@Override
	public boolean supportsColorFilter() {
		return this.delegate.supportsColorFilter();
	}

	@Override
	public int getIconWidth() {
		return this.delegate.getIconWidth();
	}

	@Override
	public int getIconHeight() {
		return this.delegate.getIconHeight();
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		this.delegate.paintIcon(c, g, x, y);
	}

	public org.pushingpixels.neon.api.icon.NeonIcon getDelegate() {
		return this.delegate;
	}
}
