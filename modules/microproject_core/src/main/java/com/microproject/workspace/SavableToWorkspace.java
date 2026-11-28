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
package com.microproject.workspace;


/**
 * A view that can save and restore its state. The caller decides which parts of
 * the state are wanted through the {@code context} argument.
 */
public interface SavableToWorkspace {
	/**
	 * Context for the state that only affects what the user sees, such as the
	 * visible date range; it is restored when a window is laid out again and is
	 * not part of the saved project.
	 */
	public static final int VIEW = 0;
	/**
	 * Context for the state that has to survive saving and reopening the
	 * project, such as the column configuration of a view.
	 */
	public static final int PERSIST = 1;

	/**
	 * Applies a previously created workspace state.
	 *
	 * @param setting the state to apply, as returned by
	 *                {@link #createWorkspace(int)}
	 * @param context {@link #VIEW} or {@link #PERSIST}
	 */
	void restoreWorkspace(WorkspaceSetting setting, int context);

	/**
	 * Captures the current state of this view.
	 *
	 * @param context {@link #VIEW} or {@link #PERSIST}; implementations may
	 *                capture only the fields that belong to it
	 * @return the state, to be applied later with {@link #restoreWorkspace}
	 */
	WorkspaceSetting createWorkspace(int context);
}
