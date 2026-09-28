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
package com.microproject.pm.task;

import java.util.Objects;

/** A model change and its domain-level reversal operations, independent of UI undo frameworks. */
public final class ReversibleModelChange {
	private static final ReversibleModelChange UNCHANGED = new ReversibleModelChange(false, () -> { }, () -> { });
	private final boolean changed;
	private final Runnable undo;
	private final Runnable redo;

	private ReversibleModelChange(boolean changed, Runnable undo, Runnable redo) {
		this.changed = changed;
		this.undo = Objects.requireNonNull(undo, "undo");
		this.redo = Objects.requireNonNull(redo, "redo");
	}

	public static ReversibleModelChange changed(Runnable undo, Runnable redo) {
		return new ReversibleModelChange(true, undo, redo);
	}

	public static ReversibleModelChange unchanged() {
		return UNCHANGED;
	}

	public boolean hasChanged() {
		return changed;
	}

	public void undo() {
		if (changed) undo.run();
	}

	public void redo() {
		if (changed) redo.run();
	}
}
