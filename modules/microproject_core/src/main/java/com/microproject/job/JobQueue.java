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
package com.microproject.job;

import java.util.Collections;
import java.util.function.Consumer;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Set;
import java.lang.reflect.Array;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Objects;
import java.util.concurrent.CancellationException;



import com.microproject.util.Environment;
import com.microproject.util.ListenerRegistry;


import com.microproject.util.UiDispatch;

/**
 *
 */
public class JobQueue extends ThreadGroup{
	private static final Logger logger = Logger.getLogger(JobQueue.class.getName());
	public final static int MAX_PROGRESS=10000;
	protected boolean documentBased = false;
	public JobQueue(String name,boolean documentBased) {
		super(name);
		this.documentBased=documentBased;
	}

	public boolean hasNext(){
		return activeCount()!=0;
	}
	public synchronized void startNext(){
		if (hasNext()){
			Thread[] threads=new Thread[1];
			if (enumerate(threads)==1){
				threads[0].start();
			}
		}
	}
	public synchronized void cancel(){
		int count=activeCount();
		if (count==0) return;
		Thread[] threads=new Thread[count];
		count=enumerate(threads);
		for (int i=0;i<count;i++){
			if (threads[i] instanceof Job job) job.cancel();
		}
	}

	private Set<String> executingJobs=Collections.synchronizedSet(new HashSet<>());

	public void addExecutingJob(Job job) {
		executingJobs.add(job.getName());
	}
	public void removeExecutingJob(Job job) {
		executingJobs.remove(job.getName());
	}

	public void schedule(Job job){
		synchronized (executingJobs) {
			if (!executingJobs.add(job.getName())) return; // avoid double click
		}
		// A queued load can own the critical section while a user presses Save.
		// Starting the save synchronously from the EDT would then wait for that
		// load and freeze the document window.  Defer only the queue admission;
		// Job still serializes its work through the same critical section and
		// retains the ordinary Swing completion callbacks.
		if (UiDispatch.isDispatchThread()) {
			Thread scheduler = new Thread(job::execute, job.getName() + "_scheduler");
			scheduler.setDaemon(job.isDaemon());
			scheduler.start();
		} else {
			job.execute();
		}
	}
	public JobProgressMonitor getProgressMonitor(String name, Object component) {
		Object parent = component == null ? getComponent() : component;
		if (parent == null)
			return null;
		return JobQueueUiServices.getProvider().createProgressMonitor(name, parent, 0, MAX_PROGRESS);
	}


	private final ListenerRegistry<JobQueueListener> listeners = new ListenerRegistry<>();

	public void addListener(JobQueueListener l) {
		listeners.add(l);
	}
	public void removeListener(JobQueueListener l) {
		listeners.remove(l);
	}
	public JobQueueListener[] getListeners() {
		return listeners.snapshotReverse().toArray(JobQueueListener[]::new);
	}
    public EventListener[] getListeners(Class listenerType) {
		if (listenerType == null)
			throw new NullPointerException("listenerType");
		if (listenerType != JobQueueListener.class)
			return (EventListener[]) Array.newInstance(listenerType, 0);
		return listeners.snapshotReverse().toArray(JobQueueListener[]::new);
    }

 	protected void fireProgressChanged(Object source,float progress) {
		JobQueueEvent e = null;
		for (JobQueueListener listener : listeners.snapshotReverse()) {
			if (e == null)
				e = new JobQueueEvent(source,progress);
			listener.progressChanged(e);
		}
	}

	protected Object criticalSectionMutex=new Object();

 	protected Job criticalSectionOwner;

 	//for free jobs (queued==false)
	public boolean executeCriticalSectionClosure(Job job,Consumer<Object> c,Object arg) {
		Objects.requireNonNull(job, "job");
		Objects.requireNonNull(c, "closure");
		synchronized (criticalSectionMutex) {
			 if (criticalSectionOwner==job){
				 c.accept(arg);
				 return true;
			 }else{
			 	 logger.fine(job.getName() + " can execute, lost critical section");
				 return false;
			 }
		 }
	}

	/**
	 * Compatibility entry point for callers compiled against the original API.
	 * Interrupted/cancelled acquisition is reported explicitly instead of
	 * silently continuing without ownership.
	 */
	public void beginCriticalSection(Job job){
		if (!tryBeginCriticalSection(job))
			throw new CancellationException("Critical section acquisition cancelled: " + job.getName());
	}

	/**
	 * Attempts to acquire the queue-wide critical section for {@code job}.
	 *
	 * <p>The old implementation ignored interruption while waiting and then
	 * granted ownership anyway.  A cancelled/interrupted MPO load could
	 * therefore acquire the section after the user had moved on, starving the
	 * next save or load.  Cancellation is observed between timed waits as well,
	 * so a normal Job.cancel() does not require an unsafe thread stop.</p>
	 *
	 * @return {@code true} when ownership was acquired, otherwise {@code false}
	 */
	public boolean tryBeginCriticalSection(Job job){
		Objects.requireNonNull(job, "job");
		synchronized (criticalSectionMutex) {
			while (criticalSectionOwner!=null&&criticalSectionOwner.isQueued()){
				if (job.isCanceled())
					return false;
				try {
					criticalSectionMutex.wait(250L);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return false;
				}
			}
			if (job.isCanceled())
				return false;
	 		criticalSectionOwner=job;
			job.logBegin("Critical section");
			return true;
		}
	}

	public void endCriticalSection(Job job){
		Objects.requireNonNull(job, "job");
		synchronized (criticalSectionMutex) {
	 		job.logEnd("Critical section");
			if (criticalSectionOwner==job){
				criticalSectionOwner=null;
				criticalSectionMutex.notifyAll();
			}

		}
 	}
	public Object getComponent() {
		if (!Environment.isVisible())
			return null;
		return JobQueueUiServices.getProvider().getComponent(documentBased);
	}

}
