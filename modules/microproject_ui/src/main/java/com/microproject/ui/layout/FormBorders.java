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
package com.microproject.ui.layout;

import java.awt.Component;
import java.awt.Insets;
import java.util.Objects;

import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/** Empty borders whose sizes are written like {@link FormLayout} sizes. */
public final class FormBorders {
	private FormBorders() {
	}

	/**
	 * Returns an empty border from {@code "top, left, bottom, right"}, each a size in
	 * {@code px}, {@code dlu} or {@code pt}. Dialog units are converted with the font
	 * metrics of the component the border is painted on.
	 */
	public static Border createEmptyBorder(String spec) {
		Objects.requireNonNull(spec, "spec");
		String[] parts = spec.trim().split("\\s*,\\s*", -1);
		if (parts.length != 4)
			throw new IllegalArgumentException("A border needs 4 sizes (top, left, bottom, right), not \"" + spec + "\"");
		return new SizedEmptyBorder(FormSpec.constant(parts[0].trim(), false), FormSpec.constant(parts[1].trim(), true),
				FormSpec.constant(parts[2].trim(), false), FormSpec.constant(parts[3].trim(), true));
	}

	private static final class SizedEmptyBorder extends EmptyBorder {
		private static final long serialVersionUID = 1L;
		private final FormSpec.ConstantSize top;
		private final FormSpec.ConstantSize left;
		private final FormSpec.ConstantSize bottom;
		private final FormSpec.ConstantSize right;

		private SizedEmptyBorder(FormSpec.ConstantSize top, FormSpec.ConstantSize left, FormSpec.ConstantSize bottom,
				FormSpec.ConstantSize right) {
			super(0, 0, 0, 0);
			this.top = top;
			this.left = left;
			this.bottom = bottom;
			this.right = right;
		}

		@Override
		public Insets getBorderInsets() {
			return getBorderInsets(null);
		}

		@Override
		public Insets getBorderInsets(Component c) {
			return getBorderInsets(c, new Insets(0, 0, 0, 0));
		}

		@Override
		public Insets getBorderInsets(Component c, Insets insets) {
			insets.top = top.pixels(c);
			insets.left = left.pixels(c);
			insets.bottom = bottom.pixels(c);
			insets.right = right.pixels(c);
			return insets;
		}
	}
}
