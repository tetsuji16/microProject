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
package com.microproject.node.transform;

import java.util.EventObject;

/**
 * Notification that a {@link ViewTransformer} changed its state.
 *
 * <p>The serialized identity is fixed: the explicit {@code serialVersionUID} and the class name are
 * preserved so streamed view state keeps reading, and {@code ClassRelocations} keeps the alias from
 * the pre-rename {@code com.microproject.grouping.core.transform} package. The only state is the
 * inherited, transient {@link EventObject#getSource() source}, so the event carries no serialized
 * fields of its own.
 */
public class ViewTransformerEvent extends EventObject {
	private static final long serialVersionUID = 6416736838418985755L;

	public ViewTransformerEvent(Object source) {
		super(source);
	}
}
