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

import java.util.Arrays;
import java.util.Locale;

/**
 * Control types of the Office Fluent ribbon, as defined by the Microsoft
 * custom UI schema ([MS-CUSTOMUI], elements {@code button}, {@code toggleButton},
 * {@code splitButton}, {@code menu}, {@code gallery}, {@code dropDown},
 * {@code comboBox}, {@code checkBox}, {@code editBox} and
 * {@code dialogBoxLauncher}).
 *
 * <p>The kind is the behavior contract of a ribbon control: what a click on its
 * primary area does, whether it owns a popup, whether it shows a pressed state
 * and whether it carries a value. Renderers and tests read these traits instead
 * of testing for individual command ids.</p>
 */
public enum RibbonControlKind {
	/** Click invokes the command. No popup, no state. */
	BUTTON("button", true, false, false, false),
	/** Click invokes the command and the control shows the on/off state of the command. */
	TOGGLE_BUTTON("toggleButton", true, false, true, false),
	/**
	 * Two hit areas: the primary area invokes the default command, the arrow area
	 * opens a popup of related commands. The primary area may itself be a toggle.
	 */
	SPLIT_BUTTON("splitButton", true, true, false, false),
	/** The whole control opens a popup of commands; it has no command of its own. */
	MENU("menu", false, true, false, false),
	/** The whole control opens a popup grid of selectable items. */
	GALLERY("gallery", false, true, false, true),
	/** Shows the current selection as text and opens a list to pick another one. */
	DROP_DOWN("dropDown", false, true, false, true),
	/** A drop-down whose text can also be typed. */
	COMBO_BOX("comboBox", false, true, false, true),
	/** A labeled check mark that shows and flips the on/off state of the command. */
	CHECK_BOX("checkBox", true, false, true, false),
	/** A text field whose committed value is passed to the command. */
	EDIT_BOX("editBox", false, false, false, true),
	/** The small corner button of a group that opens the dialog of that group. */
	DIALOG_LAUNCHER("dialogBoxLauncher", true, false, false, false);

	private final String customUiElement;
	private final boolean invokesOnClick;
	private final boolean popup;
	private final boolean pressedState;
	private final boolean value;

	RibbonControlKind(String customUiElement, boolean invokesOnClick, boolean popup, boolean pressedState, boolean value) {
		this.customUiElement = customUiElement;
		this.invokesOnClick = invokesOnClick;
		this.popup = popup;
		this.pressedState = pressedState;
		this.value = value;
	}

	/** The element name of this control in the Microsoft custom UI schema. */
	public String customUiElement() {
		return customUiElement;
	}

	/** True when a click on the primary area runs a command directly. */
	public boolean invokesOnClick() {
		return invokesOnClick;
	}

	/** True when the control owns a popup (menu, list or grid). */
	public boolean hasPopup() {
		return popup;
	}

	/** True when the control itself displays an on/off state. */
	public boolean hasPressedState() {
		return pressedState;
	}

	/** True when the control shows or accepts a value rather than only firing a command. */
	public boolean hasValue() {
		return value;
	}

	/**
	 * Parses a resource value. Accepts the enum name ({@code SPLIT_BUTTON}) and the
	 * custom UI element name ({@code splitButton}), ignoring case.
	 *
	 * @throws IllegalArgumentException when the value names no control kind
	 */
	public static RibbonControlKind parse(String value) {
		String trimmed = value == null ? "" : value.trim();
		for (RibbonControlKind kind : values()) {
			if (kind.name().equalsIgnoreCase(trimmed) || kind.customUiElement.equalsIgnoreCase(trimmed)) {
				return kind;
			}
		}
		throw new IllegalArgumentException("Unknown ribbon control kind: " + value
			+ " (expected one of " + Arrays.toString(values()).toLowerCase(Locale.ROOT) + ")");
	}
}
