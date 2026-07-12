package org.pushingpixels.trident.swing;

import java.awt.Component;
import java.awt.Rectangle;

/**
 * Compatibility wrapper for the legacy Swing repaint callback package.
 */
public class SwingRepaintCallback
		extends org.pushingpixels.trident.api.swing.SwingRepaintCallback {
	public SwingRepaintCallback(Component component) {
		super(component);
	}

	public SwingRepaintCallback(Component component, Rectangle repaintArea) {
		super(component, repaintArea);
	}
}
