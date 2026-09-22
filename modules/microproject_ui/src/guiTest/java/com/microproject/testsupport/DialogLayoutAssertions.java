/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

/** Shared visual invariants for text-bearing controls in dialogs. */
public final class DialogLayoutAssertions {
	private DialogLayoutAssertions() {
	}

	/** Ensures visible, non-empty text controls retain their font-derived height. */
	public static void assertTextControlsAtPreferredHeight(Container root, String context) throws Exception {
		final String[] clipping = new String[1];
		Runnable inspect = () -> {
			layoutTree(root);
			clipping[0] = findClippedTextControl(root);
		};
		if (SwingUtilities.isEventDispatchThread())
			inspect.run();
		else
			SwingUtilities.invokeAndWait(inspect);
		assertTrue(clipping[0] == null, () -> context + " clips a text control: " + clipping[0]);
	}

	private static String findClippedTextControl(Container root) {
		Component[] children = root.getComponents();
		for (int first = 0; first < children.length; first++) {
			Component child = children[first];
			if (!child.isVisible() || !hasVisibleText(child))
				continue;
			Rectangle bounds = child.getBounds();
			if (bounds.width <= 0 || bounds.height <= 0 || bounds.x < 0 || bounds.y < 0
					|| bounds.x + bounds.width > root.getWidth() || bounds.y + bounds.height > root.getHeight())
				return child.getClass().getName() + " escapes parent bounds=" + bounds + " parent=" + root.getSize();
			if (child.getHeight() < child.getPreferredSize().height)
				return child.getClass().getName() + " bounds=" + bounds + " preferred=" + child.getPreferredSize();
			for (int second = first + 1; second < children.length; second++) {
				Component sibling = children[second];
				if (sibling.isVisible() && hasVisibleText(sibling) && bounds.intersects(sibling.getBounds()))
					return child.getClass().getName() + " overlaps " + sibling.getClass().getName()
						+ " bounds=" + bounds + " sibling=" + sibling.getBounds();
			}
		}
		for (Component child : root.getComponents()) {
			if (!child.isVisible())
				continue;
			if (child instanceof Container nested) {
				String clipping = findClippedTextControl(nested);
				if (clipping != null)
					return clipping;
			}
		}
		return null;
	}

	private static boolean hasVisibleText(Component component) {
		if (component instanceof JTextComponent)
			return true;
		if (component instanceof JComboBox<?>)
			return true;
		if (component instanceof JLabel label)
			return label.getText() != null && !label.getText().isBlank();
		return component instanceof AbstractButton button && button.getText() != null && !button.getText().isBlank();
	}

	private static void layoutTree(Container root) {
		root.doLayout();
		for (Component child : root.getComponents())
			if (child instanceof Container nested)
				layoutTree(nested);
	}
}
