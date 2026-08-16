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
package com.microproject.command;

import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.strings.Messages;
import com.microproject.util.DateTime;


/**
 * Consumer<Object> that holds parameters from UpdateProject dialog and executes the action by visiting a task collection
 */
public class UpdateProjectCommand extends Command {
	Project project;
	long date;
	boolean updateWorkAsCompleteThrough;
	boolean setFractionalPercentComplete;
	public UpdateProjectCommand(Project project, long date, boolean updateWorkAsCompleteThrough, boolean setFractionalPercentComplete) {
		super(Messages.getString("Command.UpdateProject"),project);
		this.project = project;
		this.date = DateTime.nextDay(date); // need to move ahead a day, since we really want day end, so use midnight next day
		this.updateWorkAsCompleteThrough = updateWorkAsCompleteThrough;
		this.setFractionalPercentComplete = setFractionalPercentComplete;
	}
	public void accept(Object arg0) {
		project.setStatusDate(date);
		((Task)arg0).updateProjectTask(date,updateWorkAsCompleteThrough,setFractionalPercentComplete);
	}
}
