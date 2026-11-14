/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.ribbon;

import java.util.Locale;

/**
 * What the user observes when the body of a ribbon control is clicked.
 *
 * <p>{@link RibbonControlKind} says which Office control a command is; this says
 * what a click on it does and whether the control stays highlighted afterwards.
 * A split button is classified by its body: the arrow always opens its popup.</p>
 */
public enum RibbonActivation {
	/** Runs at once; the control returns to its normal look. */
	COMMAND(false, false),
	/** Opens a dialog or window; the control returns to its normal look. */
	DIALOG(false, true),
	/** Opens a popup anchored to the control; the control looks pressed while the popup is open. */
	POPUP(false, true),
	/** Flips an on/off state; the control stays highlighted while the state is on. */
	TOGGLE_STATE(true, false),
	/**
	 * Switches the active view; the control stays highlighted while its view is
	 * active and clicking it again leaves it highlighted.
	 */
	EXCLUSIVE_VIEW(true, false),
	/** Enters an input mode; the control stays highlighted until the mode ends. */
	MODE(true, false),
	/** Opens a Backstage page. */
	BACKSTAGE(false, true);

	private final boolean keepsPressedState;
	private final boolean opensSurface;

	RibbonActivation(boolean keepsPressedState, boolean opensSurface) {
		this.keepsPressedState = keepsPressedState;
		this.opensSurface = opensSurface;
	}

	/** True when the control remains highlighted after the click, driven by application state. */
	public boolean keepsPressedState() {
		return keepsPressedState;
	}

	/** True when the click opens a dialog, popup or Backstage page instead of changing the document directly. */
	public boolean opensSurface() {
		return opensSurface;
	}

	/** True when the control looks pressed only for as long as the surface it opened is showing. */
	public boolean pressedWhileOpen() {
		return this == POPUP;
	}

	/**
	 * The activation implied by the other metadata of a command, used when it
	 * declares no explicit {@code .activation}. Dialogs, exclusive views and modes
	 * cannot be inferred and must be declared.
	 */
	public static RibbonActivation defaultFor(RibbonControlKind kind, boolean toggle, String backstagePage,
		boolean backstageImmediate) {
		if (kind.hasPopup() && !kind.invokesOnClick()) {
			return POPUP;
		}
		if (backstagePage != null && !backstageImmediate) {
			return BACKSTAGE;
		}
		return toggle ? TOGGLE_STATE : COMMAND;
	}

	/** Parses an enum name, ignoring case and surrounding blanks. */
	public static RibbonActivation parse(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Missing ribbon activation");
		}
		try {
			return valueOf(value.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Unknown ribbon activation: " + value);
		}
	}
}
