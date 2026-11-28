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
package com.microproject.job;


/**
 * A unit of work that a {@link Job} runs. A runnable only reports progress
 * after it belongs to a job; {@link Job#addRunnable(JobRunnable)} attaches it.
 */
public abstract class JobRunnable{
	protected Job job;
	protected Object previousResult;
	protected Exception previousException;
	protected String name;
	protected float weight;

	/**
	 * @param name user-visible name of the unit of work
	 * @param weight share of the job's progress this runnable reports
	 */
	public JobRunnable(String name,float weight) {
		this.name=name;
		this.weight=weight;
	}

	/**
	 * @param name user-visible name of the unit of work
	 */
	public JobRunnable(String name) {
		this(name,0.0f);
	}

	/**
	 * Runs the work.
	 *
	 * @return the result handed to the next runnable
	 * @throws Exception when the work failed; the job enters its exception handler
	 */
	public abstract Object run() throws Exception;

	protected Object getPreviousResult() {
		return previousResult;
	}

	void setPreviousResult(Object previousResult) {
		this.previousResult = previousResult;
	}

	protected Exception getPreviousException() {
		return previousException;
	}

	void setPreviousException(Exception previousException) {
		this.previousException = previousException;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public float getWeight() {
		return weight;
	}

	public void setWeight(float weight) {
		this.weight = weight;
	}

	/**
	 * Reports the progress of this runnable to its job.
	 *
	 * @param progress progress as a fraction between 0 and 1
	 * @throws IllegalStateException when this runnable has not been added to a job
	 */
	public void setProgress(float progress) {
		if (job == null)
			throw new IllegalStateException("Runnable " + name + " is not attached to a job");
		job.setProgress(progress,this);
	}

	/**
	 * Reports the progress of this runnable and the note shown next to it.
	 *
	 * @param progress progress as a fraction between 0 and 1
	 * @param note note shown by the progress monitor, possibly null
	 * @throws IllegalStateException when this runnable has not been added to a job
	 */
	public void setProgress(float progress, final String note) {
		if (job == null)
			throw new IllegalStateException("Runnable " + name + " is not attached to a job");
		job.setProgress(progress,note,this);
	}
	public Job getJob() {
		return job;
	}
	public void setJob(Job job) {
		this.job = job;
	}

}
