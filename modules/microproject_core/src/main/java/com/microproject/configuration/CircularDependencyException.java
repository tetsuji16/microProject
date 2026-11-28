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
package com.microproject.configuration;

import com.microproject.i18n.Messages;

/**
 * Checked exception thrown when an imported or linked project introduces a circular dependency.
 */
public class CircularDependencyException extends Exception {
	private static final long serialVersionUID = -5477943199083401004L;

	/** Localized message used when a circular project link is reported to the user. */
	public static final String RUNTIME_EXCEPTION_TEXT = Messages.getString("Message.importCircularLink");

	/** Creates the exception without a message or cause. */
	public CircularDependencyException() {
		super();
	}

	/**
	 * Creates the exception with a detail message.
	 *
	 * @param arg0 detail message
	 */
	public CircularDependencyException(String arg0) {
		super(arg0);
	}

	/**
	 * Creates the exception with a cause and its derived detail message.
	 *
	 * @param arg0 cause of the circular dependency failure
	 */
	public CircularDependencyException(Throwable arg0) {
		super(arg0);
	}

	/**
	 * Creates the exception with a detail message and cause.
	 *
	 * @param arg0 detail message
	 * @param arg1 cause of the circular dependency failure
	 */
	public CircularDependencyException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}
}
