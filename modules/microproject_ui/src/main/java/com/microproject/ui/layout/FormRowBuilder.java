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

import javax.swing.JLabel;

/**
 * The form-building steps that shared helpers such as {@code FieldComponentMap} use,
 * so that they work with any builder that keeps a current row and column.
 */
public interface FormRowBuilder {
	Component append(Component component);

	Component append(String text, Component component);

	JLabel addLabel(String text);

	/**
	 * Adds {@code component} at {@code col}, {@code row} spanning {@code colSpan} columns,
	 * then moves the current column on by two, like {@code add(Component, Object)}.
	 */
	Component addSpanning(Component component, int col, int row, int colSpan);

	void nextLine(int lineCount);

	void nextColumn(int gapColumns);

	int getColumn();

	int getRow();
}
