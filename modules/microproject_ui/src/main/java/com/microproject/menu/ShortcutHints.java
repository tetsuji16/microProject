/*
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
 */
package com.microproject.menu;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Comparator;
import java.util.function.UnaryOperator;

import javax.swing.InputMap;
import javax.swing.KeyStroke;

/**
 * Derives the shortcut text shown in screen tips from the registered key
 * bindings. Nothing here reads a resource bundle: the displayed key is always
 * the one the document root pane dispatches.
 */
public final class ShortcutHints {
	private static final Comparator<KeyStroke> SIMPLEST_FIRST = Comparator
		.comparingInt((KeyStroke key) -> Integer.bitCount(modifiers(key)))
		.thenComparingInt(key -> describe(key).length())
		.thenComparing(ShortcutHints::describe);

	private ShortcutHints() {
	}

	/**
	 * Returns the shortcut to advertise for {@code actionId}. A command can own
	 * several keys (for example F5 and Ctrl+G for Go To); the one with the fewest
	 * modifiers and then the shortest label wins, so the choice does not depend
	 * on registration or map iteration order.
	 */
	public static KeyStroke primary(InputMap shortcuts, String actionId) {
		return primary(shortcuts, actionId, UnaryOperator.identity());
	}

	/**
	 * As {@link #primary(InputMap, String)}, comparing each binding after
	 * {@code resolver} maps it to the same action key space as {@code actionId}.
	 * The root pane binds action constants ({@code MoveTaskUp}) while a ribbon
	 * command names the handler ({@code MoveTaskUpAction}); both resolve through
	 * the menu definitions.
	 */
	public static KeyStroke primary(InputMap shortcuts, String actionId, UnaryOperator<String> resolver) {
		if (shortcuts == null || actionId == null)
			return null;
		KeyStroke[] keys = shortcuts.keys();
		if (keys == null)
			return null;
		KeyStroke best = null;
		for (KeyStroke key : keys) {
			if (shortcuts.get(key) instanceof String binding && actionId.equals(resolver.apply(binding))
				&& (best == null || SIMPLEST_FIRST.compare(key, best) < 0))
				best = key;
		}
		return best;
	}

	/** Formats a key the way Office screen tips do, for example {@code Ctrl+Shift+F5}. */
	public static String describe(KeyStroke key) {
		StringBuilder text = new StringBuilder();
		int modifiers = modifiers(key);
		if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0)
			text.append("Ctrl+");
		if ((modifiers & InputEvent.META_DOWN_MASK) != 0)
			text.append("Cmd+");
		if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0)
			text.append("Alt+");
		if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0)
			text.append("Shift+");
		return text.append(keyName(key.getKeyCode())).toString();
	}

	/**
	 * The screen tip of a ribbon command as two-line HTML: the bold name followed by the
	 * shortcut in parentheses, then the description. Missing parts are left out.
	 */
	public static String screenTip(String name, KeyStroke key, String description) {
		boolean hasName = name != null && !name.isBlank();
		boolean hasDescription = description != null && !description.isBlank();
		if (!hasName && !hasDescription)
			return null;
		StringBuilder html = new StringBuilder("<html>");
		if (hasName) {
			html.append("<b>").append(escapeHtml(name)).append("</b>");
			if (key != null)
				html.append(" (").append(escapeHtml(describe(key))).append(')');
		}
		if (hasDescription) {
			if (hasName)
				html.append("<br>");
			html.append(escapeHtml(description));
		}
		return html.append("</html>").toString();
	}

	private static String escapeHtml(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static int modifiers(KeyStroke key) {
		return key.getModifiers() & (InputEvent.CTRL_DOWN_MASK | InputEvent.META_DOWN_MASK
			| InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
	}

	/** Locale-independent key names; {@link KeyEvent#getKeyText(int)} is translated by the JDK. */
	private static String keyName(int keyCode) {
		if (keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z || keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9)
			return String.valueOf((char) keyCode);
		if (keyCode >= KeyEvent.VK_F1 && keyCode <= KeyEvent.VK_F12)
			return "F" + (keyCode - KeyEvent.VK_F1 + 1);
		return switch (keyCode) {
			case KeyEvent.VK_PERIOD -> ".";
			case KeyEvent.VK_COMMA -> ",";
			case KeyEvent.VK_PLUS, KeyEvent.VK_ADD -> "+";
			case KeyEvent.VK_EQUALS -> "=";
			case KeyEvent.VK_MINUS, KeyEvent.VK_SUBTRACT -> "-";
			case KeyEvent.VK_DIVIDE, KeyEvent.VK_SLASH -> "/";
			case KeyEvent.VK_MULTIPLY -> "*";
			case KeyEvent.VK_DELETE -> "Delete";
			case KeyEvent.VK_INSERT -> "Insert";
			case KeyEvent.VK_ESCAPE -> "Esc";
			case KeyEvent.VK_SPACE -> "Space";
			case KeyEvent.VK_ENTER -> "Enter";
			case KeyEvent.VK_TAB -> "Tab";
			case KeyEvent.VK_BACK_SPACE -> "Backspace";
			case KeyEvent.VK_HOME -> "Home";
			case KeyEvent.VK_END -> "End";
			case KeyEvent.VK_PAGE_UP -> "Page Up";
			case KeyEvent.VK_PAGE_DOWN -> "Page Down";
			case KeyEvent.VK_LEFT -> "Left";
			case KeyEvent.VK_RIGHT -> "Right";
			case KeyEvent.VK_UP -> "Up";
			case KeyEvent.VK_DOWN -> "Down";
			default -> KeyEvent.getKeyText(keyCode);
		};
	}
}
