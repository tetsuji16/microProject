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
package com.microproject.options;

/**
 * Corresponds to General tab on options dialog in MSProject
 */
public class GeneralOption {
	private static GeneralOption instance = null;
	public static GeneralOption getInstance() {
		if (instance == null)
			instance = new GeneralOption();
		return instance;
	}

	/**
	 * 
	 */
	public GeneralOption() {
		super();
	}
	
	/*
	 * Microsoft Project lets a user type a previously unseen person directly in
	 * the task-sheet Resource Names field.  Keep that low-friction entry path
	 * available by default; AssignmentFormat creates the local work resource and
	 * assigns it as one user edit.  Users can still turn the legacy combined
	 * option off when they need strict name validation.
	 */
	private boolean automaticallyAddNewResourcesAndTasks = true;
	private double defaultStandardRate = 0.0D;
	private double defaultOvertimeRate = 0.0D;
	private boolean startWithBlankProject = false;
	private boolean confirmDeletes = false;

	/**
	 * @return Returns the automaticallyAddNewResourcesAndTasks.
	 */
	public boolean isAutomaticallyAddNewResourcesAndTasks() {
		return automaticallyAddNewResourcesAndTasks;
	}
	/**
	 * @param automaticallyAddNewResourcesAndTasks The automaticallyAddNewResourcesAndTasks to set.
	 */
	public void setAutomaticallyAddNewResourcesAndTasks(
			boolean automaticallyAddNewResourcesAndTasks) {
		this.automaticallyAddNewResourcesAndTasks = automaticallyAddNewResourcesAndTasks;
	}
	/**
	 * @return Returns the defaultOvertimeRate.
	 */
	public double getDefaultOvertimeRate() {
		return defaultOvertimeRate;
	}
	/**
	 * @param defaultOvertimeRate The defaultOvertimeRate to set.
	 */
	public void setDefaultOvertimeRate(double defaultOvertimeRate) {
		this.defaultOvertimeRate = defaultOvertimeRate;
	}
	/**
	 * @return Returns the defaultStandardRate.
	 */
	public double getDefaultStandardRate() {
		return defaultStandardRate;
	}
	/**
	 * @param defaultStandardRate The defaultStandardRate to set.
	 */
	public void setDefaultStandardRate(double defaultStandardRate) {
		this.defaultStandardRate = defaultStandardRate;
	}
	public final boolean isStartWithBlankProject() {
		return startWithBlankProject;
	}
	public final void setStartWithBlankProject(boolean startWithBlankProject) {
		this.startWithBlankProject = startWithBlankProject;
	}

	public final boolean isConfirmDeletes() {
		return confirmDeletes;
	}

	public final void setConfirmDeletes(boolean confirmDeletes) {
		this.confirmDeletes = confirmDeletes;
	}
}
