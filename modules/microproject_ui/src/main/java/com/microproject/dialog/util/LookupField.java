/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
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
package com.microproject.dialog.util;


import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class LookupField extends JPanel {
	private static final long serialVersionUID = 1L;
	private final JLabel display;
	private String value;

	public LookupField(Object value) {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		add(display = new JLabel());
		setText((String) value);
	}

	public void setText(String value) {
		this.value = value;
		if (value == null)
			display.setText(""); //$NON-NLS-1$
		else {
			int index = value.indexOf("\\") + 1; //$NON-NLS-1$
			String label = value.substring(index); // gets the part after the slash, or all if no slash
			display.setText(label);
		}
	}
	public String getValue() {
		return value;
	}
	public String getText() {
		return value;
	}
	public JLabel getDisplay() {
		return display;
	}
}

