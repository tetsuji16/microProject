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
package com.microproject.pm.tasks;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.microproject.core.time.Duration;
import com.microproject.pm.assignment.Assignment;

/**
 * @author Laurent Chretienneau
 *
 */
public class TaskSnapshot {
	protected List<Assignment> assignments=new ArrayList<Assignment>();
	protected Date start,finish;
	protected Duration duration;
	
	public List<Assignment> getAssignments() {
		return assignments;
	}
	public void addAssignment(Assignment assignment){
		assignments.add(assignment);
	}	

	public Date getStart() {
		return start;
	}
	public void setStart(Date start) {
		this.start = start;
	}
	
	public Date getFinish() {
		return finish;
	}
	public void setFinish(Date finish) {
		this.finish = finish;
	}
	
	public Duration getDuration() {
		return duration;
	}
	public void setDuration(Duration duration) {
		this.duration = duration;
	}
	
	public String toString(String tab){
		StringBuilder s = new StringBuilder();
		s.append(tab).append("TaskSnapshot\n");
		
		s.append(tab).append("\t").append("assignments=\n");
		for (Assignment assignment : assignments)
			s.append(assignment.toString());
		
		
		return s.toString();
	}

	@Override
	public String toString(){
		return toString("");
	}
}
