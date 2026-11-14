/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import java.awt.event.KeyEvent;
import java.util.Objects;

/**
 * Turns key events into the Alt key sequence: Alt pressed and released on its own starts
 * it, then letters and digits pick KeyTips and Escape steps back. Alt combined with another
 * key (a shortcut) never starts it.
 */
final class RibbonKeyTipInput {
	private final RibbonKeyTipSession session;
	private boolean altDown;
	private boolean altAlone;

	RibbonKeyTipInput(RibbonKeyTipSession session) {
		this.session = Objects.requireNonNull(session);
	}

	/** Returns true when the event was used by the sequence and must not reach the component. */
	boolean dispatch(int eventId, int keyCode) {
		switch (eventId) {
			case KeyEvent.KEY_PRESSED -> {
				if (keyCode == KeyEvent.VK_ALT) {
					if (!altDown) {
						altDown = true;
						altAlone = true;
					}
					return false;
				}
				if (keyCode == KeyEvent.VK_SHIFT || keyCode == KeyEvent.VK_CONTROL || keyCode == KeyEvent.VK_META) {
					return false;
				}
				altAlone = false;
				if (!session.isActive()) {
					return false;
				}
				if (keyCode == KeyEvent.VK_ESCAPE) {
					session.back();
				} else if (isKeyTipKey(keyCode)) {
					session.type((char) keyCode);
				} else {
					session.cancel();
					return false;
				}
				return true;
			}
			case KeyEvent.KEY_TYPED -> {
				return session.isActive();
			}
			case KeyEvent.KEY_RELEASED -> {
				if (keyCode == KeyEvent.VK_ALT) {
					boolean start = altDown && altAlone;
					altDown = false;
					altAlone = false;
					if (start) {
						session.toggle();
						return true;
					}
					return false;
				}
				return session.isActive();
			}
			default -> {
				return false;
			}
		}
	}

	private static boolean isKeyTipKey(int keyCode) {
		return keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z || keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9;
	}
}
