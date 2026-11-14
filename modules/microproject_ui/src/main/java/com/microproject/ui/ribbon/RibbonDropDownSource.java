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
package com.microproject.ui.ribbon;

import java.util.List;

/**
 * Supplies the current value and the choices of ribbon drop-downs
 * ({@link com.microproject.ribbon.RibbonControlKind#DROP_DOWN}), for example the
 * filter or group applied to the active view. The ribbon only draws the control;
 * the document layer owns what is applied.
 *
 * <p>All methods are called on the event dispatch thread.</p>
 */
public interface RibbonDropDownSource {
	/** Display name of the value in effect for {@code commandId}, or {@code null} when there is none. */
	String currentValue(String commandId);

	/** Display names of the values that can be chosen; empty when no document is active. */
	List<String> choices(String commandId);

	/** Applies the choice at {@code index} of the list last returned by {@link #choices}. */
	void select(String commandId, int index);
}
