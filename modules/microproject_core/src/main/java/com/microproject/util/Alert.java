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
package com.microproject.util;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class Alert {
	public static final int YES_OPTION = 0;
	public static final int NO_OPTION = 1;
	public static final int CANCEL_OPTION = 2;
	public static final int OK_OPTION = 0;
	public static final int CLOSED_OPTION = -1;

	private static final Logger logger = Logger.getLogger(Alert.class.getName());
	private static volatile AlertPresenter presenter;

	/** Installs a UI presenter, or clears it when {@code alertPresenter} is null. */
	public static void setPresenter(AlertPresenter alertPresenter) {
		presenter = alertPresenter;
	}

	public static void warn(Object errorObject) {
		if (allowPopups())
			warn(errorObject, null);
	}
	public static void warn(Object errorObject, Object parent) {
		logger.log(Level.WARNING, "warning message {0}", errorObject);
		AlertPresenter currentPresenter = presenter;
		if (allowPopups() && currentPresenter != null)
			currentPresenter.warn(errorObject, parent);
	}

	public static void error(Object errorObject) {
		if (allowPopups())
			error(errorObject, null);
	}
	public static void error(Object errorObject, Object parent) {
		logger.log(Level.SEVERE, "error message {0}", errorObject);
		AlertPresenter currentPresenter = presenter;
		if (allowPopups() && currentPresenter != null)
			currentPresenter.error(errorObject, parent);
	}
	public static int confirmYesNo(Object messageObject) {
		if (!allowPopups())
			return NO_OPTION;
		AlertPresenter currentPresenter = presenter;
		return currentPresenter == null ? NO_OPTION : currentPresenter.confirmYesNo(messageObject);
	}
	public static int confirm(Object messageObject) {
		if (!allowPopups())
			return NO_OPTION;
		AlertPresenter currentPresenter = presenter;
		return currentPresenter == null ? NO_OPTION : currentPresenter.confirm(messageObject);
	}
	public static boolean okCancel(Object messageObject) {
		if (!allowPopups())
			return true;
		AlertPresenter currentPresenter = presenter;
		return currentPresenter == null || currentPresenter.okCancel(messageObject);
	}

	public static String renameProject(final String name,Set<String> projectNames,boolean saveAs){
		AlertPresenter currentPresenter = presenter;
		return currentPresenter == null ? null : currentPresenter.renameProject(name, projectNames, saveAs);
	}
	public static boolean allowPopups() {
		return Environment.isClientSide() && !Environment.isBatchMode();
	}

	public static void warnWithOnceOption(Object object,String preference) {
		warnWithOnceOption(object, preference, null);
	}
	public static void warnWithOnceOption(Object object,String preference,Object parentComponent) {
		AlertPresenter currentPresenter = presenter;
		if (currentPresenter != null)
			currentPresenter.warnWithOnceOption(object, preference, parentComponent);
	}

}
