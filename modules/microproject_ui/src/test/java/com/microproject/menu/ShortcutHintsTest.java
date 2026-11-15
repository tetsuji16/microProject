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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.InputMap;
import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;

class ShortcutHintsTest {
	private static final int CTRL = InputEvent.CTRL_DOWN_MASK;
	private static final int SHIFT = InputEvent.SHIFT_DOWN_MASK;
	private static final int ALT = InputEvent.ALT_DOWN_MASK;

	@Test
	void describesKeysWithoutLocaleDependentNames() {
		assertEquals("Ctrl+S", ShortcutHints.describe(KeyStroke.getKeyStroke(KeyEvent.VK_S, CTRL)));
		assertEquals("Ctrl+Shift+F5", ShortcutHints.describe(KeyStroke.getKeyStroke(KeyEvent.VK_F5, CTRL | SHIFT)));
		assertEquals("Alt+Shift+Right", ShortcutHints.describe(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, ALT | SHIFT)));
		assertEquals("Cmd+N", ShortcutHints.describe(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.META_DOWN_MASK)));
		assertEquals("Ctrl+.", ShortcutHints.describe(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, CTRL)));
		assertEquals("Delete", ShortcutHints.describe(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0)));
		assertEquals("F1", ShortcutHints.describe(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0)));
	}

	@Test
	void screenTipHasTheBoldNameThenTheShortcutThenTheDescriptionOnASecondLine() {
		KeyStroke ctrlS = KeyStroke.getKeyStroke(KeyEvent.VK_S, CTRL);
		assertEquals("<html><b>Save</b> (Ctrl+S)<br>Save the project</html>",
			ShortcutHints.screenTip("Save", ctrlS, "Save the project"));
		assertEquals("<html><b>Save</b><br>Save the project</html>", ShortcutHints.screenTip("Save", null, "Save the project"));
		assertEquals("<html><b>Save</b> (Ctrl+S)</html>", ShortcutHints.screenTip("Save", ctrlS, null));
		assertEquals("<html>Save the project</html>", ShortcutHints.screenTip(null, ctrlS, "Save the project"));
		assertEquals("<html><b>A &amp; B</b><br>x &lt; y</html>", ShortcutHints.screenTip("A & B", null, "x < y"));
		assertNull(ShortcutHints.screenTip(null, ctrlS, null));
	}

	@Test
	void picksTheSimplestKeyOfACommandRegardlessOfRegistrationOrder() {
		KeyStroke f5 = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);
		KeyStroke ctrlG = KeyStroke.getKeyStroke(KeyEvent.VK_G, CTRL);
		KeyStroke ctrlShiftP = KeyStroke.getKeyStroke(KeyEvent.VK_P, CTRL | SHIFT);
		KeyStroke shiftF2 = KeyStroke.getKeyStroke(KeyEvent.VK_F2, SHIFT);
		InputMap forward = new InputMap();
		forward.put(ctrlG, "GoTo");
		forward.put(f5, "GoTo");
		forward.put(ctrlShiftP, "Information");
		forward.put(shiftF2, "Information");
		InputMap reverse = new InputMap();
		reverse.put(shiftF2, "Information");
		reverse.put(ctrlShiftP, "Information");
		reverse.put(f5, "GoTo");
		reverse.put(ctrlG, "GoTo");

		for (InputMap shortcuts : new InputMap[] {forward, reverse}) {
			assertEquals(f5, ShortcutHints.primary(shortcuts, "GoTo"));
			assertEquals(shiftF2, ShortcutHints.primary(shortcuts, "Information"));
			assertNull(ShortcutHints.primary(shortcuts, "Unbound"));
		}
		assertNull(ShortcutHints.primary(null, "GoTo"));
		assertNull(ShortcutHints.primary(new InputMap(), "GoTo"));
	}
}
